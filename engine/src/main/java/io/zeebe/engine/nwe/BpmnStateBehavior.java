package io.zeebe.engine.nwe;

import io.zeebe.engine.state.instance.ElementInstance;

public interface BpmnStateBehavior {

  ElementInstance getElementInstance(BpmnElementContext context);
}
