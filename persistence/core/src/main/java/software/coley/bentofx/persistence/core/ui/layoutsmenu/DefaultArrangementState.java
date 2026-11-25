package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import javafx.application.Platform;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutRestorable;
import software.coley.bentofx.persistence.core.api.provider.PersistedDockingLayoutOrganizationProvider;
import software.coley.bentofx.persistence.core.api.state.BentoState;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers.DEFAULT_LAYOUT_IDENTIFIER;
import static software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers.SESSION_LAYOUT_IDENTIFIER;

/**
 * Tracks whether the default layout is currently showing, comparing what is
 * on screen against the arrangement recorded the last time it was applied,
 * so the check mark on the Default item stays accurate.
 *
 * <p>The last-recorded arrangement of the default layout is compared against
 * what is on screen each time the menu fills to decide whether to mark the
 * {@code Default} item.</p>
 *
 * <p>{@link #recordDefaultArrangement()} must be called on the FX Application
 * Thread, because it reads the containers currently on screen.
 * {@link #recordDefaultArrangementLater()} defers that call through
 * {@link Platform#runLater}; callers not already on the FX thread should use
 * that overload. {@link #isDefaultLayoutShowing()} does not touch the scene
 * graph and has no thread requirement.</p>
 *
 * @author Phil Bryant
 */
class DefaultArrangementState {

	private static final Logger logger =
			LoggerFactory.getLogger(DefaultArrangementState.class);

	private final DockingLayoutRestorable dockingLayoutRestorable;

	/**
	 * How the default layout was arranged when it was last applied, read from
	 * storage, or {@code null} when none is recorded or it could not be read.
	 *
	 * <p>Read once and kept, because it changes only when this state records it
	 * again, which clears {@link #isDefaultArrangementRead}.</p>
	 */
	private @Nullable List<BentoState> defaultArrangement;

	/** Whether {@link #defaultArrangement} has been read since it last changed. */
	private boolean isDefaultArrangementRead;

	/**
	 * Reads initial state and, when no session layout is stored, schedules a
	 * recording of the arrangement showing now as the default layout's. The
	 * recording is deferred to the FX event loop and must be constructed from
	 * the FX Application Thread so the deferred task reaches the scene graph.
	 *
	 * @param dockingLayoutRestorable the application whose persistence providers
	 * the default arrangement is read and written through.
	 */
	DefaultArrangementState(final DockingLayoutRestorable dockingLayoutRestorable) {
		this.dockingLayoutRestorable =
				Objects.requireNonNull(dockingLayoutRestorable, "dockingLayoutRestorable");

		// With no session layout saved, a restore fell back to the default
		// layout, so that is what the application is showing.
		if (!isSessionLayoutStored()) {
			recordDefaultArrangementLater();
		}
	}

	/**
	 * {@return {@code true} when what is showing is arranged as the default
	 * layout was when it was last applied; otherwise, {@code false}.}
	 *
	 * <p>Asked each time the menu is filled, so that rearranging the default
	 * layout clears its check mark and arranging it back restores it. Compares
	 * against the arrangement recorded by {@link #recordDefaultArrangement()}
	 * rather than building the default layout, so asking costs a walk of the
	 * containers on screen and never touches their content. See
	 * {@link LayoutArrangements} for what counts as the arrangement.</p>
	 *
	 * <p>When nothing is recorded, or the persistence provider cannot read it
	 * back, there is no way to tell, and this answers {@code true} - checking
	 * {@code Default} whenever no named layout is active, as this menu did
	 * before it could tell.</p>
	 */
	boolean isDefaultLayoutShowing() {
		final Optional<PersistedDockingLayoutOrganizationProvider> extendedProvider =
				extendedPersistenceProvider();

		if (extendedProvider.isEmpty()) {
			return true;
		}

		if (!isDefaultArrangementRead) {
			try {
				defaultArrangement = extendedProvider.get()
						.getStoredBentoStates(
								LayoutPersistenceProfile.of(DEFAULT_LAYOUT_IDENTIFIER)
						)
						.orElse(null);
				isDefaultArrangementRead = true;
			} catch (final BentoStateException e) {
				logger.warn("Could not read how the default layout was arranged.", e);
				return true;
			}
		}

		final List<BentoState> arrangement = defaultArrangement;

		return arrangement == null || LayoutArrangements.isShowing(
				arrangement,
				dockingLayoutRestorable.getBentoProvider()
		);
	}

	/**
	 * Records the arrangement showing now as the default layout's, once the
	 * application has finished applying it.
	 *
	 * <p>Deferred to the next pass of the JavaFX event loop because an
	 * application may still be attaching the layout when this is asked for -
	 * at startup this menu is built before the layout it sits beside is on
	 * screen. The user cannot rearrange anything before then, because input
	 * queued behind this task is handled after it.</p>
	 *
	 * <p>Does nothing when the persistence provider does not implement
	 * {@link PersistedDockingLayoutOrganizationProvider}, because without it the
	 * arrangement cannot be read back.</p>
	 */
	void recordDefaultArrangementLater() {
		// Only a provider that can read the arrangement back has a use for it.
		if (extendedPersistenceProvider().isPresent()) {
			Platform.runLater(this::recordDefaultArrangement);
		}
	}

	/**
	 * Records the arrangement showing now as the default layout's, under
	 * {@link software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers#DEFAULT_LAYOUT_IDENTIFIER},
	 * so it survives a restart. Must be called on the JavaFX Application Thread.
	 */
	private void recordDefaultArrangement() {
		try {
			persistenceProvider().saveLayout(
					LayoutPersistenceProfile.of(DEFAULT_LAYOUT_IDENTIFIER),
					dockingLayoutRestorable.getBentoProvider()
			);
			isDefaultArrangementRead = false;
		} catch (final BentoStateException e) {
			logger.warn("Could not record how the default layout is arranged.", e);
		}
	}

	/**
	 * {@return {@code true} when a session layout is stored, or when that
	 * cannot be read; otherwise, {@code false}.}
	 *
	 * <p>An unreadable answer counts as stored, so that this menu does not
	 * record whatever is showing as the default layout on a guess.</p>
	 */
	private boolean isSessionLayoutStored() {
		try {
			return persistenceProvider().isLayoutStored(
					LayoutPersistenceProfile.of(SESSION_LAYOUT_IDENTIFIER)
			);
		} catch (final BentoStateException e) {
			logger.warn("Could not tell whether a session layout is stored.", e);
			return true;
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
