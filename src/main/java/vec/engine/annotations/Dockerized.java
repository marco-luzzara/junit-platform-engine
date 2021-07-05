package vec.engine.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@EnabledIfEnvironmentVariable(named = "DOCKER_ONLY", matches = "true")
public @interface Dockerized {
  String image();

  String containerName();

  class ContainerInfo {
    public final String image;
    public final String containerName;

    public ContainerInfo(Dockerized dockerizedAnnotation) {
      this.image = dockerizedAnnotation.image();
      this.containerName = dockerizedAnnotation.containerName();
    }
  }
}
