import datadog.trace.agent.test.AgentTestRunner
import datadog.trace.api.DDTags
import datadog.trace.api.DisableTestTrace
import datadog.trace.bootstrap.instrumentation.api.Tags
import datadog.trace.bootstrap.instrumentation.decorator.TestDecorator
import org.junit.runner.JUnitCore
import spock.lang.Shared

class JUnit4Test extends AgentTestRunner {

  @Shared
  def runner = new JUnitCore()

  @DisableTestTrace(reason = "avoid self-tracing")
  def "test success generate spans"() {
    setup:
    runner.run(TestSucceed)

    expect:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          operationName "junit.test"
          resourceName "TestSucceed.test_succeed"
          spanType "test"
          tags {
            "$Tags.COMPONENT" "junit"
            "$DDTags.TEST_SUITE" "TestSucceed"
            "$DDTags.TEST_NAME" "test_succeed"
            "$DDTags.TEST_FRAMEWORK" "junit4"
            "$DDTags.TEST_STATUS" "$TestDecorator.TEST_PASS"
            defaultTags()
          }
        }
      }
    }
  }
}
