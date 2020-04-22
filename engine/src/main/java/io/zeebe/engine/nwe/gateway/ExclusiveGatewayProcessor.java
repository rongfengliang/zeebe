package io.zeebe.engine.nwe.gateway;

import io.zeebe.el.Expression;
import io.zeebe.engine.nwe.BpmnBehaviors;
import io.zeebe.engine.nwe.BpmnElementContext;
import io.zeebe.engine.nwe.BpmnElementProcessor;
import io.zeebe.engine.nwe.BpmnIncidentBehavior;
import io.zeebe.engine.processor.workflow.EventOutput;
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

  private final ExpressionProcessor expressionBehavior;
  private final EventOutput eventWriter;
  private final WorkflowInstanceRecord record = new WorkflowInstanceRecord();
  private final BpmnIncidentBehavior incidentBehavior;

  public ExclusiveGatewayProcessor(final BpmnBehaviors bpmnBehaviors) {
    this.expressionBehavior = bpmnBehaviors.expressionBehavior();
    this.incidentBehavior = bpmnBehaviors.incidentBehavior();

    // probably this is not bpmn behavior, but like command writer more IO related
    // todo: discuss whether this should be part of bpmnbehavior
    this.eventWriter = bpmnBehaviors.eventWriter();
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
        .ifPresent(sequenceFlow -> {
          // defer sequence flow taken, since sequence is taken when the gateway is completed
          // record.wrap(context.getValue()); todo: find a way to get the record we're currently processing
          record.setElementId(sequenceFlow.getId());
          record.setBpmnElementType(BpmnElementType.SEQUENCE_FLOW);
          eventWriter.deferRecord(
              context.getWorkflowInstanceKey() /*todo check if this is the correct key*/,
              record,
              WorkflowInstanceIntent.SEQUENCE_FLOW_TAKEN);
        });
  }

  @Override
  public void onActivated(
      final ExecutableExclusiveGateway element, final BpmnElementContext context) {

  }

  @Override
  public void onCompleting(
      final ExecutableExclusiveGateway element, final BpmnElementContext context) {

  }

  @Override
  public void onCompleted(
      final ExecutableExclusiveGateway element, final BpmnElementContext context) {
//    publishDeferredRecords(context)

    // from ElementCompletedHandler
//    if (isLastActiveExecutionPathInScope(context)) {
//      completeFlowScope(context);
//    }
    // from AbstractTerminalStateHandler
//    final ElementInstance flowScopeInstance = context.getFlowScopeInstance();
//    if (flowScopeInstance != null) {
//      context.getStateDb().getElementInstanceState().consumeToken(flowScopeInstance.getKey());
//    }
  }

  @Override
  public void onTerminating(
      final ExecutableExclusiveGateway element, final BpmnElementContext context) {

  }

  @Override
  public void onTerminated(
      final ExecutableExclusiveGateway element, final BpmnElementContext context) {

  }

  @Override
  public void onEventOccurred(
      final ExecutableExclusiveGateway element, final BpmnElementContext context) {

  }

  private Optional<ExecutableSequenceFlow> findSequenceFlowWithFulfilledConditionOrDefault(
      final ExecutableExclusiveGateway element, final BpmnElementContext context) {
    for (ExecutableSequenceFlow sequenceFlow : element.getOutgoingWithCondition()) {
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
      // todo: raise incident NO_OUTGOING_FLOW_CHOSEN_ERROR if unable to find any
      incidentBehavior
          .createIncident(ErrorType.CONDITION_ERROR, NO_OUTGOING_FLOW_CHOSEN_ERROR, context);
      return Optional.empty();
    }
  }
}
