# JUnit Platform and `TestEngine`s

## Summary
This project includes:
- **branch** `presentation`: a presentation about the JUnit Platform, in particular on the `Launcher` and how to implement a custom `TestEngine`.
- **branch** `doc`: the results of my research on the same subjects treated in the presentation. For more information read `notes.md`.
- **branch** `main` and `from_TestEngine`: these branches contain (finally) code showing two possible approaches
used to implement a custom `TestEngine`.
  
## About the `TestEngine`

---

**Note**: this was born as a demo, but I spent some time on it and if someone believes this engine has useful applications, feel free to contribute.

---

The purpose of the implemented `TestEngine` is to run `@Dockerized`-annotated methods/classes in a docker container. 
The base image of the container is the one given in `docker/Dockerfile`.

This annotation is defined in this way:

``` java
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@EnabledIfSystemProperty(named = "testingEnvironment", matches = "docker")
public @interface Dockerized {
  String image();

  String containerName();

  // ...
}
```

All annotated methods/classes are ignored unless you provide the system property `testingEnvironment=docker`, that is automatically added when run using the `docker-engine` container. 

### How is the specified container run?
Every container must have the JUnit Console Launcher, which is used to run single methods. Some bind-volumes are created:

- `-v "$(pwd)/build/libs:/prj/build/libs"`: in the `libs` folder it finds a fat jar with all the dependencies (and test dependencies) declared in Gradle. This jar is created with [Shadow](https://imperceptiblethoughts.com/shadow/).
- `-v "$(pwd)/build/classes/java:/prj/build/classes/java"`: this folder contains the test code, which is necessary in order to tell to the Console Launcher (through the classpath) where to find tests.
- `-v "$(pwd)/build/resources:/prj/build/resources"`: for the services injected with the `ServiceLoader`.

### How tests are actually run inside the container?
Here is where the Console Launcher does its work. Among other options:
- `-E=docker-engine`: to exclude discovery of dockerized methods, since you already are in a container.
- `-DtestingEnvironment=docker`: this option is applied to `java` command for the system property setup.
- `-m "package.classname#method(parameters)"`: the only selector given is the method selector. This is because only when you run methods one by one, you have test metrics as close as possible to the actual ones (consider the `docker exec` overhead).

---

For this version of the demo, the execution phase is hidden by the `HierarchicalTestEngine` implementation, but it can be done manually by implementing the `TestEngine` interface (check the `from_TestEngine` branch).