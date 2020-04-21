package io.zeebe.engine.nwe;

import io.zeebe.engine.nwe.task.ServiceTaskProcessor;
import io.zeebe.engine.processor.SideEffectProducer;
import io.zeebe.engine.processor.TypedRecord;
import io.zeebe.engine.processor.TypedRecordProcessor;
import io.zeebe.engine.processor.TypedResponseWriter;
import io.zeebe.engine.processor.TypedStreamWriter;
import io.zeebe.protocol.impl.record.value.workflowinstance.WorkflowInstanceRecord;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import io.zeebe.protocol.record.value.BpmnElementType;
import java.util.Map;
import java.util.function.Consumer;

public final class BpmnStreamProcessor implements TypedRecordProcessor<WorkflowInstanceRecord> {

  private final Map<BpmnElementType, BpmnElementProcessor<?>> processors =
      Map.of(BpmnElementType.SERVICE_TASK, new ServiceTaskProcessor());

  @Override
  public void processRecord(
      final TypedRecord<WorkflowInstanceRecord> record,
      final TypedResponseWriter responseWriter,
      final TypedStreamWriter streamWriter,
      final Consumer<SideEffectProducer> sideEffect) {

    final var recordValue = record.getValue();
    final var bpmnElementType = recordValue.getBpmnElementType();
    final BpmnElementProcessor<?> processor = processors.get(bpmnElementType);

    final WorkflowInstanceIntent intent = (WorkflowInstanceIntent) record.getIntent();
    switch (intent) {
      case ELEMENT_ACTIVATING:
        processor.onActivating();
        break;
      case ELEMENT_ACTIVATED:
        processor.onActivated();
        break;
      case EVENT_OCCURRED:
        processor.onEventOccurred();
        break;
      case ELEMENT_COMPLETING:
        processor.onCompleting();
        break;
      case ELEMENT_COMPLETED:
        processor.onCompleted();
        break;
      case ELEMENT_TERMINATING:
        processor.onTerminating();
        break;
      case ELEMENT_TERMINATED:
        processor.onTerminated();
        break;
    }
  }
}
