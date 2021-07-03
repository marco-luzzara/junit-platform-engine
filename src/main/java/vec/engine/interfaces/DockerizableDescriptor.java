package vec.engine.interfaces;

import java.util.Optional;
import vec.engine.annotations.Dockerized;

public interface DockerizableDescriptor {
  Optional<Dockerized.ContainerInfo> getContainerInfo();
}
