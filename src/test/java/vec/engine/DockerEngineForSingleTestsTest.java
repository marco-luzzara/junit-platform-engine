package vec.engine;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;
import vec.engine.sample.AnnotatedSingleTests;

public class DockerEngineForSingleTestsTest {
  private static final EngineExecutionResults executionResults =
      EngineTestKit.engine("docker-engine")
          .selectors(selectClass(AnnotatedSingleTests.class))
          .execute();

  @Test
  public void annotatedSingleTests_exactMatching() {
    Events allEvents = executionResults.allEvents();

    allEvents.assertEventsMatchExactly(
        event(engine(), started()),
        event(container(AnnotatedSingleTests.class), started()),
        event(test("simple_Aborted"), started()),
        event(
            test("simple_Aborted"),
            abortedWithReason(
                instanceOf(RuntimeException.class),
                message("Some tests aborted. See container logs"))),
        event(test("simple_Successful"), started()),
        event(test("simple_Successful"), finishedSuccessfully()),
        event(test("simple_Failed"), started()),
        event(
            test("simple_Failed"),
            finishedWithFailure(
                instanceOf(RuntimeException.class),
                message("Some tests failed. See container logs"))),
        event(container(AnnotatedSingleTests.class), finishedSuccessfully()),
        event(engine(), finishedSuccessfully()));
  }

  @Test
  public void annotatedSingleTests_looseMatchingInOrder() {
    Events allEvents = executionResults.allEvents();

    allEvents.assertEventsMatchLooselyInOrder(
        event(test("simple_Aborted"), started()),
        event(
            test("simple_Aborted"),
            abortedWithReason(
                instanceOf(RuntimeException.class),
                message("Some tests aborted. See container logs"))),
        event(test("simple_Successful"), started()),
        event(test("simple_Successful"), finishedSuccessfully()),
        event(test("simple_Failed"), started()),
        event(
            test("simple_Failed"),
            finishedWithFailure(
                instanceOf(RuntimeException.class),
                message("Some tests failed. See container logs"))));
  }

  @Test
  public void annotatedSingleTests_looseMatching() {
    Events allEvents = executionResults.allEvents();

    allEvents.assertEventsMatchLoosely(
        event(test("simple_Aborted"), started()),
        event(test("simple_Successful"), started()),
        event(test("simple_Failed"), started()),
        event(
            test("simple_Aborted"),
            abortedWithReason(
                instanceOf(RuntimeException.class),
                message("Some tests aborted. See container logs"))),
        event(test("simple_Successful"), finishedSuccessfully()),
        event(
            test("simple_Failed"),
            finishedWithFailure(
                instanceOf(RuntimeException.class),
                message("Some tests failed. See container logs"))));
  }
}
