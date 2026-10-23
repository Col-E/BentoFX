package software.coley.gradle.plugin.conventions

@SuppressWarnings("unused")
final class TestFixtures {
	public static final String TEST_FIXTURES_COMPILATION_NAME = 'testFixtures'

	private TestFixtures() {
		throw new UnsupportedOperationException(
				'Utility classes should not be instantiated.'
		)
	}
}
