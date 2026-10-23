package software.coley.bentofx.persistence.core.api.provider;

import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.LayoutRestorer;
import software.coley.bentofx.persistence.core.api.LayoutSaver;

import java.util.List;

/**
 * Creates the {@link LayoutSaver} and {@link LayoutRestorer} an application uses to
 * persist and restore its docking layout. Also provides information about any exising
 * layouts that are already stored.
 *
 * <p>Obtain an instance from
 * {@link software.coley.bentofx.persistence.core.api.DockingLayoutPersistence#provider()}
 * rather than constructing one; implementations live in packages this module does
 * not export.</p>
 *
 * @author Phil Bryant
 */
public interface DockingLayoutPersistenceProvider {

	/**
	 * {@return a {@link LayoutSaver} with the specified identifier, already
	 * saving automatically.}
	 *
	 * @param layoutIdentifier the identifier to use to distinguish the
	 * {@link LayoutSaver} from other {@link LayoutSaver}s.
	 * @param bentoProvider used to acquire {@code Bento}.
	 *
	 * @throws BentoStateException when the {@link LayoutSaver} cannot be
	 * returned.
	 */
	default LayoutSaver getLayoutSaver(
			final String layoutIdentifier,
			final BentoProvider bentoProvider
	) throws BentoStateException {
		return getLayoutSaver(
				LayoutPersistenceProfile.of(layoutIdentifier),
				bentoProvider
		);
	}

	/**
	 * {@return a {@link LayoutSaver} for the specified profile, already saving
	 * automatically.}
	 *
	 * <p>Starting auto-save is the implementation's responsibility: a saver is
	 * not permitted to arm itself from its own constructor, so a saver obtained
	 * any other way is not auto-saving until the caller asks it to.</p>
	 *
	 * @param layoutPersistenceProfile identifies the {@link LayoutSaver} to
	 * return.
	 * @param bentoProvider used to acquire {@code Bento}.
	 *
	 * @throws BentoStateException when the {@link LayoutSaver} cannot be
	 * returned.
	 */
	LayoutSaver getLayoutSaver(
			final LayoutPersistenceProfile layoutPersistenceProfile,
			final BentoProvider bentoProvider
	) throws BentoStateException;

	/**
	 * {@return a {@code LayoutRestorer} with the specified identifier.}
	 *
	 * @param layoutIdentifier the identifier to use to distinguish the
	 * {@code LayoutRestorer} from other {@code LayoutRestorer}s.
	 * @param bentoProvider used to acquire {@code Bento}.
	 * @param dockableStateProvider used to acquire {@code DockableState}
	 * @param stageIconImageProvider used to acquire {@code Stage} icon
	 * {@code Image}s, {@code null} when a restored {@code Stage} should not
	 * have its icon {@code Image}s set.
	 * @param dockContainerLeafMenuFactoryProvider used to acquire
	 * {@code DockContainerLeafMenuFactory}, {@code null} when the
	 * {@code DockContainerLeafMenu} should not be set.
	 *
	 * @throws BentoStateException when the {@code LayoutRestorer} cannot be
	 * returned.
	 */
	default LayoutRestorer getLayoutRestorer(
			final String layoutIdentifier,
			final BentoProvider bentoProvider,
			final DockableStateProvider dockableStateProvider,
			final @Nullable StageIconImageProvider stageIconImageProvider,
			final @Nullable DockContainerLeafMenuFactoryProvider dockContainerLeafMenuFactoryProvider
	) throws BentoStateException {
		return getLayoutRestorer(
				LayoutPersistenceProfile.of(layoutIdentifier),
				bentoProvider,
				dockableStateProvider,
				stageIconImageProvider,
				dockContainerLeafMenuFactoryProvider
		);
	}

	/**
	 * {@return a {@code LayoutRestorer} with the specified identifier.}
	 *
	 * @param layoutPersistenceProfile identifies the {@link LayoutRestorer} to return.
	 * @param bentoProvider used to acquire {@code Bento}.
	 * @param dockableStateProvider used to acquire {@code DockableState}
	 * @param stageIconImageProvider used to acquire {@code Stage} icon
	 * {@code Image}s, {@code null} when a restored {@code Stage} should not
	 * have its icon {@code Image}s set.
	 * @param dockContainerLeafMenuFactoryProvider used to acquire
	 * {@code DockContainerLeafMenuFactory}, {@code null} when the
	 * {@code DockContainerLeafMenu} should not be set.
	 *
	 * @throws BentoStateException when the {@code LayoutRestorer} cannot be
	 * returned.
	 */
	LayoutRestorer getLayoutRestorer(
			final LayoutPersistenceProfile layoutPersistenceProfile,
			final BentoProvider bentoProvider,
			final DockableStateProvider dockableStateProvider,
			final @Nullable StageIconImageProvider stageIconImageProvider,
			final @Nullable DockContainerLeafMenuFactoryProvider dockContainerLeafMenuFactoryProvider
	) throws BentoStateException;

