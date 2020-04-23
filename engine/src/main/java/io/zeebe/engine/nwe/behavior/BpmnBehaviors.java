package io.zeebe.engine.nwe.behavior;

import io.zeebe.engine.processor.TypedCommandWriter;
import io.zeebe.engine.processor.workflow.CatchEventBehavior;
import io.zeebe.engine.processor.workflow.EventOutput;
import io.zeebe.engine.processor.workflow.ExpressionProcessor;
import io.zeebe.engine.processor.workflow.handlers.IOMappingHelper;

public interface BpmnBehaviors {

  ExpressionProcessor expressionBehavior();

  IOMappingHelper variableMappingBehavior();

  CatchEventBehavior eventSubscriptionBehavior();

  BpmnIncidentBehavior incidentBehavior();

  BpmnStateBehavior stateBehavior();

  TypedCommandWriter commandWriter();

  EventOutput eventWriter();

  BpmnStateTransitionBehavior stateTransitionBehavior();
}
