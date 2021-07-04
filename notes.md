# JUnit Launcher and TestEngine

## Brief introduction on the new architecture
When JUnit 4 was released in 2006, it provided a simple and monolithic architecture: all its functionality was concentrated inside a single big JAR file. This choice caused:

- APIs were not flexible and tools like IDE had to inspect internals of JUnit classes and use reflection in order to get needed information.
- APIs evolution was seriously reduced because of this tight coupling. 

To face these issues, JUnit 5 framework is distributed with several modules, as in the following image:

<img src="./junit5-architecture-simplified.png" alt="JUnit 5 Architecture" width="600"/>

The new modular approach allows the separation of different concerns:

- IDEs and tools can now easily discover and run tests by interfacing with the `junit-platform-launcher`.
- If you need to write a framework that requires writing a new test engine, you need to interface with `junit-platform-engine`.
- The end user writes tests helped by the APIs made public by each third-party programming model, without having to deal with the platform modules.

The following is a sequence diagram describing the discovery and execution of tests with the new architecture. Obviously, it is very simplified, e.g. listeners are left out, and may change in future versions:

<img src="./junit-platform-sequence.png" alt="JUnit 5 Architecture" width="800"/>

---

## What does the [`Launcher`](https://junit.org/junit5/docs/current/api/org.junit.platform.launcher/org/junit/platform/launcher/Launcher.html) do?
The `Launcher` is the entry point for client code that wishes to discover, filter and execute tests using one or more test engines. It has two main responsibilities:

- Determining the set of test engines to use at runtime
- Ensuring that each test engine has a unique ID.

The probability that you need to manually run a `Launcher` or even create your own one is very very low, unless you are working on a testing tool, like the java plugin for Gradle. 

### Test Discovery
This is a very basic example showing the discovery process:

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

