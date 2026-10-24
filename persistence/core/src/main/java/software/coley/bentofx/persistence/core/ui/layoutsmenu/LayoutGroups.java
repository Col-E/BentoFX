package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static java.util.Objects.requireNonNull;

/**
 * Defines parameters for organizing saved layouts groups.
 *
 * @author Phil Bryant
 */
final class LayoutGroups {

	/**
	 * The most characters a group name may run to.
	 *
	 * <p>Nothing in storage requires this. It bounds what a submenu has to show
	 * and what a catalog has to hold, and it is generous enough that no
	 * reasonable name meets it.</p>
	 */
	static final int MAX_GROUP_NAME_LENGTH = 64;

	/** Parameter name reported when the stored layouts argument is null. */
	private static final String STORED_LAYOUTS = "storedLayouts";

	/**
	 * Why a name a user typed cannot be a group.
	 */
	enum GroupNameProblem {

		/** Nothing but space, so there would be nothing to label a submenu. */
		BLANK,

		/** Longer than {@link #MAX_GROUP_NAME_LENGTH}. */
		TOO_LONG,

		/** A group of that name already exists, ignoring case. */
		DUPLICATE
	}

	private LayoutGroups() {
		throw new UnsupportedOperationException(
				"Utility classes should not be instantiated."
		);
	}

	/**
	 * {@return every group to show, sorted and without repeats, from the stored
	 * catalog and from the layouts themselves.}
	 *
	 * @param catalogGroups the groups storage reported, which may be empty.
	 * @param storedLayouts the layouts storage reported.
	 */
	static List<String> mergeGroupNames(
			final List<String> catalogGroups,
			final Collection<LayoutPersistenceProfile> storedLayouts
	) {
		requireNonNull(catalogGroups, "catalogGroups");
		requireNonNull(storedLayouts, STORED_LAYOUTS);

		// A TreeSet with this comparator keeps the entry it already holds when an
		// equal one is added, so first spelling in wins, and iterates sorted.
		final Set<String> groupNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

		for (final String catalogGroup : catalogGroups) {
			addWhenNamed(groupNames, catalogGroup);
		}

		for (final LayoutPersistenceProfile storedLayout : storedLayouts) {
			addWhenNamed(groupNames, storedLayout.group());
		}

		return List.copyOf(groupNames);
	}

	/**
	 * Adds a group name, ignoring one that names nothing.
	 *
	 * <p>A blank or absent name would show as a submenu with no label. Nothing
	 * this menu writes produces one; a catalog edited by hand could.</p>
	 *
	 * @param groupNames the names collected so far.
	 * @param groupName the name to add, which may be {@code null} or blank.
	 */
	private static void addWhenNamed(
			final Set<String> groupNames,
			final @Nullable String groupName
	) {
		if (groupName != null && !groupName.isBlank()) {
			groupNames.add(groupName);
		}
	}

	/**
	 * {@return the layouts in each group, keyed by group name and in the order the
	 * groups were given.}
	 *
	 * @param groupNames the groups to key by, as {@link #mergeGroupNames}
	 * reported them.
	 * @param storedLayouts the layouts to distribute.
	 */
	static Map<String, List<LayoutPersistenceProfile>> groupLayouts(
			final List<String> groupNames,
			final Collection<LayoutPersistenceProfile> storedLayouts
	) {
		requireNonNull(groupNames, "groupNames");
		requireNonNull(storedLayouts, STORED_LAYOUTS);

		// Keyed without regard to case so a layout finds its group whatever the
		// two were spelled as, but iterated in the order the groups were given so
		// the menu shows them the way the caller sorted them.
		final Map<String, String> canonicalNames =
				new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		final Map<String, List<LayoutPersistenceProfile>> grouped =
				new LinkedHashMap<>();

		for (final String groupName : groupNames) {
			canonicalNames.put(groupName, groupName);
			grouped.put(groupName, new ArrayList<>());
		}

		for (final LayoutPersistenceProfile storedLayout : storedLayouts) {
			final String groupName = storedLayout.group();

			// A group every layout was matched against came from those layouts,
			// so a miss - including a layout with no group at all - means the
			// caller narrowed the group list. Skipping keeps the layout out of
			// the wrong group; the caller lists it loose.
			final String canonicalName =
					groupName == null ? null : canonicalNames.get(groupName);

			if (canonicalName == null) {
				continue;
			}

			final List<LayoutPersistenceProfile> members =
					grouped.get(canonicalName);

			// Non-null: every canonical name was put in both maps together.
			if (members != null) {
				members.add(storedLayout);
			}
		}

		return grouped;
	}

	/**
	 * {@return the layouts in no group at all, in the order they arrive.}
	 *
	 * @param groupNames the groups being shown.
	 * @param storedLayouts the layouts to filter.
	 */
	static List<LayoutPersistenceProfile> ungroupedLayouts(
			final List<String> groupNames,
			final Collection<LayoutPersistenceProfile> storedLayouts
	) {
		requireNonNull(groupNames, "groupNames");
		requireNonNull(storedLayouts, STORED_LAYOUTS);

		final Set<String> shownGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		shownGroups.addAll(groupNames);

		final List<LayoutPersistenceProfile> ungrouped = new ArrayList<>();

		for (final LayoutPersistenceProfile storedLayout : storedLayouts) {
			final String groupName = storedLayout.group();

			// A layout naming a group that is not being shown is listed loose
			// rather than dropped: an unreachable stored layout is worse than one
			// in the wrong place.
			if (groupName == null || !shownGroups.contains(groupName)) {
				ungrouped.add(storedLayout);
			}
		}

		return ungrouped;
	}

	/**
	 * {@return why the name cannot be a group, or an empty {@link Optional} when
	 * it can.}
	 *
	 * @param groupName the name the user typed, which is compared after
	 * trimming.
	 * @param existingGroups the groups that already exist.
	 */
	static Optional<GroupNameProblem> findGroupNameProblem(
			final String groupName,
			final Collection<String> existingGroups
	) {
		return findGroupNameProblem(groupName, existingGroups, null);
	}

	/**
	 * {@return why the name cannot be a group, or an empty {@link Optional} when
	 * it can.}
	 *
	 * @param groupName the name the user typed, which is compared after
	 * trimming.
	 * @param existingGroups the groups that already exist.
	 * @param renamedGroup the group being renamed, which does not count as a
	 * name already taken, or {@code null} when a group is being created. Without
	 * this, correcting the case of a group's own name would be refused as a
	 * duplicate of itself.
	 */
	static Optional<GroupNameProblem> findGroupNameProblem(
			final String groupName,
			final Collection<String> existingGroups,
			final @Nullable String renamedGroup
	) {
		requireNonNull(groupName, "groupName");
		requireNonNull(existingGroups, "existingGroups");

		final String trimmedName = groupName.trim();

		if (trimmedName.isEmpty()) {
			return Optional.of(GroupNameProblem.BLANK);
		}

		if (trimmedName.length() > MAX_GROUP_NAME_LENGTH) {
			return Optional.of(GroupNameProblem.TOO_LONG);
		}

		if (trimmedName.equalsIgnoreCase(renamedGroup)) {
			return Optional.empty();
		}

		for (final String existingGroup : existingGroups) {
			if (trimmedName.equalsIgnoreCase(existingGroup)) {
				return Optional.of(GroupNameProblem.DUPLICATE);
			}
		}

		return Optional.empty();
	}
}
