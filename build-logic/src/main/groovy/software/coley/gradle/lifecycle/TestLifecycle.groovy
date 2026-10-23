package software.coley.gradle.lifecycle

@SuppressWarnings("unused")
final class TestLifecycle {
	public static final String CHECK_INTEGRATION_TASK_NAME = 'checkIntegration'
	public static final String CHECK_ALL_TASK_NAME = 'checkAll'

	public static final ENABLE_CSV_REPORT = false
	public static final ENABLE_HTML_REPORT = true
	public static final ENABLE_XML_REPORT = true

	private TestLifecycle() {
		throw new UnsupportedOperationException(
				'Utility classes should not be instantiated.'
		)
	}
}
