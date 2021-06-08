package vec.myproject;

public class Employee {
  private final String fullname;
  private Integer workingHours = 0;

  public Employee(String fullname) {
    if (fullname == null || fullname.isEmpty())
      throw new IllegalArgumentException("fullname cannot be blank");

    this.fullname = fullname;
  }

  public void workFor(int hours) {
    this.workingHours += hours;
  }

  public int computeSalary() {
    return workingHours * 10;
  }

  public boolean sendMailTo(Employee empl, String message) {
    return !message.startsWith("Do not send");
  }
}
