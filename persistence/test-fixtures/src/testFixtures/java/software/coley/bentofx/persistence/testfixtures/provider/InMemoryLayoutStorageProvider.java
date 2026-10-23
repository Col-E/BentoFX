package software.coley.bentofx.persistence.testfixtures.provider;

import software.coley.bentofx.persistence.core.api.provider.LayoutStorageProvider;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;
import software.coley.bentofx.persistence.testfixtures.storage.InMemoryLayoutStorage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link LayoutStorageProvider} that keeps what is written to it in memory,
 * keyed by layout identifier, so a save and a later restore round-trip the same
 * bytes.
 *
 * <p>Unlike {@code ConfigurableLayoutStorageProvider}, which hands out a fresh empty
 * {@code DoNothingLayoutStorage} per call, this returns the <em>same</em>
 * {@link InMemoryLayoutStorage} for a given layout identifier on every call. That
 * is what makes a write visible to a later read - the property a round-trip test
 * needs, and the one a real file or database row provides by persisting. Reach for
 * this fixture when a test has to read back what it wrote; reach for
 * {@code ConfigurableLayoutStorageProvider} when it only needs to watch which identifiers
 * a component asks for.</p>
 *
 * <p>The codec identifier is not part of the key: a test that needs to separate
 * layouts by codec uses distinct layout identifiers. The provider reports
 * {@code "in-memory"} and {@code true} from {@link #isDefault()}, so a
 * {@code DefaultDockingLayoutPersistenceProvider} built with only this storage
 * selects it without being named.</p>
 *
 * @author Phil Bryant
 */
public final class InMemoryLayoutStorageProvider
		extends AbstractConfigurableLayoutProvider
		implements LayoutStorageProvider {

	private final Map<String, InMemoryLayoutStorage> storagesByLayoutIdentifier =
			new LinkedHashMap<>();

	/**
	 * Creates a provider reporting {@code "in-memory"} as its identifier and as
	 * the default.
	 */
	public InMemoryLayoutStorageProvider() {
		super("in-memory", true);
	}

	@Override
	public LayoutStorage getLayoutStorage(
			final String layoutIdentifier,
			final String codecIdentifier
	) {
		return storagesByLayoutIdentifier.computeIfAbsent(
				layoutIdentifier,
				key -> new InMemoryLayoutStorage()
		);
	}

	@Override
	public List<String> getLayoutIdentifiers(final String codecIdentifier) {
		final List<String> identifiers = new ArrayList<>();
		storagesByLayoutIdentifier.forEach((identifier, storage) -> {
			if (storage.exists()) {
				identifiers.add(identifier);
			}
		});
		return identifiers;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Overridden to answer from the backing map rather than through the
	 * {@link #getLayoutStorage} default, so that asking about a layout that was
	 * never stored does not create an empty entry for it.</p>
	 */
	@Override
	public boolean isLayoutStored(
			final String layoutIdentifier,
			final String codecIdentifier
	) {
		final InMemoryLayoutStorage storage =
				storagesByLayoutIdentifier.get(layoutIdentifier);
		return storage != null && storage.exists();
	}

	@Override
	public boolean deleteLayout(
			final String layoutIdentifier,
			final String codecIdentifier
	) {
		final InMemoryLayoutStorage removed =
				storagesByLayoutIdentifier.remove(layoutIdentifier);
		return removed != null && removed.exists();
	}
}
