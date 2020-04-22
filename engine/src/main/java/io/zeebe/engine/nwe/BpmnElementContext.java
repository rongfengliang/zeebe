package io.zeebe.engine.nwe;

import io.zeebe.engine.processor.workflow.BpmnStepContext;
import io.zeebe.engine.processor.workflow.deployment.model.element.ExecutableFlowElement;
import org.agrona.DirectBuffer;

public interface BpmnElementContext {

  long getElementInstanceKey();

  long getWorkflowInstanceKey();

  long getWorkflowKey();

  int getWorkflowVersion();

  DirectBuffer getBpmnProcessId();

  <T extends ExecutableFlowElement> BpmnStepContext<T> toStepContext();
}
