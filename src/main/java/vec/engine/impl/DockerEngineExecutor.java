package vec.engine.impl;

import java.util.*;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestExecutionResult;
import vec.engine.impl.descriptors.DockerEngineDescriptor;
import vec.engine.impl.descriptors.DockerizedTestClassDescriptor;
import vec.engine.impl.descriptors.DockerizedTestMethodDescriptor;
import vec.helpers.DockerHelper;

public class DockerEngineExecutor {
  public void execute(ExecutionRequest request) {
    DockerEngineDescriptor rootDescriptor =
        (DockerEngineDescriptor) request.getRootTestDescriptor();
    var listener = request.getEngineExecutionListener();
    var dockerHelper = new DockerHelper();

    Map<String, String> containerNameIdMap =
        startAllContainers(dockerHelper, rootDescriptor.getAllContainerInfo());
    try {
      startBuildInAllContainers(dockerHelper, containerNameIdMap);
      listener.executionStarted(rootDescriptor);

      for (var clsDesc : rootDescriptor.getChildren()) {
        Preconditions.condition(
            clsDesc instanceof DockerizedTestClassDescriptor,
            String.format(
                "class descriptor %s is a non-Dockerized descriptor", clsDesc.getDisplayName()));

        listener.executionStarted(clsDesc);

        var containerExecutionResult =
            executeDockerizedMethodsInsideClass(
                (DockerizedTestClassDescriptor) clsDesc,
                listener,
                dockerHelper,
                containerNameIdMap);
        listener.executionFinished(clsDesc, containerExecutionResult);
      }

      listener.executionFinished(rootDescriptor, TestExecutionResult.successful());
    } finally {
      stopAllContainers(dockerHelper, containerNameIdMap);
    }
  }

  private TestExecutionResult executeDockerizedMethodsInsideClass(
      DockerizedTestClassDescriptor clsDesc,
      EngineExecutionListener listener,
      DockerHelper dockerHelper,
      Map<String, String> containerNameIdMap) {
    for (var methodDesc : clsDesc.getChildren()) {
      Preconditions.condition(
          methodDesc instanceof DockerizedTestMethodDescriptor,
          String.format(
              "method descriptor %s is a non-Dockerized descriptor", methodDesc.getDisplayName()));
      listener.executionStarted(methodDesc);

      var dockerizedMethodDesc = (DockerizedTestMethodDescriptor) methodDesc;
      String methodFullyQualifiedName =
          ReflectionUtils.getFullyQualifiedMethodName(
              dockerizedMethodDesc.getTestClass(), dockerizedMethodDesc.getTestMethod());
      var containerId =
          containerNameIdMap.get(
              dockerizedMethodDesc.getContainerInfo().orElseThrow().containerName);
      var testResult = runTest(dockerHelper, containerId, methodFullyQualifiedName);

      listener.executionFinished(methodDesc, testResult);
    }

    return TestExecutionResult.successful();
  }

  /**
   * return a map having the container name as key and the container id as value
   *
   * @param dockerHelper
   * @param containerInfoMap a map of container name as key and image as value
   * @return
   */
  private Map<String, String> startAllContainers(
      DockerHelper dockerHelper, Map<String, String> containerInfoMap) {
    var containerNameIdMap = new HashMap<String, String>();
    for (var containerInfo : containerInfoMap.entrySet()) {
      var containerId =
          dockerHelper.startTestingContainer(containerInfo.getValue(), containerInfo.getKey());
      containerNameIdMap.put(containerInfo.getKey(), containerId);
    }

    return containerNameIdMap;
  }

  /**
   * run "gradle testClasses" in all started containers
   *
   * @param dockerHelper
   * @param containerNameIdMap
   * @return
   */
  private void startBuildInAllContainers(
      DockerHelper dockerHelper, Map<String, String> containerNameIdMap) {
    for (var containerId : containerNameIdMap.values()) {
      dockerHelper.buildTestClasses(containerId);
    }
  }

  /**
   * stop all running containers
   *
   * @param dockerHelper
   * @param containerNameIdMap
   * @return
   */
  private void stopAllContainers(
      DockerHelper dockerHelper, Map<String, String> containerNameIdMap) {
    for (var containerId : containerNameIdMap.values()) {
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