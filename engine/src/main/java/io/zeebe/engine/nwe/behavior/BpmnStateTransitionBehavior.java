package io.zeebe.engine.nwe.behavior;

import io.zeebe.engine.nwe.BpmnElementContext;
import io.zeebe.engine.processor.TypedStreamWriter;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;

public final class BpmnStateTransitionBehavior {

  private final TypedStreamWriter streamWriter;

  public BpmnStateTransitionBehavior(final TypedStreamWriter streamWriter) {
    this.streamWriter = streamWriter;
  }

  public void transitionToActivated(final BpmnElementContext context) {
    transitionTo(context, WorkflowInstanceIntent.ELEMENT_ACTIVATED);
  }

  public void transitionToTerminated(final BpmnElementContext context) {
    transitionTo(context, WorkflowInstanceIntent.ELEMENT_TERMINATED);
  }

  public void transitionToCompleting(final BpmnElementContext context) {
    transitionTo(context, WorkflowInstanceIntent.ELEMENT_COMPLETING);
  }

  public void transitionToCompleted(final BpmnElementContext context) {
    transitionTo(context, WorkflowInstanceIntent.ELEMENT_COMPLETED);
  }

  private void transitionTo(final BpmnElementContext context, final WorkflowInstanceIntent intent) {
    streamWriter.appendFollowUpEvent(
        context.getElementInstanceKey(), intent, context.getRecordValue());
  }
}
