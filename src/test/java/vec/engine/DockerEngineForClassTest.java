package vec.engine;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;
import vec.engine.sample.AnnotatedClass;

public class DockerEngineForClassTest {
  private static final EngineExecutionResults executionResults =
      EngineTestKit.engine("docker-engine").selectors(selectClass(AnnotatedClass.class)).execute();

  @Test
  public void annotatedClass_testStatisticsForTests() {
    executionResults
        .testEvents()
        .assertStatistics(stats -> stats.started(6).succeeded(2).aborted(1).failed(3));
  }

  @Test
  public void annotatedClass_testStatisticsForContainers() {
    executionResults
        .containerEvents()
        .assertStatistics(stats -> stats.started(2).succeeded(0).failed(2));
  }

  @Test
  public void annotatedClass_singleTestAssertions() {
    Events testEvents = executionResults.testEvents();

    testEvents
        .assertThatEvents()
        .haveExactly(1, event(test("parameterized_Successful"), finishedSuccessfully()))
        .haveExactly(
            1,
            event(
                test("simple_Failed"),
                finishedWithFailure(
                    instanceOf(RuntimeException.class),
                    message("Some tests failed. See container logs"))))
        .doNotHave(event(test("simple_Undiscovered"), started()));
  }
}
