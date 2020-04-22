package io.zeebe.engine.nwe;

import io.zeebe.engine.nwe.task.ServiceTaskProcessor;
import io.zeebe.engine.processor.SideEffectProducer;
import io.zeebe.engine.processor.TypedRecord;
import io.zeebe.engine.processor.TypedRecordProcessor;
import io.zeebe.engine.processor.TypedResponseWriter;
import io.zeebe.engine.processor.TypedStreamWriter;
import io.zeebe.engine.processor.workflow.CatchEventBehavior;
import io.zeebe.engine.processor.workflow.ExpressionProcessor;
import io.zeebe.engine.processor.workflow.deployment.model.element.ExecutableFlowElement;
import io.zeebe.engine.processor.workflow.handlers.IOMappingHelper;
import io.zeebe.engine.state.deployment.WorkflowState;
import io.zeebe.protocol.impl.record.value.workflowinstance.WorkflowInstanceRecord;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import io.zeebe.protocol.record.value.BpmnElementType;
import java.util.Map;
import java.util.function.Consumer;

public final class BpmnStreamProcessor implements TypedRecordProcessor<WorkflowInstanceRecord> {

  private final ExpressionProcessor expressionProcessor;
  private final IOMappingHelper ioMappingHelper;
  private final CatchEventBehavior catchEventBehavior;

  private final WorkflowState workflowState;

  private final Map<BpmnElementType, BpmnElementProcessor<?>> processors;

  public BpmnStreamProcessor(
      final ExpressionProcessor expressionProcessor,
      final IOMappingHelper ioMappingHelper,
      final CatchEventBehavior catchEventBehavior,
      final WorkflowState workflowState) {
    this.expressionProcessor = expressionProcessor;
    this.ioMappingHelper = ioMappingHelper;
    this.catchEventBehavior = catchEventBehavior;
    this.workflowState = workflowState;

    final BpmnBehaviors bpmnBehaviors = null;

    processors = Map.of(BpmnElementType.SERVICE_TASK, new ServiceTaskProcessor(bpmnBehaviors));
  }

  private <T extends ExecutableFlowElement> BpmnElementProcessor<T> getProcessor(
      final BpmnElementType bpmnElementType) {
    return (BpmnElementProcessor<T>) processors.get(bpmnElementType);
  }

  @Override
  public void processRecord(
      final TypedRecord<WorkflowInstanceRecord> record,
      final TypedResponseWriter responseWriter,
      final TypedStreamWriter streamWriter,
      final Consumer<SideEffectProducer> sideEffect) {

    final var recordValue = record.getValue();
    final var bpmnElementType = recordValue.getBpmnElementType();
    final var processor = getProcessor(bpmnElementType);

    final var element =
        workflowState.getFlowElement(
            recordValue.getWorkflowKey(), recordValue.getElementIdBuffer(), processor.getType());

    final BpmnElementContext context = null;

    final WorkflowInstanceIntent intent = (WorkflowInstanceIntent) record.getIntent();
    switch (intent) {
      case ELEMENT_ACTIVATING:
        processor.onActivating(element, context);
        // transition to ELEMENT_ACTIVATED
        break;
      case ELEMENT_ACTIVATED:
        processor.onActivated(element, context);
        break;
      case EVENT_OCCURRED:
        processor.onEventOccurred(element, context);
        break;
      case ELEMENT_COMPLETING:
        processor.onCompleting(element, context);
        // transition to ELEMENT_COMPLETED
        break;
      case ELEMENT_COMPLETED:
        processor.onCompleted(element, context);
        break;
      case ELEMENT_TERMINATING:
        processor.onTerminating(element, context);
        break;
      case ELEMENT_TERMINATED:
        processor.onTerminated(element, context);
        break;
    }
  }
}
