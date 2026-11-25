package software.coley.gradle.conventions.test

@SuppressWarnings("unused")
final class TestSuites {
	public static final String INTEGRATION_TEST = 'integrationTest'
	public static final String INTEGRATION_TEST_GRAPHICAL = 'integrationTestGraphical'
	public static final String INTEGRATION_TEST_PARALLEL = 'integrationTestParallel'
	public static final String UNIT_TEST = 'test'

	private TestSuites() {
		throw new UnsupportedOperationException(
				'Utility classes should not be instantiated.'
		)
	}
}
