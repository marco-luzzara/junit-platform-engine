package vec.engine.impl.descriptors;

import java.util.Optional;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import vec.engine.annotations.Dockerized;
import vec.engine.interfaces.DockerizableDescriptor;

public class DockerizedTestClassDescriptor extends AbstractTestDescriptor
    implements DockerizableDescriptor {
  private final Class<?> testClass;

  public DockerizedTestClassDescriptor(Class<?> testClass, TestDescriptor parent) {
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

  /**
   * this method could return false even for a DockerizedTestClassDescriptor, which could be created
   * as a container for DockerizedTestMethodDescriptor. In this case the class is not
   * actually @Dockerized, but only some methods are.
   *
   * @param classCandidate
   * @return
   */
  public static boolean isDockerizedTestClass(Class<?> classCandidate) {
    return !ReflectionUtils.isAbstract(classCandidate)
        && ReflectionUtils.isPublic(classCandidate)
        && AnnotationSupport.isAnnotated(classCandidate, Dockerized.class);
  }

  public Optional<Dockerized.ContainerInfo> getContainerInfo() {
    if (!isDockerizedTestClass(this.testClass)) return Optional.empty();

    var dockerizedAnnotation =
        AnnotationSupport.findAnnotation(this.testClass, Dockerized.class).orElseThrow();

    return Optional.of(new Dockerized.ContainerInfo(dockerizedAnnotation));
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
