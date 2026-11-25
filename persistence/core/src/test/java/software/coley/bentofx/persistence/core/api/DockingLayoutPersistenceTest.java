package software.coley.bentofx.persistence.core.api;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.impl.provider.DefaultDockingLayoutPersistenceProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DockingLayoutPersistenceTest {

	/**
	 * This module registers {@code DefaultDockingLayoutPersistenceProvider} for
	 * {@code ServiceLoader} both ways: a {@code provides} directive in
	 * {@code module-info.java} for the module path, and a
	 * {@code META-INF/services} file for the class path. The test runs on the
	 * class path, so {@code provider()} resolves the default rather than
	 * throwing, which is the behavior a class-path application depends on.
	 */
	@Test
	void providerResolvesTheDefaultImplementation() {
		assertThat(DockingLayoutPersistence.provider())
				.describedAs("provider() resolved from the class path")
				.isInstanceOf(DefaultDockingLayoutPersistenceProvider.class);
	}

	/**
	 * A utility class with only static members has no reason to be
	 * instantiated; the private constructor exists to say so rather than to
	 * silently allow it.
	 */
	@Test
	void utilityClassConstructorThrowsIllegalStateException() throws Exception {
		final Constructor<DockingLayoutPersistence> constructor =
				DockingLayoutPersistence.class.getDeclaredConstructor();
		constructor.setAccessible(true);

		assertThatThrownBy(constructor::newInstance)
				.describedAs("reflective instantiation of the utility class")
				.isInstanceOf(InvocationTargetException.class)
				.cause()
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Utility class");
	}
}
