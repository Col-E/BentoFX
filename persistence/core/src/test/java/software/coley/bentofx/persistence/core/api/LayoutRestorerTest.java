package software.coley.bentofx.persistence.core.api;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatCode;

class LayoutRestorerTest {

	/**
	 * An implementation that owns no resources has nothing to release, so the
	 * default is a no-op rather than forcing every implementation to override
	 * {@code close()}.
	 */
	@Test
	void closeDefaultsToANoOp() {
		final LayoutRestorer restorer = new LayoutRestorer() {
			@Override
			public boolean doesLayoutExist() {
				return false;
			}

			@Override
			public DockingLayout restoreLayout(
					final Supplier<DockingLayout> defaultLayoutSupplier
			) {
				return defaultLayoutSupplier.get();
			}
		};

		assertThatCode(restorer::close)
				.describedAs("close() with no override")
				.doesNotThrowAnyException();
	}
}
