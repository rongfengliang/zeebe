package io.zeebe.engine.nwe;

import io.zeebe.engine.processor.TypedRecord;
import io.zeebe.engine.processor.workflow.BpmnStepContext;
import io.zeebe.engine.processor.workflow.deployment.model.element.ExecutableFlowElement;
import io.zeebe.engine.state.ZeebeState;
import io.zeebe.protocol.impl.record.value.workflowinstance.WorkflowInstanceRecord;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import org.agrona.DirectBuffer;

public final class BpmnElementContextImpl implements BpmnElementContext {

  private long elementInstanceKey;

  private WorkflowInstanceRecord recordValue;
  private WorkflowInstanceIntent intent;

  private final BpmnStepContext<?> stepContext;

  public BpmnElementContextImpl(final ZeebeState zeebeState) {
    stepContext = new BpmnStepContext<>(zeebeState.getWorkflowState(), null);
  }

  @Override
  public long getElementInstanceKey() {
    return elementInstanceKey;
  }

  @Override
  public long getFlowScopeKey() {
    return recordValue.getFlowScopeKey();
  }

  @Override
  public long getWorkflowInstanceKey() {
    return recordValue.getWorkflowInstanceKey();
  }

  @Override
  public long getWorkflowKey() {
    return recordValue.getWorkflowKey();
  }

  @Override
  public int getWorkflowVersion() {
    return recordValue.getVersion();
  }

  @Override
  public DirectBuffer getBpmnProcessId() {
    return recordValue.getBpmnProcessIdBuffer();
  }

  @Override
  public DirectBuffer getElementId() {
    return recordValue.getElementIdBuffer();
  }

  @Override
  public long getVariableScopeKey() {
    // TODO (saig0): variable scope key is sometimes not the element instance key
    return elementInstanceKey;
  }

  @Override
  public <T extends ExecutableFlowElement> BpmnStepContext<T> toStepContext() {
    return (BpmnStepContext<T>) stepContext;
  }

  @Override
  public WorkflowInstanceRecord getRecordValue() {
    return recordValue;
  }

  @Override
  public WorkflowInstanceIntent getIntent() {
    return intent;
  }

  public void init(
      final TypedRecord<WorkflowInstanceRecord> record,
      final WorkflowInstanceIntent intent,
      final ExecutableFlowElement element) {
    elementInstanceKey = record.getKey();
    recordValue = record.getValue();
    this.intent = intent;

    stepContext.init(elementInstanceKey, recordValue, intent);
    stepContext.setElement(element);
  }
}
