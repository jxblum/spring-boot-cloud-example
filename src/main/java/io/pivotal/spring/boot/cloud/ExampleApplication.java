package io.pivotal.spring.boot.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Properties;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.lang.Nullable;

/**
 * The ExampleApplication class...
 *
 * @author John Blum
 * @since 1.0.0
 */
@SpringBootApplication
public class ExampleApplication {

  private static final String TEST_PROPERTY_NAME_ONE = "example.app.test.property.one";
  private static final String TEST_PROPERTY_NAME_TWO = "example.app.test.property.two";
  private static final String TEST_PROPERTY_VALUE_ONE = "test.value.one";
  private static final String TEST_PROPERTY_VALUE_TWO = "test.value.two";
  private static final String TEST_PROPERTY_SOURCE_NAME = "example.app.test.property.source";

  public static void main(String[] args) {
    SpringApplication.run(ExampleApplication.class, args);
  }

  @Bean
  ApplicationRunner environmentRunner(Environment environment) {

    return args -> {

      assertThat(environment.getProperty(TEST_PROPERTY_NAME_ONE))
        .describedAs("FAIL!")
        .isEqualTo(TEST_PROPERTY_VALUE_ONE);

      assertThat(environment.getProperty(TEST_PROPERTY_NAME_TWO))
        .describedAs("FAIL!")
        .isEqualTo(TEST_PROPERTY_VALUE_TWO);

      System.err.println("SUCCESS!");
    };
  }

  public static class ExampleEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

      Optional.ofNullable(environment)
        .filter(ConfigurableEnvironment.class::isInstance)
        .map(ConfigurableEnvironment.class::cast)
        .map(ConfigurableEnvironment::getPropertySources)
        .ifPresent(propertySources -> {

          Properties testProperties = new Properties();

          testProperties.setProperty(TEST_PROPERTY_NAME_ONE, TEST_PROPERTY_VALUE_ONE);

          // When I conditionally set a property based on whether it exists or not, this is the problem.
          // I do not think this is unreasonable nor even uncommon to do this.  Also...
          // 1) This problem does not exists if this EPP does not add properties as part of an EnumerablePropertySource
          // 2) Would not exist if the EPP was not called twice.  However, in this case it is reasonable that the EPP
          // was called twice, once for the "bootstrap" Environment and again for the "main" Environment, in that order
          // which actually suggests a problem with the BootstrapApplicationListener merge procedure, which is currently
          // behaving as all or nothing.
          if (!environment.containsProperty(TEST_PROPERTY_NAME_TWO)) {
            testProperties.setProperty(TEST_PROPERTY_NAME_TWO, TEST_PROPERTY_VALUE_TWO);
          }

          //propertySources.addLast(new ExamplePropertySource(testProperties));
          propertySources.addLast(new PropertiesPropertySource(TEST_PROPERTY_SOURCE_NAME, testProperties));
        });

    }
  }

  static class ExamplePropertySource extends PropertySource<Properties> {

    ExamplePropertySource(Properties properties) {
      super(TEST_PROPERTY_SOURCE_NAME, properties);
    }

    @Nullable @Override @SuppressWarnings("all")
    public Object getProperty(String propertyName) {
      return getSource().getProperty(propertyName);
    }
  }
}
