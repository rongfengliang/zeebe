/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.nwe.gateway;

import io.zeebe.el.Expression;
import io.zeebe.engine.nwe.BpmnElementContext;
import io.zeebe.engine.nwe.BpmnElementProcessor;
import io.zeebe.engine.nwe.behavior.BpmnBehaviors;
import io.zeebe.engine.nwe.behavior.BpmnIncidentBehavior;
import io.zeebe.engine.nwe.behavior.BpmnStateBehavior;
import io.zeebe.engine.nwe.behavior.BpmnStateTransitionBehavior;
import io.zeebe.engine.nwe.behavior.DeferredRecordsBehavior;
import io.zeebe.engine.processor.workflow.ExpressionProcessor;
import io.zeebe.engine.processor.workflow.deployment.model.element.ExecutableExclusiveGateway;
import io.zeebe.engine.processor.workflow.deployment.model.element.ExecutableSequenceFlow;
import io.zeebe.protocol.impl.record.value.workflowinstance.WorkflowInstanceRecord;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import io.zeebe.protocol.record.value.BpmnElementType;
import io.zeebe.protocol.record.value.ErrorType;
import java.util.Optional;

public class ExclusiveGatewayProcessor implements BpmnElementProcessor<ExecutableExclusiveGateway> {

  private static final String NO_OUTGOING_FLOW_CHOSEN_ERROR =
      "Expected at least one condition to evaluate to true, or to have a default flow";

  private final WorkflowInstanceRecord record = new WorkflowInstanceRecord();

  private final BpmnStateBehavior stateBehavior;
  private final BpmnStateTransitionBehavior stateTransitionBehavior;
  private final BpmnIncidentBehavior incidentBehavior;
  private final DeferredRecordsBehavior deferredRecordsBehavior;
  private final ExpressionProcessor expressionBehavior;

  public ExclusiveGatewayProcessor(final BpmnBehaviors behaviors) {
    expressionBehavior = behaviors.expressionBehavior();
    incidentBehavior = behaviors.incidentBehavior();
    stateBehavior = behaviors.stateBehavior();
    deferredRecordsBehavior = behaviors.deferredRecordsBehavior();
    stateTransitionBehavior = behaviors.stateTransitionBehavior();
  }

  @Override
  public Class<ExecutableExclusiveGateway> getType() {
    return ExecutableExclusiveGateway.class;
  }

  @Override
  public void onActivating(
      final ExecutableExclusiveGateway element, final BpmnElementContext context) {
    // find outgoing sequence flow with fulfilled condition or default
    findSequenceFlowWithFulfilledConditionOrDefault(element, context)
        .ifPresent(
            sequenceFlow -> {
              // defer sequence flow taken, since sequence is taken when the gateway is completed
              record.wrap(context.getRecordValue());
              record.setElementId(sequenceFlow.getId());
              record.setBpmnElementType(BpmnElementType.SEQUENCE_FLOW);
              deferredRecordsBehavior.deferNewRecord(
                  context.getElementInstanceKey(),
                  record,
                  WorkflowInstanceIntent.SEQUENCE_FLOW_TAKEN);

              stateTransitionBehavior.transitionToActivated(context);
              // TODO (saig0): update state because of the step guards
              stateBehavior.updateElementInstance(
                  context,
                  elementInstance ->
                      elementInstance.setState(WorkflowInstanceIntent.ELEMENT_ACTIVATED));
            });
  }

  @Override
  public void onActivated(
      final ExecutableExclusiveGateway element, final BpmnElementContext context) {
    stateTransitionBehavior.transitionToCompleting(context);
  }

  @Override
  public void onCompleting(
      final ExecutableExclusiveGateway element, final BpmnElementContext context) {
    stateTransitionBehavior.transitionToCompleted(context);
  }

  @Override
  public void onCompleted(
      final ExecutableExclusiveGateway element, final BpmnElementContext context) {
    deferredRecordsBehavior.publishDeferredRecords(context);

    if (stateBehavior.isLastActiveExecutionPathInScope(context)) {
      stateBehavior.completeFlowScope(context);
    }
    // from AbstractTerminalStateHandler
    stateBehavior.consumeToken(context);
  }

  @Override
  public void onTerminating(
      final ExecutableExclusiveGateway element, final BpmnElementContext context) {}

  @Override
  public void onTerminated(
      final ExecutableExclusiveGateway element, final BpmnElementContext context) {}

  @Override
  public void onEventOccurred(
      final ExecutableExclusiveGateway element, final BpmnElementContext context) {
    throw new UnsupportedOperationException(
        "expected to handle occurred event on exclusive gateway, but events should not occur on exclusive gateway");
  }

  private Optional<ExecutableSequenceFlow> findSequenceFlowWithFulfilledConditionOrDefault(
      final ExecutableExclusiveGateway element, final BpmnElementContext context) {
    for (final ExecutableSequenceFlow sequenceFlow : element.getOutgoingWithCondition()) {
      final Expression condition = sequenceFlow.getCondition();
      final Optional<Boolean> isFulfilled =
          expressionBehavior.evaluateBooleanExpression(condition, context.toStepContext());
      if (isFulfilled.isEmpty()) {
        // the condition evaluation failed and an incident is raised
        // todo: discuss whether it would be better to move
        //  this incident raising away from the expressionbehavior
        return Optional.empty();

      } else if (isFulfilled.get()) {
        // the condition is fulfilled
        return Optional.of(sequenceFlow);
      }
    }
    // no condition is fulfilled - take the default flow if exists
    final var defaultFlow = element.getDefaultFlow();
    if (defaultFlow != null) {
      return Optional.of(defaultFlow);

    } else {
      // todo: move this incident creation outside of this method
      incidentBehavior.createIncident(
          ErrorType.CONDITION_ERROR,
          NO_OUTGOING_FLOW_CHOSEN_ERROR,
          context,
          context.getElementInstanceKey());
      return Optional.empty();
    }
  }
}
