package io.zeebe.engine.nwe;

import io.zeebe.engine.processor.workflow.deployment.model.element.ExecutableServiceTask;

public final class ServiceTaskProcessor implements BpmnElementProcessor<ExecutableServiceTask> {

  @Override
  public void onActivating() {
    // input mappings
    // subscript to events

  }

  @Override
  public void onActivated() {
    // --> may be better done on activating

    // evaluate job type expression
    // evaluate job retries expression

    // create job
  }

  @Override
  public void onCompleting() {
    // output mappings
    // unsubscribe from events
  }

  @Override
  public void onCompleted() {
    // take outgoing sequence flows
    // complete scope if last active token
    // consume token
    // remove from event scope instance state
    // remove from element instance state
  }

  @Override
  public void onTerminating() {
    // cancel job

    // resolve job incident
    // unsubscribe from events
  }

  @Override
  public void onTerminated() {
    // publish deferred events (i.e. an occurred boundary event)

    // resolve incidents
    // terminate scope if scope is terminated and last active token
    // publish deferred event if an interrupting event sub-process was triggered
    // consume token
  }

  @Override
  public void onEventOccurred() {
    // when boundary event is triggered
    // if interrupting then terminate element and defer occurred event
    // if non-interrupting then activate boundary event, remove event trigger from state, spawn
    // token
  }
}
