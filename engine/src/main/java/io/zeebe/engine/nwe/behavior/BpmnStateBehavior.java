/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.nwe.behavior;

import io.zeebe.engine.nwe.BpmnElementContext;
import io.zeebe.engine.processor.workflow.WorkflowInstanceLifecycle;
import io.zeebe.engine.state.ZeebeState;
import io.zeebe.engine.state.instance.ElementInstance;
import io.zeebe.engine.state.instance.ElementInstanceState;
import io.zeebe.engine.state.instance.EventScopeInstanceState;
import io.zeebe.engine.state.instance.IndexedRecord;
import io.zeebe.engine.state.instance.JobState;
import io.zeebe.protocol.impl.record.value.workflowinstance.WorkflowInstanceRecord;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import java.util.Optional;
import java.util.function.Consumer;

public final class BpmnStateBehavior {

  private final ElementInstanceState elementInstanceState;
  private final EventScopeInstanceState eventScopeInstanceState;
  private final JobState jobState;

  private final TypesStreamWriterProxy streamWriter;

  public BpmnStateBehavior(
      final ZeebeState zeebeState, final TypesStreamWriterProxy streamWriterProxy) {
    final var workflowState = zeebeState.getWorkflowState();
    elementInstanceState = workflowState.getElementInstanceState();
    eventScopeInstanceState = workflowState.getEventScopeInstanceState();
    jobState = zeebeState.getJobState();
    streamWriter = streamWriterProxy;
  }

  public ElementInstance getElementInstance(final BpmnElementContext context) {
    return elementInstanceState.getInstance(context.getElementInstanceKey());
  }

  public void updateElementInstance(final ElementInstance elementInstance) {
    elementInstanceState.updateInstance(elementInstance);
  }

  public void updateElementInstance(
      final BpmnElementContext context, final Consumer<ElementInstance> modifier) {
    final var elementInstance = getElementInstance(context);
    modifier.accept(elementInstance);
    updateElementInstance(elementInstance);
  }

  public void updateFlowScopeInstance(
      final BpmnElementContext context, final Consumer<ElementInstance> modifier) {
    final var elementInstance = getFlowScopeInstance(context);
    modifier.accept(elementInstance);
    updateElementInstance(elementInstance);
  }

  public JobState getJobState() {
    return jobState;
  }

  public boolean isLastActiveExecutionPathInScope(final BpmnElementContext context) {
    final ElementInstance flowScopeInstance = getFlowScopeInstance(context);

    if (flowScopeInstance == null) {
      return false;
    }

    final int activePaths = flowScopeInstance.getNumberOfActiveTokens();
    if (activePaths < 0) {
      throw new IllegalStateException(
          String.format(
              "Expected number of active paths to be positive but got %d for instance %s",
              activePaths, flowScopeInstance));
    }

    return activePaths == 1;
  }

  public void completeFlowScope(final BpmnElementContext context) {
    final ElementInstance flowScopeInstance = getFlowScopeInstance(context);
    final WorkflowInstanceRecord flowScopeInstanceValue = flowScopeInstance.getValue();

    streamWriter.appendFollowUpEvent(
        flowScopeInstance.getKey(),
        WorkflowInstanceIntent.ELEMENT_COMPLETING,
        flowScopeInstanceValue);
  }

  public void consumeToken(final BpmnElementContext context) {
    final ElementInstance flowScopeInstance = getFlowScopeInstance(context);
    if (flowScopeInstance != null) {
      elementInstanceState.consumeToken(flowScopeInstance.getKey());
    }
  }

  // from ElementTerminatedHandler (with small changes)
  public void terminateFlowScope(final BpmnElementContext context) {
    final ElementInstance flowScopeInstance = getFlowScopeInstance(context);
    final boolean isScopeTerminating =
        flowScopeInstance != null
            && WorkflowInstanceLifecycle.canTransition(
                flowScopeInstance.getState(), WorkflowInstanceIntent.ELEMENT_TERMINATED);
    if (isScopeTerminating && isLastActiveExecutionPathInScope(context)) {
      streamWriter.appendFollowUpEvent(
          flowScopeInstance.getKey(),
          WorkflowInstanceIntent.ELEMENT_TERMINATED,
          flowScopeInstance.getValue());
      // TODO (saig0): update state because of the step guards
      flowScopeInstance.setState(WorkflowInstanceIntent.ELEMENT_TERMINATED);
      updateElementInstance(flowScopeInstance);

    } else if (wasInterrupted(flowScopeInstance)) {
      publishInterruptingEventSubproc(context, flowScopeInstance);
    }
  }

  // from ElementTerminatedHandler (with small changes)
  private void publishInterruptingEventSubproc(
      final BpmnElementContext context, final ElementInstance flowScopeInstance) {
    final Optional<IndexedRecord> eventSubprocOptional =
        elementInstanceState.getDeferredRecords(flowScopeInstance.getKey()).stream()
            .filter(r -> r.getKey() == flowScopeInstance.getInterruptingEventKey())
            .findFirst();

    if (eventSubprocOptional.isPresent()) {
      final IndexedRecord record = eventSubprocOptional.get();

      record.getValue().setFlowScopeKey(flowScopeInstance.getKey());
      if (record.getState().equals(WorkflowInstanceIntent.ELEMENT_ACTIVATING)) {
        elementInstanceState.newInstance(
            flowScopeInstance, record.getKey(), record.getValue(), record.getState());
      }
      streamWriter.appendFollowUpEvent(record.getKey(), record.getState(), record.getValue());
    }
  }

  public void spawnToken(final BpmnElementContext context) {
    final ElementInstance flowScopeInstance = getFlowScopeInstance(context);
    if (flowScopeInstance != null) {
      elementInstanceState.spawnToken(flowScopeInstance.getKey());
    }
  }

  // replaces BpmnStepContext.getFlowScopeInstance()
  public ElementInstance getFlowScopeInstance(final BpmnElementContext context) {
    return elementInstanceState.getInstance(context.getFlowScopeKey());
  }

  // from ElementTerminatedHandler
  private boolean wasInterrupted(final ElementInstance flowScopeInstance) {
    return flowScopeInstance != null
        && flowScopeInstance.getNumberOfActiveTokens() == 2
        && flowScopeInstance.isInterrupted()
        && flowScopeInstance.isActive();
  }

  public void removeInstance(final BpmnElementContext context) {
    eventScopeInstanceState.deleteInstance(context.getElementInstanceKey());
    elementInstanceState.removeInstance(context.getElementInstanceKey());
  }
}