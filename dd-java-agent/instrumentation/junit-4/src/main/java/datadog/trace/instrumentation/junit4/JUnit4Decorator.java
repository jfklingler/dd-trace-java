package datadog.trace.instrumentation.junit4;

import datadog.trace.api.DDSpanTypes;
import datadog.trace.api.DDTags;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.decorator.TestDecorator;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

@Slf4j
public class JUnit4Decorator extends TestDecorator {
  public static final JUnit4Decorator DECORATE = new JUnit4Decorator();

  private static final String JUNIT4 = "junit4";

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"junit", "junit-4"};
  }

  @Override
  protected String spanType() {
    return DDSpanTypes.TEST;
  }

  @Override
  protected String component() {
    return "junit";
  }

  public void onTestStart(final AgentSpan span, final Description description) {
    final String testSuite = description.getClassName();
    final String testName = description.getMethodName();

    span.setTag(DDTags.RESOURCE_NAME, testSuite + "." + testName);

    span.setTag(DDTags.TEST_SUITE, testSuite);
    span.setTag(DDTags.TEST_NAME, testName);
    span.setTag(DDTags.TEST_FRAMEWORK, JUNIT4);
    span.setTag(DDTags.TEST_STATUS, TEST_PASS);
  }

  public void onTestFinish(final AgentSpan span) {}

  public void onTestFailure(final AgentSpan span, final Failure failure) {
    final Throwable throwable = failure.getException();
    if (throwable != null) {
      span.setError(true);
      span.addThrowable(throwable);
      span.setTag(DDTags.TEST_STATUS, TEST_FAIL);
    }
  }

  public void onTestIgnored(final AgentSpan span, final Description description) {
    onTestStart(span, description);
    span.setTag(DDTags.TEST_STATUS, TEST_SKIP);
  }
}
