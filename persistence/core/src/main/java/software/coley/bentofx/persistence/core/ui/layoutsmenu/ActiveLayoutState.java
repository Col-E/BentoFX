package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutRestorable;
import software.coley.bentofx.persistence.core.api.provider.PersistedDockingLayoutOrganizationProvider;

import java.util.Objects;
import java.util.Optional;

import static software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers.SESSION_LAYOUT_IDENTIFIER;

/**
 * Tracks which named layout is currently shown as the user's active choice,
 * and reads and writes that choice through the extended persistence provider
 * so it survives a restart.
 *
 * <p>None of the methods here touch the scene graph, so none require the
 * FX Application Thread.</p>
 *
 * @author Phil Bryant
 */
class ActiveLayoutState {

	private static final Logger logger =
			LoggerFactory.getLogger(ActiveLayoutState.class);

	private final DockingLayoutRestorable dockingLayoutRestorable;

	/**
	 * The custom layout showing now, or {@code null} when no named layout is
	 * active.
	 *
	 * <p>Saving changes and renaming both write the live containers, so both
	 * are offered only for the layout on screen. This is what they write to,
	 * and what the check mark in the menu marks.</p>
	 */
	private @Nullable LayoutPersistenceProfile activeCustomLayoutProfile;

	/**
	 * @param dockingLayoutRestorable the application whose persistence providers
	 * the active-layout identifier is read and written through.
	 */
	ActiveLayoutState(final DockingLayoutRestorable dockingLayoutRestorable) {
		this.dockingLayoutRestorable =
				Objects.requireNonNull(dockingLayoutRestorable, "dockingLayoutRestorable");
		this.activeCustomLayoutProfile = findActiveLayoutProfile();
	}

	/**
	 * {@return the named layout currently active, or {@code null} when the
	 * default layout is showing or no named layout has been activated yet.}
	 */
	@Nullable LayoutPersistenceProfile getActiveLayoutProfile() {
		return activeCustomLayoutProfile;
	}

	/**
	 * {@return {@code true} when the supplied layout is the one showing now;
	 * otherwise, {@code false}.}
	 *
	 * @param layoutPersistenceProfile identifies the layout to test.
	 */
	boolean isActiveLayout(final LayoutPersistenceProfile layoutPersistenceProfile) {
		final LayoutPersistenceProfile activeLayout = activeCustomLayoutProfile;

		return activeLayout != null && activeLayout.layoutIdentifier()
				.equals(layoutPersistenceProfile.layoutIdentifier());
	}

	/**
	 * Sets the active layout and records it through
	 * {@link PersistedDockingLayoutOrganizationProvider}, when the persistence
	 * provider supports it.
	 *
	 * <p>The only way {@link #activeCustomLayoutProfile} changes — every
	 * other assignment goes through here, so a restart never misses a
	 * change.</p>
	 *
	 * @param activeCustomLayoutProfile the layout to show as active, or
	 * {@code null} for none.
	 */
	void setActiveCustomLayoutProfile(
			final @Nullable LayoutPersistenceProfile activeCustomLayoutProfile
	) {
		this.activeCustomLayoutProfile = activeCustomLayoutProfile;
		rememberActiveLayout(activeCustomLayoutProfile);
	}

	/**
	 * {@return the named layout to show as active from the moment the menu
	 * opens, read through {@link PersistedDockingLayoutOrganizationProvider}, or
	 * {@code null} when none is recorded, it no longer exists, or the
	 * persistence provider does not support this.}
	 *
	 * <p>An application that restores the session layout at startup — which
	 * mirrors whatever was last on screen, including the content of a named
	 * layout that was switched to and then left there — has no other way to
	 * know that happened. This is what lets the menu show that layout selected
	 * again after a restart, rather than {@code Default}.</p>
	 */
	private @Nullable LayoutPersistenceProfile findActiveLayoutProfile() {
		final Optional<PersistedDockingLayoutOrganizationProvider> extendedProvider =
				extendedPersistenceProvider();

		if (extendedProvider.isEmpty()) {
			return null;
		}

		try {
			final Optional<String> activeLayoutIdentifier =
					extendedProvider.get().getActiveLayoutIdentifier(
							LayoutPersistenceProfile.of(SESSION_LAYOUT_IDENTIFIER)
					);

			if (activeLayoutIdentifier.isEmpty()) {
				return null;
			}

			// Taken from the catalog rather than built from the identifier, so
			// that it carries the stored display name and group: saving changes
			// writes this profile's naming, and one without it would take the
			// layout out of its group and drop its name. Recorded once, but not
			// necessarily still true: the layout may have been deleted since.
			return persistenceProvider()
					.getStoredLayouts(LayoutPersistenceProfile.of(SESSION_LAYOUT_IDENTIFIER))
					.stream()
					.filter(storedLayout -> storedLayout.layoutIdentifier()
							.equals(activeLayoutIdentifier.get()))
					.findFirst()
					.orElse(null);
		} catch (final BentoStateException e) {
			logger.warn("Could not read which named layout was active.", e);
			return null;
		}
	}

	/**
	 * Records which named layout is active through
	 * {@link PersistedDockingLayoutOrganizationProvider}, so
	 * {@link #findActiveLayoutProfile()} can read it back on the next launch.
	 * Does nothing when the persistence provider does not support this.
	 *
	 * @param activeLayoutProfile the layout now active, or {@code null} when
	 * the default layout is showing or the active layout was deleted.
	 */
	private void rememberActiveLayout(final @Nullable LayoutPersistenceProfile activeLayoutProfile) {
		extendedPersistenceProvider().ifPresent(extendedProvider -> {
			try {
				extendedProvider.setActiveLayoutIdentifier(
						LayoutPersistenceProfile.of(SESSION_LAYOUT_IDENTIFIER),
						activeLayoutProfile == null
								? null
								: activeLayoutProfile.layoutIdentifier()
				);
			} catch (final BentoStateException e) {
				logger.warn("Could not record which named layout is active.", e);
			}
		});
	}

	/**
	 * Keeps {@link #activeCustomLayoutProfile} in step with a naming change
	 * just written to storage.
	 *
	 * <p>Saving changes writes the active profile's display name and group, so
	 * a profile left holding the old naming would undo the change on the next
	 * save — and, for a deleted group, bring the group back.</p>
	 *
	 * @param renamedLayout the layout as it is now named in storage.
	 */
	void followNamingChange(final LayoutPersistenceProfile renamedLayout) {
		if (isActiveLayout(renamedLayout)) {
			setActiveCustomLayoutProfile(renamedLayout);
		}
	}

	/**
	 * {@return the provider layouts are read, written, and listed through.}
	 *
	 * <p>Asked of the application at each use rather than held here, so that
	 * this state cannot be the reason a stale provider stays reachable.</p>
	 */
	private DockingLayoutPersistenceProvider persistenceProvider() {
		return dockingLayoutRestorable.getPersistenceProvider();
	}

	/**
	 * {@return {@link #persistenceProvider()}, narrowed to
	 * {@link PersistedDockingLayoutOrganizationProvider}, or an empty
	 * {@link Optional} when it does not implement that too.}
	 */
	private Optional<PersistedDockingLayoutOrganizationProvider> extendedPersistenceProvider() {
		return persistenceProvider() instanceof PersistedDockingLayoutOrganizationProvider extendedProvider
				? Optional.of(extendedProvider)
				: Optional.empty();
	}
}