The configurable part is the `LauncherDiscoveryRequest`, that mainly accepts filters and selectors. Assuming filter is a known concept, selectors indicate the places where tests can be found. You can specify java classes, packages, methods, but also directories and file paths.
The resulting `TestPlan` is a hierarchical (and read-only) description of all engines, classes, and test methods that fit the `LauncherDiscoveryRequest`. `TestDescriptor`s exist for the scope of a `TestEngine` only, during the construction of the `TestPlan` every descriptor is converted to a [`TestIdentifier`](https://github.com/junit-team/junit5/blob/4a9b660b3d7c6de41c728f1e9140cd8fbb682107/junit-platform-launcher/src/main/java/org/junit/platform/launcher/TestIdentifier.java), which contains the same information as a descriptor, but additionally it is `Serializable` (with its own `readObject` and `writeObject`). 

The client can traverse the tree, retrieve details about a node, and get a link to the original source (like class, method, or file position). Every node in the test plan has a unique ID used to identify a single test or a container.

Clients can also register one or more [`LauncherDiscoveryListener`](https://junit.org/junit5/docs/snapshot/api/org.junit.platform.launcher/org/junit/platform/launcher/LauncherDiscoveryListener.html) to get insights into events that occur during test discovery.

The `Java` plugin for Gradle uses the `Launcher` when `useJUnitPlatform` is specified, the implementation is publically available [here](https://github.com/gradle/gradle/blob/master/subprojects/testing-junit-platform/src/main/java/org/gradle/api/internal/tasks/testing/junitplatform/JUnitPlatformTestClassProcessor.java):

```
public class JUnitPlatformTestClassProcessor extends AbstractJUnitTestClassProcessor<JUnitPlatformSpec> {
    ...
    private class CollectAllTestClassesExecutor implements Action<String> {
        ...
        private void processAllTestClasses() {
            Launcher launcher = LauncherFactory.create();
            launcher.registerTestExecutionListeners(new JUnitPlatformTestExecutionListener(resultProcessor, clock, idGenerator));
            launcher.execute(createLauncherDiscoveryRequest(testClasses));
        }
    }
    
    private LauncherDiscoveryRequest createLauncherDiscoveryRequest(List<Class<?>> testClasses) {
        List<DiscoverySelector> classSelectors = testClasses.stream()
            .map(DiscoverySelectors::selectClass)
            .collect(Collectors.toList());

        LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request().selectors(classSelectors);

        addTestNameFilters(requestBuilder);
        addEnginesFilter(requestBuilder);
        addTagsFilter(requestBuilder);

        return requestBuilder.build();
    }
    ...
}
```

### Test Execution
To execute tests, clients can use the same `LauncherDiscoveryRequest` passed to the `discover` method or the test plan built during the discovery phase. Test progress and reporting can be achieved by registering one or more [`TestExecutionListener`](https://junit.org/junit5/docs/snapshot/api/org.junit.platform.launcher/org/junit/platform/launcher/TestExecutionListener.html)

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

To aggregate execution results you can use built-in Listeners, like [`SummaryGeneratingListener`](https://junit.org/junit5/docs/snapshot/api/org.junit.platform.launcher/org/junit/platform/launcher/listeners/SummaryGeneratingListener.html) and [`LegacyXmlReportGeneratingListener`](https://junit.org/junit5/docs/snapshot/api/org.junit.platform.reporting/org/junit/platform/reporting/legacy/xml/LegacyXmlReportGeneratingListener.html).

---

## When do I need a custom [`TestEngine`](https://junit.org/junit5/docs/current/api/org.junit.platform.engine/org/junit/platform/engine/TestEngine.html)?
Most of the times having tests in the form of Java methods is enough, and this is already covered by `JUnit Vintage` and `JUnit Jupiter` test engines. However, you might need to run tests written in a DSL, like in the case of Cucumber with Gherkin syntax, or just discover tests that are auto-generated or downloaded at runtime. `DynamicTest`s cover only the cases where the *executable code* is available at compile-time.
In special cases JUnit extensions are not suitable and powerful enough to contain your testing logic, as in the case of `jqwik`, which has a pretty complex test engine.

In general, the `TestEngine` facilitates discovery and execution of tests for a particular programming model. We already saw two concrete implementations:

- `JUnit Vintage`(`junit-vintage-engine`), which can discover and run tests written in the “old” JUnit 3 and 4 style
- `JUnit Jupiter`(`junit-jupiter-engine`), which supports the new programming model introduced in JUnit 5.

Actually, there is another engine, that is `junit-platform-suite-engine`, to execute declarative suites of tests with the launcher infrastructure. With `junit-platform-suite-api` you can setup a test suite, instead of using the vintage engine with the `@RunWith(JUnitPlatform.class)` annotation.

---

## TestEngine Registration
There are 2 ways to register a custom test engine:

- By default, engine registration is supported via Java's [`ServiceLoader`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/ServiceLoader.html) mechanism. Specifically, you have to create a new file under `src/main/resources/META-INF/services` called `org.junit.platform.engine.TestEngine`. Each line of this file corresponds to the full-name (package name + class name) of the engine to register.
- If you require fine-grained control over automatic detection and registration of test engines (and listeners) you may create an instance of [`LauncherConfig`](https://junit.org/junit5/docs/snapshot/api/org.junit.platform.launcher/org/junit/platform/launcher/core/LauncherConfig.html) and supply that to the [`LauncherFactory`](https://junit.org/junit5/docs/snapshot/api/org.junit.platform.launcher/org/junit/platform/launcher/core/LauncherFactory.html). Among other configuration properties, you can also manually add the engines you need:

```
    LauncherConfig launcherConfig = LauncherConfig.builder()
        .enableTestEngineAutoRegistration(false)
        .addTestEngines(new CustomTestEngine())
        // ...
        .build();

    try (LauncherSession session = LauncherFactory.openSession(launcherConfig)) {
        session.getLauncher().execute(request);
    }
```

Considering you generally cannot control the `LauncherConfig`s, build tools often allows you to specify which test engines you want to use, in case of `Gradle`:
```
test {
    useJUnitPlatform {
        includeEngines 'engine-id1', 'engine-id2'
    }
}
```

---

## How to create a `TestEngine`?
There are 2 approaches:

- implementing the `TestEngine` interface
- extending the `HierarchicalTestEngine` class, made available by the jupiter engine

In both the cases the discovery phase result must be a `TestDescriptor`, which is usually an instance of an `EngineDescriptor`. A `TestDescriptor` could be of type `TEST`, `CONTAINER` or `CONTAINER_AND_TEST`, which indicates a container that could potentially become a test. Basically the root descriptor (i.e. `EngineDescriptor`) is the root node of a tree where every child represents a container or a test. 

The execution phase is just about visiting the entire tree, run tests and get results using the execution listeners. The main difference between the 2 approaches is that in the second one the `execute` method simply does not exist, but every descriptor implements the `Node` interface and you have to indicate for each of them how to behave when some methods are called. The execution order of methods defined in the `Node` interface can be established by looking at the implementation of [`NodeTestTask.execute`](https://github.com/junit-team/junit5/blob/main/junit-platform-engine/src/main/java/org/junit/platform/engine/support/hierarchical/NodeTestTask.java#L87). In order to store data needed throughout the whole phase, an `EngineExecutionContext` is passed to every method.

---

## How to test your `TestEngine` implementation?
In order to test your custom `TestEngine`, the JUnit platform provides support for executing a [`TestPlan`](https://junit.org/junit5/docs/current/api/org.junit.platform.launcher/org/junit/platform/launcher/TestPlan.html) for a given `TestEngine` and then accessing the results via a fluent API to verify the expected results. This is done through the [`EngineTestKit`](https://junit.org/junit5/docs/snapshot/api/org.junit.platform.testkit/org/junit/platform/testkit/engine/EngineTestKit.html), that configure and execute your testing environment starting from a builder. Assertions are made with some utility methods that internally use AssertJ.

One of the most common need is to assert statistics against events fired during the execution of a `TestPlan`. You can both assert statistics for tests and containers, and the syntax is very similar:

```
EngineTestKit
    .engine("my-test-engine")  // test engine id
    .selectors(selectClass(ExampleTest.class)) 
    .execute() 
    .testEvents()  // or containerEvents to test container events
    .assertStatistics(stats ->
        stats.skipped(1)
            .started(3)
            .succeeded(1)
            .aborted(1)
            .failed(1));
```

If you feel that asserting statistics is insufficient for verifying appropriately the `TestPlan`, you can directly query the events fired during the execution using `assertThatEvents` method.

```
Events testEvents = EngineTestKit 
    .engine("my-test-engine")  // test engine id
    .selectors(selectClass(ExampleTest.class)) 
    .execute() 
    .testEvents()

testEvents.assertThatEvents() 
    .haveExactly(1, event(test(methodName), finishedWithFailure(
        instanceOf(ArithmeticException.class), 
        message("division by zero"))));
```

Although typically unnecessary, you might want to test the whole sequence of events fired during the execution of the `TestPlan`: in this case you can use other api like `assertEventsMatchExactly()` or `assertEventsMatchLooselyInOrder()`.

---

## Cucumber engine workaround
If you are writing a `TestEngine` and your build tool is Gradle, there are some restrictions to keep in mind. Unfortunately, you cannot customize the `Launcher` settings extensively: as for now the [test.useJUnitPlatform](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/testing/Test.html#useJUnitPlatform-org.gradle.api.Action-) method does not provide a way to change the test discovery preference, which is set to `ClassSelector`, so that only class files are considered. 

This is clearly problematic in the case of `Cucumber` because tests are in `.feature` files, that are not recognized as Java class files, of course. There are two workarounds for this:

- forcing the creation of a dummy class with the `@Cucumber` annotation in the package containing the `.feature` files. In this case the `cucumber-engine` retrieves the package name from the found class and search for all `.feature` files in that package.
- Using the `JUnit Platform Console Launcher`, which is a standalone application for launching the JUnit platform from the console and fully supports the test discovery options. Instead of specifying selectors manually, you can use the `--scan-classpath` option that scans all directories in the classpath. It can be run as a `Gradle` task as well:
```
    tasks {

    	val consoleLauncherTest by registering(JavaExec::class) {
    		dependsOn(testClasses)
    		val reportsDir = file("$buildDir/test-results")
    		outputs.dir(reportsDir)
    		classpath = sourceSets["test"].runtimeClasspath
    		main = "org.junit.platform.console.ConsoleLauncher"
    		args("--scan-classpath")
    		args("--include-engine", "cucumber")
    		args("--reports-dir", reportsDir)
    	}

    	test {
    		dependsOn(consoleLauncherTest)
    		exclude("**/*")
    	}
    }
```

---

## The [`Testable`](https://junit.org/junit5/docs/current/api/org.junit.platform.commons/org/junit/platform/commons/annotation/Testable.html) annotation
`@Testable` is used to signal to IDEs and tooling vendors that the annotated or meta-annotated element is *testable*.
Some clients of the JUnit Platform, notably IDEs such as IntelliJ IDEA, operate only on sources for test discovery. Thus, they cannot use the full runtime discovery mechanism of the JUnit Platform since it relies on compiled classes. `@Testable` therefore serves as an alternative mechanism for IDEs to discover potential tests by analyzing the source code only.

`@Testable` is typically used as a meta-annotation, that is annotates an existing annotation. `@Test` and `@TestFactory` are meta-annotated with `@Testable`. Alternatively, if concrete test classes extend from a base class, the base class can be annotated with @Testable, whic is an @Inherited annotation.

It is important to point out that a custom test engine must never perform discovery based on the presence of this annotation, which should only be meaningful to clients as IDEs and tooling vendors.

---

## Other customizations

### Discovery and PostDiscovery filters
Filters are very useful in order to run only those tests we are temporarily interested in. The junit platform distinguishes between two types of filters:

- `DiscoveryFilter<?>`, that are applied during test discovery to determine if a test or container must be included in the `TestPlan`, e.g. `ClassNameFilter` (or `PackageNameFilter`), that accepts a pattern that is going to match the class fully qualified name. For the `junit-jupiter` engine they take action in `SelectorResolver`s, like the [`ClassSelectorResolver`](https://github.com/junit-team/junit5/blob/4a9b660b3d7c6de41c728f1e9140cd8fbb682107/junit-jupiter-engine/src/main/java/org/junit/jupiter/engine/discovery/ClassSelectorResolver.java):

```
    @Override
	public Resolution resolve(ClassSelector selector, Context context) {
		Class<?> testClass = selector.getJavaClass();
		if (isTestClassWithTests.test(testClass)) {
			// Nested tests are never filtered out
			if (classNameFilter.test(testClass.getName())) {
				return toResolution(
					context.addToParent(parent -> Optional.of(newClassTestDescriptor(parent, testClass))));
			}
		}

        // ...

		return unresolved();
	}
```
- `PostDiscoveryFilter`, that is applied to `TestDescriptor`s after test discovery, e.g. `TagFilter`. The default behaviour of the `Launcher` is to filter descriptors in the [`EngineDiscoveryOrchestrator.discoverSafely`](https://github.com/junit-team/junit5/blob/4a9b660b3d7c6de41c728f1e9140cd8fbb682107/junit-platform-launcher/src/main/java/org/junit/platform/launcher/core/EngineDiscoveryOrchestrator.java) method and prune the tree if necessary, like when a container does not have any tests left:

```
    private Map<TestEngine, TestDescriptor> discoverSafely(LauncherDiscoveryRequest request, Phase phase,
			LauncherDiscoveryListener listener, Function<String, UniqueId> uniqueIdCreator) {
		Map<TestEngine, TestDescriptor> testEngineDescriptors = new LinkedHashMap<>();

		for (TestEngine testEngine : this.testEngines) {

			// ...

			TestDescriptor rootDescriptor = discoverEngineRoot(testEngine, request, listener, uniqueIdCreator);
			testEngineDescriptors.put(testEngine, rootDescriptor);
		}

		List<PostDiscoveryFilter> filters = new LinkedList<>(postDiscoveryFilters);
		filters.addAll(request.getPostDiscoveryFilters());

		applyPostDiscoveryFilters(testEngineDescriptors, filters);
		prune(testEngineDescriptors);

		return testEngineDescriptors;
	}
```

They both extend the `Filter` interface, but the post-discovery filters must never modify the `TestDescriptor` passed as argument. Only post-discovery filter can be registered for the `DefaultLauncher`, the suggested way is to leverage the `ServiceLoader` already seen for the test engines. In this case, the file to create is `/META-INF/services/org.junit.platform.launcher.PostDiscoveryFilter`.

### Listeners
As per the documentation it is possible to register 3 types of listener (but there are [many more](https://github.com/junit-team/junit5/tree/main/junit-platform-launcher/src/main/java/org/junit/platform/launcher/listeners)) and you can add each of them with the `ServiceLoader` mechanism or with manual registration, if allowed by the `Launcher` implementation:

- [`LauncherSessionListener`](https://junit.org/junit5/docs/snapshot/api/org.junit.platform.launcher/org/junit/platform/launcher/LauncherSessionListener.html): it is notified when a `LauncherSession` is opened, that is before test discovery and execution, and when it is closed.
- [`LauncherDiscoveryListener`](https://junit.org/junit5/docs/snapshot/api/org.junit.platform.launcher/org/junit/platform/launcher/LauncherDiscoveryListener.html): it is specific for the discovery phase only. It is notified at the start and at the end of the discovery phase of each test engine, when the root-level discovery begins and when it is complete.
- [`TestExecutionListener`](https://junit.org/junit5/docs/current/api/org.junit.platform.launcher/org/junit/platform/launcher/TestExecutionListener.html): it is called during the `TestPlan`'s execution, in fact every method of this listener receives a `TestIdentifier` as its argument. It lets you customize the start/end/skip event of every test (and container) and the registration of dynamic ones.
- [`EngineExecutionListener`](https://junit.org/junit5/docs/5.5.2/api/org/junit/platform/engine/EngineExecutionListener.html): Listener to be notified of test execution events by a certain test engine (not available with `ServiceLoader`).

To configure listeners registered using the `ServiceLoader` you have 3 options, described [here](https://junit.org/junit5/docs/snapshot/user-guide/index.html#running-tests-config-params).

---

## Sources
JUnit5 official documentation: [https://junit.org/junit5/docs/snapshot/user-guide/index.html#launcher-api](https://junit.org/junit5/docs/snapshot/user-guide/index.html#launcher-api)

Implementing a Custom JUnit5 Test Engine, Software matters: [https://software-matters.net/posts/custom-test-engine/](https://software-matters.net/posts/custom-test-engine/)

Cucumber test engine: [https://github.com/cucumber/cucumber-jvm/tree/main/junit-platform-engine](https://github.com/cucumber/cucumber-jvm/tree/main/junit-platform-engine)

JQwik test engine: [https://github.com/jlink/jqwik/tree/main/engine/src/main/java/net/jqwik/engine](https://github.com/jlink/jqwik/tree/main/engine/src/main/java/net/jqwik/engine)

Simple test engine to execute `main` as test method (`Mainrunner`): [https://github.com/sormuras/mainrunner/tree/main/com.github.sormuras.mainrunner.engine/main/java/com/github/sormuras/mainrunner/engine](https://github.com/sormuras/mainrunner/tree/main/com.github.sormuras.mainrunner.engine/main/java/com/github/sormuras/mainrunner/engine)

Third party test engines: [https://github.com/junit-team/junit5/wiki/Third-party-Extensions#junit-platform-test-engines](https://github.com/junit-team/junit5/wiki/Third-party-Extensions#junit-platform-test-engines)

Demo on creating a TestEngine: [Presentation](https://vimeo.com/236707523) and [Github repo](https://github.com/jlink/jax2017)