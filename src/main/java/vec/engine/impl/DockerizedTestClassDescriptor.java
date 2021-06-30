package vec.engine.impl;

import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import vec.engine.annotations.Dockerized;

public class DockerizedTestClassDescriptor extends AbstractTestDescriptor {
  private final Class<?> testClass;

  protected DockerizedTestClassDescriptor(Class<?> testClass, TestDescriptor parent) {
    super(
        getTestClassDescriptorUniqueId(parent, testClass),
        testClass.getSimpleName() + " Dockerized",
        ClassSource.from(testClass));
    this.testClass = testClass;
    super.setParent(parent);
    addAllChildren();
  }

  public static UniqueId getTestClassDescriptorUniqueId(
      TestDescriptor parentDescriptor, Class<?> testClass) {
    return parentDescriptor.getUniqueId().append("class", testClass.getCanonicalName());
  }

  public static boolean isDockerizedTestClass(Class<?> classCandidate) {
    return !ReflectionUtils.isAbstract(classCandidate)
        && ReflectionUtils.isPublic(classCandidate)
        && AnnotationSupport.isAnnotated(classCandidate, Dockerized.class);
  }

  private void addAllChildren() {
    ReflectionUtils.findMethods(testClass, DockerizedTestMethodDescriptor::isDockerizedTestMethod)
        .stream()
        .map(method -> new DockerizedTestMethodDescriptor(method, testClass, this))
        .forEach(this::addChild);
  }

  @Override
  public Type getType() {
    return Type.CONTAINER;
  }
}
