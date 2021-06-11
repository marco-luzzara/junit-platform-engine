package vec.helpers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

public class DockerHelper {
  private final DockerClient client;

  public DockerHelper() {
    var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    var dockerHttpClient =
        new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(5)
            .build();

    this.client = DockerClientImpl.getInstance(config, dockerHttpClient);
  }

  // docker run --rm -it -d -v "$(pwd)/src:/prj/src" -v "$(pwd)/build.gradle:/prj/build.gradle"
  //    --name junit-cl junit-console-launcher
  public String startTestingContainer() {
    final String projectDir = System.getProperty("user.dir");
    final String srcDir = projectDir + "/src";
    final String buildGradlePath = projectDir + "/build.gradle";
    //    final String homeDir = System.getenv("HOME");
    //    final String gradleModulesDir = homeDir + "/.gradle/caches";

    var containerId =
        client
            .createContainerCmd("junit-console-launcher")
            .withTty(true)
            .withHostConfig(
                new HostConfig()
                    .withAutoRemove(true)
                    .withBinds(
                        new Bind(srcDir, new Volume("/prj/src")),
                        new Bind(buildGradlePath, new Volume("/prj/build.gradle"))))
            // new Bind(gradleModulesDir, new Volume("/home/gradle/.gradle/caches"))))
            .withName("junit-cl")
            .exec()
            .getId();

    client.startContainerCmd(containerId).exec();

    return containerId;
  }

  // docker stop junit-cl
  public void stopTestingContainer(String containerId) {
    client.stopContainerCmd(containerId).exec();
  }

  // docker exec junit-cl gradle build
  public void buildTestClasses(final String containerId) {
    try {
      var execId =
          client
              .execCreateCmd(containerId)
              .withAttachStdout(true)
              .withAttachStderr(true)
              .withCmd("gradle", "testClasses")
              .exec()
              .getId();

      var execStartCallback = new ExecStartResultCallback();
      client.execStartCmd(execId).exec(execStartCallback).awaitCompletion();

      System.out.println(execStartCallback.getExecOutput());
    } catch (InterruptedException exc) {
      throw new RuntimeException(exc);
    }
  }

  // docker exec junit-cl bash -c 'java -jar /junit-console-launcher.jar -cp $(gradle -q
  // printClassPath) \
  //    -E="docker-engine" --details=summary --disable-banner \
  //    -m "vec.myproject.EmployeeOnDockerTest#computeSalary_workedForNHours_salaryIsNTimes10"'
  public String runTestInsideDockerContainer(String containerId, String methodFullyQualifiedName) {
    try {
      var junitConsoleLauncherCommand =
          String.format(
              "java -jar /junit-console-launcher.jar -cp $(gradle -q printClassPath) -E=\"docker-engine\" --details=summary --disable-banner -m \"%s\"",
              methodFullyQualifiedName);

      var execId =
          client
              .execCreateCmd(containerId)
              .withAttachStdout(true)
              .withAttachStderr(true)
              .withCmd("bash", "-c", junitConsoleLauncherCommand)
              .exec()
              .getId();

      var execStartCallback = new ExecStartResultCallback();
      client.execStartCmd(execId).exec(execStartCallback).awaitCompletion();

      var execOutput = execStartCallback.getExecOutput();
      System.out.println(execOutput);

      return execOutput;
    } catch (InterruptedException exc) {
      throw new RuntimeException(exc);
    }
  }

  private static class ExecStartResultCallback extends ResultCallback.Adapter<Frame> {
    private String execOutput = "";

    @Override
    public void onNext(Frame frame) {
      var payload = new String(frame.getPayload());
      execOutput += payload;
    }

    public String getExecOutput() {
      return execOutput;
    }
  }
}
