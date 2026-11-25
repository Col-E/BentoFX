package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.core.api.provider.PersistedDockingLayoutOrganizationProvider;
import software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers.SESSION_LAYOUT_IDENTIFIER;

/**
 * Shared queries over the stored docking layouts: which are user-named, what
 * to call one in a menu, which to offer for action, and which groups to show
 * for them.
 *
 * <p>Package-private utility class with only static members. Both the menu
 * view and the layout operations read the same stored layouts the same way,
 * so this is the one place that reads them.</p>
 *
 * @author Phil Bryant
 */
final class StoredLayouts {

	private static final Logger logger =
			LoggerFactory.getLogger(StoredLayouts.class);

	private StoredLayouts() {
	}

	/**
	 * {@return the layouts a user may act on, or an empty {@link Optional}
	 * when storage could not be read.}
	 *
	 * <p>The profile this asks with selects only the codec and the storage;
	 * its layout identifier is not used, so the session profile serves rather
	 * than a second one being invented. The session layout is dropped from the
	 * result, because it is the layout the application saves to on its own and
	 * not one a user restores or deletes by name.</p>
	 *
	 * @param provider the provider to read from.
	 */
	static Optional<List<LayoutPersistenceProfile>> findStoredCustomLayouts(
			final DockingLayoutPersistenceProvider provider
	) {
		try {
			return Optional.of(
					provider.getStoredLayouts(
							LayoutPersistenceProfile.of(SESSION_LAYOUT_IDENTIFIER)
					).stream()
							.filter(StoredLayouts::isUserLayout)
							.sorted(Comparator.comparing(
									StoredLayouts::getLayoutLabel,
									String.CASE_INSENSITIVE_ORDER
							))
							.toList()
			);
		} catch (final BentoStateException e) {
			logger.warn("Could not list the stored docking layouts.", e);
			return Optional.empty();
		}
	}

	/**
	 * {@return the groups to show, or an empty {@link Optional} when storage
	 * could not be read.}
	 *
	 * <p>The stored catalog together with the groups the layouts themselves
	 * name — see {@link LayoutGroups#mergeGroupNames}. Callers that could not
	 * list the layouts must not call this at all, because a catalog on its own
	 * would report groups for a list of layouts the caller does not have. A
	 * provider with no {@link PersistedDockingLayoutOrganizationProvider}
	 * support has no catalog to read, so this reports only the groups the
	 * layouts themselves name.</p>
	 *
	 * @param provider the provider to read the catalog from, when it has one.
	 * @param storedLayouts the layouts storage reported.
	 */
	static Optional<List<String>> findGroupNames(
			final DockingLayoutPersistenceProvider provider,
			final List<LayoutPersistenceProfile> storedLayouts
	) {
		if (!(provider instanceof PersistedDockingLayoutOrganizationProvider extendedProvider)) {
			return Optional.of(
					LayoutGroups.mergeGroupNames(List.of(), storedLayouts)
			);
		}

		try {
			return Optional.of(LayoutGroups.mergeGroupNames(
					extendedProvider.getStoredGroups(
							LayoutPersistenceProfile.of(SESSION_LAYOUT_IDENTIFIER)
					),
					storedLayouts
			));
		} catch (final BentoStateException e) {
			logger.warn("Could not list the stored layout groups.", e);
			return Optional.empty();
		}
	}

	/**
	 * {@return {@code true} when the layout is one a user named; otherwise,
	 * {@code false}.}
	 *
	 * @param layoutPersistenceProfile identifies the layout to test.
	 */
	static boolean isUserLayout(
			final LayoutPersistenceProfile layoutPersistenceProfile
	) {
		return !LayoutIdentifiers.isReserved(
				layoutPersistenceProfile.layoutIdentifier()
		);
	}

	/**
	 * {@return what to call the layout in a menu.}
	 *
	 * <p>A layout stored before display names were kept has none, and shows
	 * the identifier it is addressed by rather than a name invented for it.</p>
	 *
	 * @param layoutPersistenceProfile identifies the layout to label.
	 */
	static String getLayoutLabel(
			final LayoutPersistenceProfile layoutPersistenceProfile
	) {
		return layoutPersistenceProfile.findDisplayName()
				.orElseGet(layoutPersistenceProfile::layoutIdentifier);
	}
}
