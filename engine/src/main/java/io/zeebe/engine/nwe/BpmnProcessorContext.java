package io.zeebe.engine.nwe;

import io.zeebe.engine.processor.workflow.deployment.model.element.ExecutableFlowElement;

public interface BpmnProcessorContext<T extends ExecutableFlowElement> {

  T getElement();
}
