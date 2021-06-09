package vec.engine.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.platform.commons.annotation.Testable;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Testable
@EnabledIfEnvironmentVariable(named = "DOCKER_ONLY", matches = "true")
public @interface Dockerized {}
