package vec.engine.impl.descriptors;

import com.google.common.base.Preconditions;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;
import vec.engine.impl.DockerEngineExecutionContext;

public class DockerEngineDescriptor extends EngineDescriptor
    implements Node<DockerEngineExecutionContext> {
  public DockerEngineDescriptor(UniqueId uniqueId) {
    super(uniqueId, "Docker Engine");
  }

  @Override
  public DockerEngineExecutionContext prepare(DockerEngineExecutionContext context) {
    context.prepareDockerContainers();
    return context;
  }

  @Override
  public void cleanUp(DockerEngineExecutionContext context) {
    Preconditions.checkState(context.getContainerIds().length > 0);
    context.cleanUpDockerContainer();
  }
}
