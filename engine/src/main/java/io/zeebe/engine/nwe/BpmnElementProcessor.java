package io.zeebe.engine.nwe;

import io.zeebe.engine.processor.workflow.deployment.model.element.ExecutableFlowElement;

public interface BpmnElementProcessor<T extends ExecutableFlowElement> {

  Class<T> getType();

  void onActivating(final T element, final BpmnElementContext context);

  void onActivated(final T element, final BpmnElementContext context);

  void onCompleting(final T element, final BpmnElementContext context);

  void onCompleted(final T element, final BpmnElementContext context);

  void onTerminating(final T element, final BpmnElementContext context);

  void onTerminated(final T element, final BpmnElementContext context);

  void onEventOccurred(final T element, final BpmnElementContext context);
}
