package vec.engine.sample;

import org.junit.jupiter.api.*;
import vec.engine.annotations.Dockerized;

public class AnnotatedSingleTests {
  @Test
  @Dockerized
  public void simple_Successful() {}

  public void simple_Undiscovered() {}

  @Test
  @Dockerized
  public void simple_Failed() {
    Assertions.fail("Another failure");
  }

  @Test
  @Dockerized
  public void simple_Aborted() {
    Assumptions.assumeTrue(false, "Assumption not satisfied");
  }
}
