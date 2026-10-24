package software.coley.bentofx.persistence.core.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class LayoutSaverTest {

	/**
	 * An implementation that owns no resources has nothing to release, so the
	 * default is a no-op rather than forcing every implementation to override
	 * {@code close()}.
	 */
	@Test
	void closeDefaultsToANoOp() {
		final LayoutSaver saver = () -> { };

		assertThatCode(saver::close)
				.describedAs("close() with no override")
				.doesNotThrowAnyException();
	}
}
