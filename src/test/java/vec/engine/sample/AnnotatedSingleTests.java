package vec.engine.sample;

import org.junit.jupiter.api.*;
import vec.engine.annotations.Dockerized;

public class AnnotatedSingleTests {
  @Test
  @Dockerized(containerName = "junit-cl", image = "junit-console-launcher")
  public void simple_Successful() {}

  public void simple_Undiscovered() {}

  @Test
  @Dockerized(containerName = "junit-cl1", image = "junit-console-launcher")
  public void simple_Failed() {
    Assertions.fail("Another failure");
  }

  @Test
  @Dockerized(containerName = "junit-cl", image = "junit-console-launcher")
  public void simple_Aborted() {
    Assumptions.assumeTrue(false, "Assumption not satisfied");
  }
}
