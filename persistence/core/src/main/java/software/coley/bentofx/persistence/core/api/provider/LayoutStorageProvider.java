package software.coley.bentofx.persistence.core.api.provider;

import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;

import java.util.List;

/**
 * Supplies the {@link LayoutStorage} that decides where a docking layout is
 * written to.
 *
 * <p>Discovered at runtime, so an application changes where layouts are kept by
 * changing which storage implementation it depends on - see
 * {@link LayoutPersistenceComponentProvider}. The provider is the discoverable
 * type; the {@link LayoutStorage} it returns does not need to be discoverable
 * itself.</p>
 *
 * <p>Beyond handing out storage for one layout, a provider answers what its
 * destination holds: {@link #getLayoutIdentifiers(String)} and
 * {@link #deleteLayout(String, String)} are what an application needs to offer
 * users a list of saved layouts and let them remove one, so both are required of
 * an implementation. {@link #isLayoutStored(String, String)} has a default that
 * opens the storage and asks it, which every implementation can rely on.
 * Applications reach all three through {@link DockingLayoutPersistenceProvider}
 * rather than selecting a storage provider themselves.</p>
 *
 * <p><b>Codec-partitioned namespace.</b> This provider enumerates, opens, and
 * deletes layouts keyed by a {@code (layoutIdentifier, codecIdentifier)} pair.
 * Each codec owns its own namespace within the storage - a layout written by
 * the JSON codec is not enumerated or openable through the XML codec, even
 * against the same storage root. This keeps every codec's byte format
 * self-contained and avoids examining content when opening, at the cost that
 * an application that switches codecs sees only the layouts stored under the
 * active codec. See {@link #getLayoutIdentifiers(String)} for the enumeration
 * consequences and {@link #getLayoutStorage(String, String)} for the open
 * contract.</p>
 *
 * @author Phil Bryant
 */
public interface LayoutStorageProvider extends LayoutPersistenceComponentProvider {

	/**
	 * Returns a {@link LayoutStorage} that can be used to persist a Bento
	 * layout.
	 * <p>
	 * Each call must return a fresh instance, even for arguments it has already
	 * been given. The caller takes ownership of what it is handed and closes it
	 * (see {@link LayoutStorage#close()}), so an implementation that caches and
	 * returns one instance per layout would have a closed saver take the
	 * restorer's storage down with it.
	 *
	 * @return a {@link LayoutStorage} that can be used to persist a Bento
	 * layout.
	 */
	LayoutStorage getLayoutStorage(
			final String layoutIdentifier,
			final String codecIdentifier
	);

	/**
	 * {@return the identifiers of the layouts this destination holds for the
	 * supplied codec, in no particular order.}
	 *
	 * <p>A layout appears only when there is something to read: an entry with no
	 * content is not a layout, which is the same rule {@link LayoutStorage#exists()}
	 * applies. A destination holding no layouts returns an empty list.</p>
	 *
	 * <p>An unreachable destination throws an unchecked exception rather than
	 * reporting no layouts, for the reason {@link LayoutStorage#exists()} gives:
	 * answering "nothing here" when the truth is "cannot tell" invites the caller
	 * to write over a layout that exists.</p>
	 *
	 * <p>Enumeration is codec-scoped: only layouts that were written by the named
	 * codec are returned. Layouts stored under a different codec identifier
	 * remain in the storage (per the storage's own retention rules) but are
	 * invisible to this call. Applications that let the active codec change over
	 * time should either migrate stored layouts to the new codec at the switch,
	 * or notify application users that previously-saved layouts are temporarily
	 * inaccessible - this provider deliberately does neither. See
	 * {@link LayoutStorageProvider} for the design intent.</p>
	 *
	 * @param codecIdentifier identifies the codec whose layouts are wanted.
	 */
	List<String> getLayoutIdentifiers(final String codecIdentifier);

	/**
	 * {@return {@code true} when this destination holds a layout for the supplied
	 * identifiers; otherwise, {@code false}.}
	 *
	 * <p>The default asks the storage itself, which every implementation can
	 * answer. Override when the destination can answer more cheaply than opening
	 * one.</p>
	 *
	 * @param layoutIdentifier identifies the layout.
	 * @param codecIdentifier identifies the codec whose output is stored.
	 */
	default boolean isLayoutStored(
			final String layoutIdentifier,
			final String codecIdentifier
	) {
		try (final LayoutStorage layoutStorage =
					 getLayoutStorage(layoutIdentifier, codecIdentifier)) {
			return layoutStorage.exists();
		}
	}

	/**
	 * Removes the stored layout, if there is one.
	 * <p>
	 * Location-level operation: does not require a prior
	 * {@link #getLayoutStorage(String, String)} call. Delete lives on this
	 * provider rather than on
	 * {@link software.coley.bentofx.persistence.core.api.storage.LayoutStorage}
	 * because a {@code LayoutStorage} is a single-use I/O session against the
	 * stored layout, not a persistent reference to its location. The split
	 * mirrors the JDK's {@link java.nio.file.Files#delete(java.nio.file.Path)}
	 * versus {@link java.nio.file.Files#newInputStream(java.nio.file.Path,
	 * java.nio.file.OpenOption...)}: both operations are keyed by the same
	 * identifier, but neither requires the other to have been called first.
	 *
	 * @param layoutIdentifier identifies the layout to remove.
	 * @param codecIdentifier identifies the codec whose output is stored.
	 * @return {@code true} when a layout was removed; {@code false} when there was
	 * nothing to remove.
	 */
	boolean deleteLayout(
			final String layoutIdentifier,
			final String codecIdentifier
	);
}
