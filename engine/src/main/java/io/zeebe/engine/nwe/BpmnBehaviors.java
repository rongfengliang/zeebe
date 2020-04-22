package io.zeebe.engine.nwe;

import io.zeebe.engine.processor.TypedCommandWriter;
import io.zeebe.engine.processor.workflow.CatchEventBehavior;
import io.zeebe.engine.processor.workflow.ExpressionProcessor;
import io.zeebe.engine.processor.workflow.handlers.IOMappingHelper;

public interface BpmnBehaviors {

  ExpressionProcessor expressionBehavior();

  IOMappingHelper variableMappingBehavior();

  CatchEventBehavior eventSubscriptionBehavior();

  BpmnIncidentBehavior incidentBehavior();

  TypedCommandWriter commandWriter();
}
