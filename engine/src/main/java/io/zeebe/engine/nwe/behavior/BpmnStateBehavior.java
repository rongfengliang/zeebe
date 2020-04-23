package io.zeebe.engine.nwe.behavior;

import io.zeebe.engine.nwe.BpmnElementContext;
import io.zeebe.engine.state.ZeebeState;
import io.zeebe.engine.state.instance.ElementInstance;
import io.zeebe.engine.state.instance.ElementInstanceState;
import io.zeebe.engine.state.instance.JobState;
import java.util.function.Consumer;

public final class BpmnStateBehavior {

  private final ElementInstanceState elementInstanceState;
  private final JobState jobState;

  public BpmnStateBehavior(final ZeebeState zeebeState) {
    elementInstanceState = zeebeState.getWorkflowState().getElementInstanceState();
    jobState = zeebeState.getJobState();
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

  public JobState getJobState() {
    return jobState;
  }
}
