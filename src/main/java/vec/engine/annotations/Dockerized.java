package vec.engine.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@EnabledIfSystemProperty(named = "testingEnvironment", matches = "docker")
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
