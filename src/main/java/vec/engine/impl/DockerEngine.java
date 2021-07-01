package vec.engine.impl;

import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;
import vec.engine.impl.descriptors.DockerEngineDescriptor;
import vec.engine.impl.descriptors.DockerizedTestClassDescriptor;
import vec.engine.impl.descriptors.DockerizedTestMethodDescriptor;

public class DockerEngine extends HierarchicalTestEngine<DockerEngineExecutionContext> {
  private static final String ENGINE_ID = "docker-engine";

  @Override
  public String getId() {
    return ENGINE_ID;
  }

  @Override
  protected DockerEngineExecutionContext createExecutionContext(ExecutionRequest request) {
    return new DockerEngineExecutionContext();
  }

  @Override
  public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
    DockerEngineDescriptor engineDescriptor = new DockerEngineDescriptor(uniqueId);

    discoveryRequest
        .getSelectorsByType(ClassSelector.class)
        .forEach(selector -> appendTestsInClass(selector.getJavaClass(), engineDescriptor));

    return engineDescriptor;
  }

  private void appendTestsInClass(Class<?> javaClass, TestDescriptor engineDescriptor) {
    if (ReflectionUtils.findMethods(
                javaClass, DockerizedTestMethodDescriptor::isDockerizedTestMethod)
            .size()
        > 0)
      engineDescriptor.addChild(new DockerizedTestClassDescriptor(javaClass, engineDescriptor));
  }
}
