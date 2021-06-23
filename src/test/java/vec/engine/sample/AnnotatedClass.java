package vec.engine.sample;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vec.engine.annotations.Dockerized;

@Dockerized
public class AnnotatedClass {
  @Test
  public void simple_Successful() {}

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  public void parameterized_Successful(int param) {}

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  public void parameterized_OnlyOneSuccessful(int param) {
    if (param == 2) Assertions.fail();
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  public void parameterized_BothFailed(int param) {
    Assertions.fail();
  }

  @Test
  public void simple_Failed() {
    Assertions.fail("This fails");
  }

  @Test
  public void simple_Aborted() {
    Assumptions.assumeTrue(false);
  }

  private void simple_Undiscovered() {}
}
