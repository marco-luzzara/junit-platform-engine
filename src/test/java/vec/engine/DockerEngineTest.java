package vec.engine;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.EventConditions;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import org.junit.platform.testkit.engine.Events;
import org.opentest4j.TestAbortedException;
import vec.engine.sample.AnnotatedClass;
import vec.engine.sample.AnnotatedSingleTests;

import java.util.ArrayList;

public class DockerEngineTest {
//  private static final EngineExecutionResults executionResultsForClass =
//      EngineTestKit.engine("docker-engine").selectors(selectClass(AnnotatedClass.class)).execute();
  private static final EngineExecutionResults executionResultsForSingleTests =
          EngineTestKit.engine("docker-engine").selectors(selectClass(AnnotatedSingleTests.class)).execute();

//  @Test
//  public void annotatedClass_testStatisticsForTests() {
//    executionResultsForClass
//        .testEvents()
//        .assertStatistics(stats -> stats.started(6).succeeded(2).aborted(1).failed(3));
//  }
//
//  @Test
//  public void annotatedClass_testStatisticsForContainers() {
//    executionResultsForClass
//        .containerEvents()
//        .assertStatistics(stats -> stats.started(2).succeeded(0).failed(2));
//  }
//
//  @Test
//  public void annotatedClass_singleTestAssertions() {
//    Events testEvents = executionResultsForClass.testEvents();
//
//    testEvents.assertThatEvents()
//            .haveExactly(1, event(test("parameterized_Successful"), finishedSuccessfully()))
//            .haveExactly(1, event(test("simple_Failed"),
//                    finishedWithFailure(instanceOf(RuntimeException.class), message("Some tests failed. See container logs"))))
//            .doNotHave(event(test("simple_Undiscovered"), started()));
//  }

  @Test
  public void annotatedSingleTests_exactMatching() {
    Events allEvents = executionResultsForSingleTests.allEvents();

    allEvents.assertEventsMatchExactly(
            event(engine(), started()),
            event(container(AnnotatedSingleTests.class), started()),
            event(test("simple_Aborted"), started()),
            event(test("simple_Aborted"),
                    abortedWithReason(instanceOf(RuntimeException.class),
                            message("Some tests aborted. See container logs"))),
            event(test("simple_Successful"), started()),
            event(test("simple_Successful"), finishedSuccessfully()),
            event(test("simple_Failed"), started()),
            event(test("simple_Failed"), finishedWithFailure(
                    instanceOf(RuntimeException.class), message("Some tests failed. See container logs")
            )),
            event(container(AnnotatedSingleTests.class), finishedWithFailure()),
            event(engine(), finishedWithFailure()));
  }
}
