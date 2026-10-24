package software.coley.bentofx.persistence.core.api;

import software.coley.bentofx.persistence.core.api.provider.DockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorageLocations;

import java.util.ServiceLoader;

/**
 * Entry point to the persistence API: hands back the
 * {@link DockingLayoutPersistenceProvider} that creates {@link LayoutSaver} and
 * {@link LayoutRestorer} instances.
 *
 * <p>Start here. An application needs no other knowledge of how the
 * implementation is found or named:</p>
 *
 * {@snippet lang = java:
 * final DockingLayoutPersistenceProvider persistence =
 *         DockingLayoutPersistence.provider();
 *}
 *
 * <p>Where a locally-backed implementation keeps its data - and how to give
 * this application its own directory so another BentoFX-based application on
 * the same machine does not share it - is controlled by
 * {@link LayoutStorageLocations}, either from code or from an environment
 * variable that needs no code at all. Configure that, if at all, before
 * calling {@link #provider()}.</p>
 *
 * @author Phil Bryant
 */
public final class DockingLayoutPersistence {

	private DockingLayoutPersistence() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * {@return the {@link DockingLayoutPersistenceProvider} implementation found
	 * on the module path.}
	 *
	 * <p>Call this once during start-up and hold the result. The method
	 * re-resolves on every call rather than caching so that an application
	 * that reconfigures its module layer or persistence setup after start-up
	 * sees the new provider on the next call.</p>
	 *
	 * @throws IllegalStateException when no implementation can be found, which
	 * means no persistence implementation is on the module path or the class
	 * path.
	 */
	public static DockingLayoutPersistenceProvider provider() {

		// Resolved against this class's own loader rather than the thread context
		// loader. The two are the same in an ordinary launch, but a container or
		// a nested module layer can leave the context loader pointing somewhere
		// that cannot see this module's services, which fails as "no
		// implementation found" and sends the reader looking in the wrong place.
		return ServiceLoader.load(
						DockingLayoutPersistenceProvider.class,
						DockingLayoutPersistence.class.getClassLoader()
				)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(
						"No DockingLayoutPersistenceProvider implementation was " +
								"found. Add a persistence implementation to the " +
								"module path or the class path."
				));
	}
}
