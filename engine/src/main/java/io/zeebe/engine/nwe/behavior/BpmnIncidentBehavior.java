package io.zeebe.engine.nwe.behavior;

import io.zeebe.engine.nwe.BpmnElementContext;
import io.zeebe.engine.processor.TypedStreamWriter;
import io.zeebe.engine.state.ZeebeState;
import io.zeebe.engine.state.instance.ElementInstanceState;
import io.zeebe.engine.state.instance.IncidentState;
import io.zeebe.engine.state.instance.StoredRecord.Purpose;
import io.zeebe.protocol.impl.record.value.incident.IncidentRecord;
import io.zeebe.protocol.record.intent.IncidentIntent;
import io.zeebe.protocol.record.value.ErrorType;

public final class BpmnIncidentBehavior {

  private final IncidentRecord incidentCommand = new IncidentRecord();

  private final IncidentState incidentState;
  private final ElementInstanceState elementInstanceState;
  private final TypedStreamWriter streamWriter;

  public BpmnIncidentBehavior(final ZeebeState zeebeState, final TypedStreamWriter streamWriter) {
    incidentState = zeebeState.getIncidentState();
    elementInstanceState = zeebeState.getWorkflowState().getElementInstanceState();
    this.streamWriter = streamWriter;
  }

  public void resolveJobIncident(final long jobKey) {
    final long incidentKey = incidentState.getJobIncidentKey(jobKey);
    final boolean hasIncident = incidentKey != IncidentState.MISSING_INCIDENT;

    if (hasIncident) {
      final IncidentRecord incidentRecord = incidentState.getIncidentRecord(incidentKey);
      streamWriter.appendFollowUpEvent(incidentKey, IncidentIntent.RESOLVED, incidentRecord);
    }
  }

  public void createIncident(
      final ErrorType errorType, final String errorMessage, final BpmnElementContext context) {

    incidentCommand.reset();
    incidentCommand
        .setElementInstanceKey(context.getElementInstanceKey())
        .setBpmnProcessId(context.getBpmnProcessId())
        .setWorkflowKey(context.getWorkflowKey())
        .setElementInstanceKey(context.getWorkflowInstanceKey())
        .setElementId(context.getElementId())
        .setVariableScopeKey(context.getVariableScopeKey())
        .setErrorType(errorType)
        .setErrorMessage(errorMessage);

    elementInstanceState.storeRecord(
        context.getElementInstanceKey(),
        context.getFlowScopeKey(),
        context.getRecordValue(),
        context.getIntent(),
        Purpose.FAILED);

    streamWriter.appendNewCommand(IncidentIntent.CREATE, incidentCommand);
  }
}
