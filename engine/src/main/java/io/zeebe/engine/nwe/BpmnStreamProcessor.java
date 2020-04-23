package io.zeebe.engine.nwe;

import io.zeebe.engine.Loggers;
import io.zeebe.engine.nwe.behavior.BpmnBehaviors;
import io.zeebe.engine.nwe.behavior.BpmnBehaviorsImpl;
import io.zeebe.engine.nwe.behavior.BpmnIncidentBehavior;
import io.zeebe.engine.nwe.behavior.BpmnStateBehavior;
import io.zeebe.engine.nwe.behavior.BpmnStateTransitionBehavior;
import io.zeebe.engine.nwe.behavior.DeferredRecordsBehavior;
import io.zeebe.engine.nwe.behavior.TypesStreamWriterProxy;
import io.zeebe.engine.nwe.gateway.ExclusiveGatewayProcessor;
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
import io.zeebe.engine.state.ZeebeState;
import io.zeebe.engine.state.deployment.WorkflowState;
import io.zeebe.protocol.impl.record.value.workflowinstance.WorkflowInstanceRecord;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import io.zeebe.protocol.record.value.BpmnElementType;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;

public final class BpmnStreamProcessor implements TypedRecordProcessor<WorkflowInstanceRecord> {

  private static final Logger LOGGER = Loggers.WORKFLOW_PROCESSOR_LOGGER;

  private final TypesStreamWriterProxy streamWriterProxy = new TypesStreamWriterProxy();

  private final BpmnElementContextImpl context;
  private final WorkflowState workflowState;

  private final Map<BpmnElementType, BpmnElementProcessor<?>> processors;

  public BpmnStreamProcessor(
      final ExpressionProcessor expressionProcessor,
      final IOMappingHelper ioMappingHelper,
      final CatchEventBehavior catchEventBehavior,
      final ZeebeState zeebeState) {

    workflowState = zeebeState.getWorkflowState();

    final BpmnBehaviors bpmnBehaviors =
        new BpmnBehaviorsImpl(
            expressionProcessor,
            ioMappingHelper,
            catchEventBehavior,
            new BpmnIncidentBehavior(zeebeState, streamWriterProxy),
            new BpmnStateBehavior(zeebeState, streamWriterProxy),
            new BpmnStateTransitionBehavior(streamWriterProxy),
            streamWriterProxy,
            new DeferredRecordsBehavior(zeebeState, streamWriterProxy));

    processors =
        Map.of(
            BpmnElementType.SERVICE_TASK, new ServiceTaskProcessor(bpmnBehaviors),
            BpmnElementType.EXCLUSIVE_GATEWAY,
            new ExclusiveGatewayProcessor(bpmnBehaviors));

    context = new BpmnElementContextImpl(zeebeState);
  }

  private <T extends ExecutableFlowElement> BpmnElementProcessor<T> getProcessor(
      final BpmnElementType bpmnElementType) {
    final var processor = (BpmnElementProcessor<T>) processors.get(bpmnElementType);
    if (processor == null) {
      throw new UnsupportedOperationException(
          String.format("no processor found for BPMN element type '%s'", bpmnElementType));
    }
    return processor;
  }

  @Override
  public void processRecord(
      final TypedRecord<WorkflowInstanceRecord> record,
      final TypedResponseWriter responseWriter,
      final TypedStreamWriter streamWriter,
      final Consumer<SideEffectProducer> sideEffect) {

    final var intent = (WorkflowInstanceIntent) record.getIntent();
    final var recordValue = record.getValue();
    final var bpmnElementType = recordValue.getBpmnElementType();
    final var processor = getProcessor(bpmnElementType);

    final var element =
        workflowState.getFlowElement(
            recordValue.getWorkflowKey(), recordValue.getElementIdBuffer(), processor.getType());

    LOGGER.info(
        "[NEW] process workflow instance event [BPMN element type: {}, intent: {}]",
        bpmnElementType,
        intent);

    // initialize the stuff
    streamWriterProxy.wrap(streamWriter);
    context.init(record, intent, element, streamWriterProxy);

    // process the event
    processEvent(intent, processor, element);
  }

  private void processEvent(
      final WorkflowInstanceIntent intent,
      final BpmnElementProcessor<ExecutableFlowElement> processor,
      final ExecutableFlowElement element) {

    switch (intent) {
      case ELEMENT_ACTIVATING:
        processor.onActivating(element, context);
        break;
      case ELEMENT_ACTIVATED:
        processor.onActivated(element, context);
        break;
      case EVENT_OCCURRED:
        processor.onEventOccurred(element, context);
        break;
      case ELEMENT_COMPLETING:
        processor.onCompleting(element, context);
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
      default:
        throw new UnsupportedOperationException(
            String.format(
                "processor '%s' can not handle intent '%s'", processor.getClass(), intent));
    }
  }
}
