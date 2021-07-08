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

  /**
   * start the testing container with 3 bind mounts: - build/libs folder where the fat jar is
   * created - build/classes/java containing all the .class files - build/resources/main for the
   * test engine registration container name is junit-cl
   *
   * @return the id of the started container
   */
  //  docker run --rm -it -v "$(pwd)/build/libs:/prj/build/libs" \
  //          -v "$(pwd)/build/classes/java:/prj/build/classes/java" \
  //          -v "$(pwd)/build/resources:/prj/build/resources" \
  //          --name {containerName} {image}
  public String startTestingContainer(String image, String containerName) {
    final String projectDir = System.getProperty("user.dir");
    final String buildDir = projectDir + "/build";

    var containerId =
        client
            .createContainerCmd(image)
            .withTty(true)
            .withHostConfig(
                new HostConfig()
                    .withAutoRemove(true)
                    .withBinds(
                        new Bind(buildDir + "/classes/java", new Volume("/prj/build/classes/java")),
                        new Bind(buildDir + "/resources", new Volume("/prj/build/resources")),
                        new Bind(buildDir + "/libs", new Volume("/prj/build/libs"))))
            .withName(containerName)
            .exec()
            .getId();

    client.startContainerCmd(containerId).exec();

    return containerId;
  }

  /**
   * Stops the container with id containerId
   *
   * @param containerId
   */
  // docker stop junit-cl
  public void stopTestingContainer(String containerId) {
    client.stopContainerCmd(containerId).exec();
  }

  /**
   * calls the junit console launcher inside the container and returns the summary of the execution.
   * the only selector given is the methodFullyQualifiedName passed in input. The classpath includes
   * the classes, resources and the fat jar created with the shadow plugin
   *
   * @param containerId
   * @param methodFullyQualifiedName
   * @return the output of the executed command
   */
  // docker exec junit-cl docker exec junit-cl java -DtestingEnvironment=docker -jar \
  // /junit-console-launcher.jar -cp \
  // build/classes/java/test:build/classes/java/main:build/resources/main:build/libs/junit-custom-engine-1.0-SNAPSHOT-tests.jar \
  //    -E="docker-engine" --details=summary --disable-banner \
  //    -m "vec.myproject.EmployeeOnDockerTest#computeSalary_workedForNHours_salaryIsNTimes10"
  public String runTestInsideDockerContainer(String containerId, String methodFullyQualifiedName) {
    try {
      var execId =
          client
              .execCreateCmd(containerId)
              .withAttachStdout(true)
              .withAttachStderr(true)
              .withCmd(
                  "java",
                  "-DtestingEnvironment=docker",
                  "-jar",
                  "/junit-console-launcher.jar",
                  "-cp",
                  "build/classes/java/test:build/classes/java/main:build/resources/main:build/libs/junit-custom-engine-1.0-SNAPSHOT-tests.jar",
                  "-E=\"docker-engine\"",
                  "--details=summary",
                  "--disable-banner",
                  "-m",
                  methodFullyQualifiedName)
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
