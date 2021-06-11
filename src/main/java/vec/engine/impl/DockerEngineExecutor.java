package vec.engine.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import vec.helpers.DockerHelper;

public class DockerEngineExecutor {
  public void execute(ExecutionRequest request) {
    TestDescriptor rootDescriptor = request.getRootTestDescriptor();
    var listener = request.getEngineExecutionListener();
    var dockerHelper = new DockerHelper();

    var containerId = dockerHelper.startTestingContainer();

    try {
      dockerHelper.buildTestClasses(containerId);
      listener.executionStarted(rootDescriptor);

      for (var clsDesc : (Set<DockerizedTestClassDescriptor>) rootDescriptor.getChildren()) {
        listener.executionStarted(clsDesc);

        for (var methodDesc : (Set<DockerizedTestMethodDescriptor>) clsDesc.getChildren()) {
          listener.executionStarted(methodDesc);

          String methodFullyQualifiedName =
              ReflectionUtils.getFullyQualifiedMethodName(
                  methodDesc.getTestClass(), methodDesc.getTestMethod());
          var testResult = runTest(dockerHelper, containerId, methodFullyQualifiedName);

          listener.executionFinished(methodDesc, testResult);
        }

        listener.executionFinished(clsDesc, TestExecutionResult.successful());
      }

      listener.executionFinished(rootDescriptor, TestExecutionResult.successful());
    } finally {
      dockerHelper.stopTestingContainer(containerId);
    }
  }

  private TestExecutionResult runTest(
      DockerHelper dockerHelper, String containerId, String methodFullyQualifiedName) {
    var execResult =
        dockerHelper.runTestInsideDockerContainer(containerId, methodFullyQualifiedName);

    var parser = new ConsoleLauncherResultParser(execResult);

    if (parser.successfulTests > 0
        && parser.failedTests == 0
        && parser.abortedTests == 0
        && parser.skippedTests == 0) return TestExecutionResult.successful();
    else if (parser.abortedTests > 0)
      return TestExecutionResult.aborted(
          new RuntimeException("Some tests aborted. See container logs"));
    else if (parser.failedTests > 0)
      return TestExecutionResult.failed(
          new RuntimeException("Some tests failed. See container logs"));
    else throw new RuntimeException("See container logs");
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
