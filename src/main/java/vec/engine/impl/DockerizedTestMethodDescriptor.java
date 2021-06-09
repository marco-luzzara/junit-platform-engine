package vec.engine.impl;

import java.lang.reflect.Method;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;
import vec.engine.annotations.Dockerized;

public class DockerizedTestMethodDescriptor extends AbstractTestDescriptor {
  private final Method testMethod;
  private final Class testClass;

  public DockerizedTestMethodDescriptor(
      Method testMethod, Class<?> testClass, DockerizedTestClassDescriptor parent) {
    super(
        getTestMethodDescriptorUniqueId(parent, testMethod),
        testMethod.getName(),
        MethodSource.from(testMethod));
    this.testMethod = testMethod;
    this.testClass = testClass;
    setParent(parent);
  }

  private static UniqueId getTestMethodDescriptorUniqueId(
      TestDescriptor parentDescriptor, Method testMethod) {
    return parentDescriptor.getUniqueId().append("method", testMethod.getName());
  }

  public static boolean isDockerizedTestMethod(Method methodCandidate) {
    var declaringClass = methodCandidate.getDeclaringClass();

    return ReflectionUtils.isNotStatic(methodCandidate)
        && ReflectionUtils.isPublic(methodCandidate)
        && methodCandidate.getReturnType().equals(void.class)
        && (DockerizedTestClassDescriptor.isDockerizedTestClass(declaringClass)
            || (isPublicAndConcreteClass(declaringClass)
                && AnnotationSupport.isAnnotated(methodCandidate, Dockerized.class)));
  }

  private static boolean isPublicAndConcreteClass(Class<?> cls) {
    return !ReflectionUtils.isAbstract(cls) && ReflectionUtils.isPublic(cls);
  }

  public Method getTestMethod() {
    return testMethod;
  }

  public Class<?> getTestClass() {
    return testClass;
  }

  @Override
  public Type getType() {
    return Type.TEST;
  }
}
