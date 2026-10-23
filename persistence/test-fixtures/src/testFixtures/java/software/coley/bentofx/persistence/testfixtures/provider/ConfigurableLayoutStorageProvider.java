package software.coley.bentofx.persistence.testfixtures.provider;

import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.core.api.provider.LayoutStorageProvider;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;
import software.coley.bentofx.persistence.testfixtures.storage.DoNothingLayoutStorage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Configurable {@link LayoutStorageProvider} for provider-selection tests.
 *
 * <p>The recorded identifiers are held atomically because the persistence API
 * decides which thread calls a provider, so the test asserting on them is not
 * the thread that recorded them.</p>
 *
 * @author Phil Bryant
 */
public final class ConfigurableLayoutStorageProvider
		extends AbstractConfigurableLayoutProvider
		implements LayoutStorageProvider {

	private final AtomicReference<@Nullable String> layoutIdentifier =
			new AtomicReference<>();
	private final AtomicReference<@Nullable String> codecIdentifier =
			new AtomicReference<>();

	private final AtomicReference<@Nullable String> catalogCodecIdentifier =
			new AtomicReference<>();

	private final List<String> storedLayoutIdentifiers =
			new CopyOnWriteArrayList<>();

	/**
	 * Creates a provider that reports the supplied identifier and default flag.
	 *
	 * @param identifier the identifier this provider answers to.
	 * @param defaultProvider whether this provider is the default one.
	 */
	public ConfigurableLayoutStorageProvider(
			final String identifier,
			final boolean defaultProvider
	) {
		super(identifier, defaultProvider);
	}

	@Override
	public LayoutStorage getLayoutStorage(
			final String layoutIdentifier,
			final String codecIdentifier
	) {
		this.layoutIdentifier.set(layoutIdentifier);
		this.codecIdentifier.set(codecIdentifier);
		return new DoNothingLayoutStorage();
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Reports whatever {@link #setStoredLayoutIdentifiers} was given, and records
	 * the codec identifier it was asked with.</p>
	 */
	@Override
	public List<String> getLayoutIdentifiers(final String codecIdentifier) {
		catalogCodecIdentifier.set(codecIdentifier);
		return List.copyOf(storedLayoutIdentifiers);
	}

	@Override
	public boolean isLayoutStored(
			final String layoutIdentifier,
			final String codecIdentifier
	) {
		catalogCodecIdentifier.set(codecIdentifier);
		return storedLayoutIdentifiers.contains(layoutIdentifier);
	}

	@Override
	public boolean deleteLayout(
			final String layoutIdentifier,
			final String codecIdentifier
	) {
		catalogCodecIdentifier.set(codecIdentifier);
		return storedLayoutIdentifiers.remove(layoutIdentifier);
	}

	/**
	 * Sets the layouts this provider reports as stored.
	 *
	 * @param layoutIdentifiers the layouts to report.
	 */
	public void setStoredLayoutIdentifiers(final List<String> layoutIdentifiers) {
		storedLayoutIdentifiers.clear();
		storedLayoutIdentifiers.addAll(layoutIdentifiers);
	}

	/**
	 * {@return the codec identifier the last catalog call was made with, or
	 * {@code null} when none has been made.}
	 */
	public @Nullable String getCatalogCodecIdentifier() {
		return catalogCodecIdentifier.get();
	}

	/**
	 * {@return the layout identifier this provider was last asked for, or
	 * {@code null} when it has not been asked.}
	 */
	public @Nullable String getLayoutIdentifier() {
		return layoutIdentifier.get();
	}

	/**
	 * {@return the codec identifier this provider was last asked for, or
	 * {@code null} when it has not been asked.}
	 */
	public @Nullable String getCodecIdentifier() {
		return codecIdentifier.get();
	}
}