	/**
	 * Saves the current docking layout once, under the specified identifier.
	 *
	 * @param layoutIdentifier identifies the layout to write.
	 * @param bentoProvider used to acquire {@code Bento}.
	 *
	 * @throws BentoStateException when the layout cannot be saved.
	 */
	default void saveLayout(
			final String layoutIdentifier,
			final BentoProvider bentoProvider
	) throws BentoStateException {
		saveLayout(
				LayoutPersistenceProfile.of(layoutIdentifier),
				bentoProvider
		);
	}

	/**
	 * Saves the current docking layout once, for the specified profile.
	 *
	 * <p>This provides "save as" functionality; it is not a {@link LayoutSaver}.
	 * Nothing is armed, no listeners are registered on any {@code Bento}, and the
	 * storage is released before the call returns. Use this for a layout a user
	 * asked to keep under a name. Use {@link #getLayoutSaver} for the layout that
	 * follows the session.</p>
	 *
	 * <p>This reads the root branches that have a {@code Scene}, so it must be
	 * called while the layout it should capture is showing.</p>
	 *
	 * @param layoutPersistenceProfile identifies the layout to write, and
	 * optionally the codec and storage to write it with.
	 * @param bentoProvider used to acquire {@code Bento}.
	 *
	 * @throws BentoStateException when the layout cannot be saved.
	 * @see #getLayoutSaver for the layout that follows the session.
	 */
	void saveLayout(
			final LayoutPersistenceProfile layoutPersistenceProfile,
			final BentoProvider bentoProvider
	) throws BentoStateException;

	/**
	 * {@return the identifiers of the layouts already stored, in no particular
	 * order.}
	 *
	 * <p>The profile selects the codec and the storage. This reports every layout
	 * the destination holds, which includes the one the application saves
	 * automatically, so a list of layouts a user may restore should filter
	 * that one out.</p>
	 *
	 * <p>An empty list means the destination holds no layouts, or that the storage
	 * implementation cannot enumerate them.</p>
	 *
	 * @param layoutPersistenceProfile selects the codec and storage to ask.
	 *
	 * @throws BentoStateException when the codec or storage cannot be selected.
	 */
	List<String> getStoredLayoutIdentifiers(
			final LayoutPersistenceProfile layoutPersistenceProfile
	) throws BentoStateException;

	/**
	 * {@return a profile for each layout already stored, carrying its
	 * identifier and its display name, in no particular order.}
	 *
	 * <p>Unlike {@link #getStoredLayoutIdentifiers}, this reads each layout's
	 * display name, which means opening and decoding every stored layout - the
	 * display name lives inside the layout, not in its identifier. Use it to
	 * populate a menu that shows users the names they chose; use the identifier
	 * listing when the names are not needed.</p>
	 *
	 * <p>Each returned profile carries the codec and storage identifiers of the
	 * profile passed in, so it can be handed straight back to
	 * {@link #getLayoutRestorer} or {@link #deleteLayout}. The session layout is
	 * included, as it is in the identifier listing.</p>
	 *
	 * <p>A stored layout that cannot be read is still listed, with no display
	 * name and no group, so that one damaged layout neither hides the others
	 * nor becomes impossible to delete.</p>
	 *
	 * @param layoutPersistenceProfile selects the codec and storage to ask.
	 *
	 * @throws BentoStateException when the codec or storage cannot be selected.
	 */
	List<LayoutPersistenceProfile> getStoredLayouts(
			final LayoutPersistenceProfile layoutPersistenceProfile
	) throws BentoStateException;

	/**
	 * {@return {@code true} when the profile's layout is already stored;
	 * otherwise, {@code false}.}
	 *
	 * @param layoutPersistenceProfile identifies the layout, and optionally the
	 * codec and storage holding it.
	 *
	 * @throws BentoStateException when the codec or storage cannot be selected.
	 */
	boolean isLayoutStored(
			final LayoutPersistenceProfile layoutPersistenceProfile
	) throws BentoStateException;

	/**
	 * Removes the profile's stored layout, if there is one.
	 *
	 * @param layoutPersistenceProfile identifies the layout to remove, and
	 * optionally the codec and storage holding it.
	 *
	 * @return {@code true} when a layout was removed; {@code false} when there was
	 * nothing to remove or the storage implementation does not support removal.
	 *
	 * @throws BentoStateException when the codec or storage cannot be selected.
	 */
	boolean deleteLayout(
			final LayoutPersistenceProfile layoutPersistenceProfile
	) throws BentoStateException;

	/**
	 * Stores a different display name and group for a layout already in storage,
	 * leaving the docking state it holds exactly as it is.
	 *
	 * @param layoutPersistenceProfile identifies the layout, and carries the
	 * display name and group to store.
	 *
	 * @return {@code true} when a layout was rewritten; {@code false} when there
	 * was nothing stored under that identifier, in which case nothing is
	 * written.
	 *
	 * @throws BentoStateException when the codec or storage cannot be selected,
	 * or the layout cannot be read or written.
	 */
	boolean updateStoredLayoutNaming(
			final LayoutPersistenceProfile layoutPersistenceProfile
	) throws BentoStateException;
}
