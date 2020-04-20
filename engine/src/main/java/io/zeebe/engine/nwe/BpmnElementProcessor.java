package io.zeebe.engine.nwe;

import io.zeebe.engine.processor.workflow.deployment.model.element.ExecutableFlowElement;

public interface BpmnElementProcessor<T extends ExecutableFlowElement> {

  void onActivating();

  void onActivated();

  void onCompleting();

  void onCompleted();

  void onTerminating();

  void onTerminated();

  void onEventOccurred();
}
