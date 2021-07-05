package vec.engine.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;
import org.opentest4j.TestAbortedException;
import vec.helpers.DockerHelper;

public class DockerEngineExecutionContext implements EngineExecutionContext {
  private final DockerHelper dockerHelper = new DockerHelper();
  private final ArrayList<String> containerIds = new ArrayList<>();

  public String[] getContainerIds() {
    return containerIds.toArray(String[]::new);
  }

  /**
   * start containers and build test classes to later run enclosed tests.
   *
   * @return container ids of started containers
   */
  public String[] prepareDockerContainers() {
    var containerId = dockerHelper.startTestingContainer();
    containerIds.add(containerId);

    return new String[] {containerId};
  }

  /** stop all registered testing containers */
  public void cleanUpDockerContainer() {
    for (var containerId : containerIds) dockerHelper.stopTestingContainer(containerId);
  }

  /**
   * run the test method (in canonical name form) inside the container with containerId. Test
   * execution throws only when there is at least a test failed or aborted. in case of skip or
   * successful it returns successfully. In order to detect a skipped test you need to override the
   * shouldBeSkipped method, but I cannot know if it is skipped unless I run it first inside the
   * Console Launcher. This is why skipped tests are basically ignored
   *
   * @param containerId
   * @param methodFullyQualifiedName method canonical name (package.class#methodName(parameters)
   */
  public void runTest(String containerId, String methodFullyQualifiedName) {
    var execResult =
        dockerHelper.runTestInsideDockerContainer(containerId, methodFullyQualifiedName);

    var parser = new ConsoleLauncherResultParser(execResult);

    if (parser.abortedTests > 0)
      throw new TestAbortedException("Some tests aborted. Check container logs");
    else if (parser.failedTests > 0)
      throw new RuntimeException("Some tests failed. Check container logs");
  }

  private static class ConsoleLauncherResultParser {
    public final int successfulTests;
    public final int failedTests;
    public final int abortedTests;
    public final int skippedTests;

    public ConsoleLauncherResultParser(String testSummary) {
      var summaryResultLines = getLastLines(testSummary, 13);

      final int LINE_OF_SUCCESSFUL_TESTS = 11;
      this.successfulTests =
          getTestNumberFromConsoleSummaryLine(summaryResultLines[LINE_OF_SUCCESSFUL_TESTS]);
      final int LINE_OF_FAILED_TESTS = 12;
      this.failedTests =
          getTestNumberFromConsoleSummaryLine(summaryResultLines[LINE_OF_FAILED_TESTS]);
      final int LINE_OF_ABORTED_TESTS = 10;
      this.abortedTests =
          getTestNumberFromConsoleSummaryLine(summaryResultLines[LINE_OF_ABORTED_TESTS]);
      final int LINE_OF_SKIPPED_TESTS = 8;
      this.skippedTests =
          getTestNumberFromConsoleSummaryLine(summaryResultLines[LINE_OF_SKIPPED_TESTS]);
    }

    private static int getTestNumberFromConsoleSummaryLine(String testSummaryLine) {
      return Integer.parseInt(testSummaryLine.replaceAll("[^0-9]", ""));
    }

    // https://stackoverflow.com/questions/43133957/how-to-get-the-last-x-lines-of-a-string-in-java
    private static String[] getLastLines(String string, int numLines) {
      List<String> lines = Arrays.asList(string.split("\n"));
      return lines
          .subList(Math.max(0, lines.size() - numLines), lines.size())
          .toArray(String[]::new);
    }
  }
}
