package vec.myproject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static vec.myproject.EmployeeAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

public class EmployeeTest {
  @ParameterizedTest
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

  @Test
  public void workFor_nHours_addNToWorkingHours() {
    var empl = new Employee("emp1");

    empl.workFor(5);
    empl.workFor(10);

    assertThat(empl).hasWorkingHours(15);
  }

  @Test
  public void computeSalary_workedForNHours_salaryIsNTimes10() {
    var empl = new Employee("emp1");
    empl.workFor(5);

    var salary = empl.computeSalary();

    assertThat(salary).isEqualTo(5 * 10);
  }

  @ParameterizedTest
  @CsvSource({"valid mail, true", "Do not send, false"})
  public void sendMailTo_startsWithDoNotSend_mailNotSent(String message, boolean shouldSendMail) {
    var empl1 = new Employee("emp1");
    var empl2 = new Employee("emp2");

    var isMailSent = empl1.sendMailTo(empl2, message);

    assertThat(isMailSent).isEqualTo(shouldSendMail);
  }
}
