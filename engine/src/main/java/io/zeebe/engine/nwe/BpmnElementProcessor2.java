package io.zeebe.engine.nwe;

import io.zeebe.engine.processor.workflow.deployment.model.element.ExecutableFlowElement;

public interface BpmnElementProcessor2<T extends ExecutableFlowElement> {

  void activate();

  void complete();

  void terminate();
}
