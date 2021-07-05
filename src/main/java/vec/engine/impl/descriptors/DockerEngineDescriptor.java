package vec.engine.impl.descriptors;

import java.util.HashMap;
import java.util.Map;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;
import vec.engine.annotations.Dockerized;
import vec.engine.impl.DockerEngineExecutionContext;
import vec.engine.interfaces.DockerizableDescriptor;

public class DockerEngineDescriptor extends EngineDescriptor
    implements Node<DockerEngineExecutionContext> {
  public DockerEngineDescriptor(UniqueId uniqueId) {
    super(uniqueId, "Docker Engine");
  }

  /**
   * the key is the container name, the value is the container image. it visits all the children
   * retrieving the ContainerInfo if present.
   */
  public Map<String, String> getAllContainerInfo() {
    Map<String, String> containerInfoMap = new HashMap<>();
    for (var descendant : this.getDescendants()) {
      org.junit.platform.commons.util.Preconditions.condition(
          descendant instanceof DockerizableDescriptor,
          String.format(
              "engine descriptor %s contains a non-Dockerizable descriptor", this.getUniqueId()));
      var dockDescendant = (DockerizableDescriptor) descendant;

      var optionalContInfo = dockDescendant.getContainerInfo();
      if (optionalContInfo.isEmpty()) continue;

      var containerInfo = optionalContInfo.get();
      checkContainerNameHasUniqueImage(containerInfoMap, containerInfo);

      containerInfoMap.putIfAbsent(containerInfo.containerName, containerInfo.image);
    }

    return containerInfoMap;
  }

  private void checkContainerNameHasUniqueImage(
      Map<String, String> containerInfoMap, Dockerized.ContainerInfo containerInfo) {
    org.junit.platform.commons.util.Preconditions.condition(
        !containerInfoMap.containsKey(containerInfo.containerName)
            || containerInfoMap.get(containerInfo.containerName).equals(containerInfo.image),
        String.format(
            "Container %s references 2 different images: %s, %s",
            containerInfo.containerName,
            containerInfo.image,
            containerInfoMap.get(containerInfo.containerName)));
  }

  @Override
  public DockerEngineExecutionContext prepare(DockerEngineExecutionContext context) {
    var containerInfoMap = getAllContainerInfo();
    context.startDockerContainers(containerInfoMap);

    return context;
  }

  @Override
  public void cleanUp(DockerEngineExecutionContext context) {
    context.cleanUpDockerContainer();
  }
}
