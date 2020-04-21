package io.zeebe.engine.nwe.task;

import io.zeebe.engine.nwe.BpmnElementProcessor;
import io.zeebe.engine.processor.workflow.deployment.model.element.ExecutableServiceTask;

public final class ServiceTaskProcessor implements BpmnElementProcessor<ExecutableServiceTask> {

  @Override
  public void onActivating() {
    // for all activities:
    // input mappings
    // subscribe to events
  }

  @Override
  public void onActivated() {
    // only for service task:
    // evaluate job type expression
    // evaluate job retries expression
    // create job

    // --> may be better done on activating
  }

  @Override
  public void onCompleting() {
    // for all activities:
    // output mappings
    // unsubscribe from events
  }

  @Override
  public void onCompleted() {
    // for all activities:
    // take outgoing sequence flows
    // complete scope if last active token
    // consume token
    // remove from event scope instance state
    // remove from element instance state
  }

  @Override
  public void onTerminating() {
    // only for service task:
    // cancel job
    // resolve job incident

    // for all activities:
    // unsubscribe from events
  }

  @Override
  public void onTerminated() {
    // for all activities:
    // publish deferred events (i.e. an occurred boundary event)
    // resolve incidents
    // terminate scope if scope is terminated and last active token
    // publish deferred event if an interrupting event sub-process was triggered
    // consume token
  }

  @Override
  public void onEventOccurred() {
    // for all activities:
    // (when boundary event is triggered)
    // if interrupting then terminate element and defer occurred event
    // if non-interrupting then activate boundary event, remove event trigger from state, spawn
    // token
  }
}
