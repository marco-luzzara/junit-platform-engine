# JUnit Launcher and TestEngine

## Why do I need a Custom JUnit5 Engine
Most of the cases having tests in the form of Java classes is enough, and this is already covered by `JUnit Vintage` and `JUnit Jupiter` test engines. However, you might need to run tests written in a DSL, like in the case of Cucumber with Gherkin syntax, or just discover tests that are auto-generated or downloaded at runtime. 

---

## What is the difference between [`Launcher`](https://junit.org/junit5/docs/current/api/org.junit.platform.launcher/org/junit/platform/launcher/Launcher.html) and [`TestEngine`](https://junit.org/junit5/docs/current/api/org.junit.platform.engine/org/junit/platform/engine/TestEngine.html)?
They are both interfaces but the `Launcher` is the entry point for client code that wishes to discover and execute tests using one or more test engines. It has two main responsibilities:

- Determining the set of test engines to use at runtime
- Ensuring that each test engine has a unique ID.

The `TestEngine` facilitates discovery and execution of tests for a particular programming model. We already saw two concrete implementations:

- `JUnit Vintage`(`junit-vintage-engine`), which can discover and run tests written in the “old” JUnit 3 and 4 style
- `JUnit Jupiter`(`junit-jupiter-engine`) which supports the new programming model introduced in JUnit 5.

Actually, there is another "engine", `junit-platform-suite-engine`, to execute declarative suites of tests with the launcher infrastructure. With `junit-platform-suite-api` you can setup a test suite, instead of using the vintage engine with the `@RunWith(JUnitPlatform.class)` annotation.

---

## The `Launcher`

JUnit introduces the concept of `Launcher`, used to discover, filter and execute tests.

```
LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
    .selectors(
        selectPackage("com.example.mytests"),
        selectClass(MyTestClass.class)
    )
    .filters(
        includeClassNamePatterns(".*Tests")
    )
    .build();

try (LauncherSession session = LauncherFactory.openSession()) {
    TestPlan testPlan = session.getLauncher().discover(request);

    // ... discover additional test plans or execute tests
}
```

The resulting `TestPlan` is a hierarchical (and read-only) description of all engines, classes, and test methods that fit the `LauncherDiscoveryRequest`. The client can traverse the tree, retrieve details about a node, and get a link to the original source (like class, method, or file position). Every node in the test plan has a unique ID that can be used to invoke a particular test or group of tests.

Clients can also register one or more [`LauncherDiscoveryListener`](https://junit.org/junit5/docs/snapshot/api/org.junit.platform.launcher/org/junit/platform/launcher/LauncherDiscoveryListener.html) to get insights into events that occur during test discovery.

### Test Execution
To execute tests, clients can use the same `LauncherDiscoveryRequest` used in the discovery phase or create a new one. Test progress and reporting can be achieved by registering one or more [`TestExecutionListener`](https://junit.org/junit5/docs/snapshot/api/org.junit.platform.launcher/org/junit/platform/launcher/TestExecutionListener.html)

```
SummaryGeneratingListener listener = new SummaryGeneratingListener();

try (LauncherSession session = LauncherFactory.openSession()) {
    Launcher launcher = session.getLauncher();
    // Register a listener of your choice
    launcher.registerTestExecutionListeners(listener);
    // Discover tests and build a test plan using the previous LauncherDiscoveryRequest
    TestPlan testPlan = launcher.discover(request);
    // Execute test plan
    launcher.execute(testPlan);
    // Alternatively, execute the request directly
    launcher.execute(request);
}

TestExecutionSummary summary = listener.getSummary();
// Do something with the summary...
```

To aggregate execution results you can use built-in Listeners, like [`SummaryGeneratingListener`](https://junit.org/junit5/docs/snapshot/api/org.junit.platform.launcher/org/junit/platform/launcher/listeners/SummaryGeneratingListener.html) and [`LegacyXmlReportGeneratingListener`](https://junit.org/junit5/docs/snapshot/api/org.junit.platform.reporting/org/junit/platform/reporting/legacy/xml/LegacyXmlReportGeneratingListener.html) or create a new one.

## TestEngine Registration
There are 2 ways to register a custom test engine:

- By default, engine registration is supported via Java’s [`ServiceLoader`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/ServiceLoader.html) mechanism.

## Sources
JUnit5 official documentation: [https://junit.org/junit5/docs/snapshot/user-guide/index.html#launcher-api](https://junit.org/junit5/docs/snapshot/user-guide/index.html#launcher-api)

Implementing a Custom JUnit5 Test Engine, Software matters: [https://software-matters.net/posts/custom-test-engine/](https://software-matters.net/posts/custom-test-engine/)

Cucumber custom engine: [https://github.com/cucumber/cucumber-jvm/tree/main/junit-platform-engine](https://github.com/cucumber/cucumber-jvm/tree/main/junit-platform-engine)

















