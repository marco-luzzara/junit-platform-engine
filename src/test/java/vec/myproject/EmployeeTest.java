package vec.myproject;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static vec.myproject.EmployeeAssert.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import vec.engine.annotations.Dockerized;

public class EmployeeTest {
  @DisplayName("Constructor with invalid fullname")
  @ParameterizedTest(name = "[{index}] {displayName} - {argumentsWithNames}")
  @NullAndEmptySource
  public void constructor_invalidFullname_throw(String invalidFullname) {
    assertThatThrownBy(() -> new Employee(invalidFullname))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_validFullname() {
    var empl = new Employee("emp1");

    assertAll(
        () -> assertThat(empl).hasFullname("emp1"), () -> assertThat(empl).hasWorkingHours(0));
  }

  @Dockerized(containerName = "junit-cl", image = "junit-console-launcher")
  @Test
  public void workFor_nHours_addNToWorkingHours() {
    var empl = new Employee("emp1");

    empl.workFor(5);
    empl.workFor(10);

    assertThat(empl).hasWorkingHours(15);
  }
}
