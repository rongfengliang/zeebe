package io.zeebe.engine.nwe;

import io.zeebe.engine.processor.workflow.BpmnStepContext;
import io.zeebe.engine.processor.workflow.deployment.model.element.ExecutableFlowElement;
import io.zeebe.protocol.impl.record.value.workflowinstance.WorkflowInstanceRecord;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import org.agrona.DirectBuffer;

public interface BpmnElementContext {

  long getElementInstanceKey();

  long getFlowScopeKey();

  long getWorkflowInstanceKey();

  long getWorkflowKey();

  int getWorkflowVersion();

  DirectBuffer getBpmnProcessId();

  DirectBuffer getElementId();

  // ---- for migration ----
  <T extends ExecutableFlowElement> BpmnStepContext<T> toStepContext();

  WorkflowInstanceRecord getRecordValue();

  WorkflowInstanceIntent getIntent();
}
