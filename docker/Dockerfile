FROM openjdk:15.0.2

ADD https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.7.2/junit-platform-console-standalone-1.7.2.jar /junit-console-launcher.jar
RUN chmod +rx /junit-console-launcher.jar

RUN mkdir /prj && chown 1000 /prj
USER 1000
WORKDIR /prj

ENTRYPOINT bash