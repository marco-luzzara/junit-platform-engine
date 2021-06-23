package vec.myproject;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vec.engine.annotations.Dockerized;

@Dockerized
public class EmployeeOnDockerTest {
  @Test
  public void computeSalary_workedForNHours_salaryIsNTimes10() {
    var empl = new Employee("emp1");
    empl.workFor(5);

    var salary = empl.computeSalary();

    assertThat(salary).isEqualTo(5 * 10);
  }

  @DisplayName("send mail with different messages")
  @ParameterizedTest(name = "[{index}] {displayName} - {argumentsWithNames}")
  @CsvSource({"valid mail, true", "Do not send, false"})
  public void sendMailTo_ValidAndInvalid(String message, boolean shouldSendMail) {
    var empl1 = new Employee("emp1");
    var empl2 = new Employee("emp2");

    var isMailSent = empl1.sendMailTo(empl2, message);

    assertThat(isMailSent).isEqualTo(shouldSendMail);
  }
}
