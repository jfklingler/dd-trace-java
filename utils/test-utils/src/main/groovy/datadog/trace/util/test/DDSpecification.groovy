package datadog.trace.util.test

import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.Transformer
import spock.lang.Specification

import static net.bytebuddy.description.modifier.FieldManifestation.VOLATILE
import static net.bytebuddy.description.modifier.Ownership.STATIC
import static net.bytebuddy.description.modifier.Visibility.PUBLIC
import static net.bytebuddy.matcher.ElementMatchers.named
import static net.bytebuddy.matcher.ElementMatchers.none

abstract class DDSpecification extends Specification {
  private static final String CONFIG = "datadog.trace.api.Config"
  private static final String CAPTURED_ENV = "datadog.trace.api.env.CapturedEnvironment"

  static {
    makeConfigInstanceModifiable()
    //cleanCapturedEnvironment()
  }

  // Keep track of config instance already made modifiable
  private static isConfigInstanceModifiable = false

  static void makeConfigInstanceModifiable() {
    if (isConfigInstanceModifiable) {
      return
    }

    def instrumentation = ByteBuddyAgent.install()
    new AgentBuilder.Default()
      .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
      .with(AgentBuilder.RedefinitionStrategy.Listener.ErrorEscalating.FAIL_FAST)
    // Config is injected into the bootstrap, so we need to provide a locator.
      .with(
        new AgentBuilder.LocationStrategy.Simple(
          ClassFileLocator.ForClassLoader.ofSystemLoader()))
      .ignore(none()) // Allow transforming bootstrap classes
      .type(named(CONFIG))
      .transform { builder, typeDescription, classLoader, module ->
        builder
          .field(named("INSTANCE"))
          .transform(Transformer.ForField.withModifiers(PUBLIC, STATIC, VOLATILE))
      }
    // Making runtimeId modifiable so that it can be preserved when resetting config in tests
      .transform { builder, typeDescription, classLoader, module ->
        builder
          .field(named("runtimeId"))
          .transform(Transformer.ForField.withModifiers(PUBLIC, VOLATILE))
      }
      .installOn(instrumentation)
    isConfigInstanceModifiable = true
  }

  //Clean captured environment to avoid affecting Config settings with platform dependant properties.
  static void cleanCapturedEnvironment() {
    try {
      def capturedEnvClass = this.getClassLoader().loadClass(CAPTURED_ENV)
      def capturedEnvField = capturedEnvClass.getDeclaredField("INSTANCE")
      capturedEnvField.setAccessible(true)

      def propsField = capturedEnvClass.getDeclaredField("properties")
      propsField.setAccessible(true)
      propsField.set(capturedEnvField.get(null), new HashMap<String, String>())
    } catch (final ClassNotFoundException ignored) {
      //If CapturedEnvironment class is not found, no actions needed.
    }
  }
}
