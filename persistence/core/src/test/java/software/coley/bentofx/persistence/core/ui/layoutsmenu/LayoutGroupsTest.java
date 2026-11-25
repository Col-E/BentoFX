package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.ui.layoutsmenu.LayoutGroups.GroupNameProblem;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Coverage for the rules the {@code Layouts} menu organizes groups by.
 *
 * <p>Reached directly rather than through {@code LayoutsMenu}, which needs the
 * JavaFX toolkit to load at all. What the menu does with these answers is
 * assembly; the rules are here.</p>
 *
 * @author Phil Bryant
 */
class LayoutGroupsTest {

	@Test
	void takesGroupsFromTheCatalogAndFromTheLayouts() {
		final List<String> groupNames = LayoutGroups.mergeGroupNames(
				List.of("Presentation"),
				List.of(
						layout("one", "Wide", "Debugging"),
						layout("two", "Narrow", null)
				)
		);

		assertThat(groupNames)
				.describedAs("merged group names")
				.containsExactly("Debugging", "Presentation");
	}

	/**
	 * A group created through the menu is in the catalog, so it is the catalog
	 * that has to survive its last layout being moved out.
	 */
	@Test
	void keepsACatalogGroupThatNoLayoutIsIn() {
		final List<String> groupNames = LayoutGroups.mergeGroupNames(
				List.of("Empty"),
				List.of(layout("one", "Wide", null))
		);

		assertThat(groupNames)
				.describedAs("merged group names")
				.containsExactly("Empty");
	}

	/**
	 * The union of the catalog and the layouts, rather than the catalog alone,
	 * is what stops a rename or a delete that failed part way through from
	 * leaving a stored layout inside a group that no longer appears.
	 */
	@Test
	void keepsAGroupOnlyTheLayoutsStillName() {
		final List<String> groupNames = LayoutGroups.mergeGroupNames(
				List.of(),
				List.of(layout("one", "Wide", "Orphaned"))
		);

		assertThat(groupNames)
				.describedAs("merged group names")
				.containsExactly("Orphaned");
	}

	@Test
	void treatsGroupsDifferingOnlyInCaseAsOneAndKeepsTheCatalogSpelling() {
		final List<String> groupNames = LayoutGroups.mergeGroupNames(
				List.of("Debugging"),
				List.of(layout("one", "Wide", "DEBUGGING"))
		);

		assertThat(groupNames)
				.describedAs("merged group names")
				.containsExactly("Debugging");
	}

	/**
	 * A group with no name would show as a submenu with no label. Nothing the
	 * menu writes produces one; a catalog edited by hand could.
	 */
	@ParameterizedTest
	@ValueSource(strings = {"", " ", "\t"})
	void ignoresAGroupThatNamesNothing(final String groupName) {
		assertThat(LayoutGroups.mergeGroupNames(
				List.of(groupName),
				List.of(layout("one", "Wide", groupName))
		))
				.describedAs("merged group names")
				.isEmpty();
	}

	@Test
	void putsEachLayoutInItsGroupWhateverTheCaseEitherWasSpelledIn() {
		final Map<String, List<LayoutPersistenceProfile>> grouped =
				LayoutGroups.groupLayouts(
						List.of("Debugging"),
						List.of(
								layout("one", "Wide", "debugging"),
								layout("two", "Narrow", "DEBUGGING")
						)
				);

		assertThat(grouped.get("Debugging"))
				.describedAs("layouts in 'Debugging'")
				.extracting(LayoutPersistenceProfile::layoutIdentifier)
				.containsExactly("one", "two");
	}

	/**
	 * A group a user created exists before anything is in it, so it still gets
	 * an entry and the menu still gives it a submenu.
	 */
	@Test
	void givesAnEmptyGroupAnEntryOfItsOwn() {
		final Map<String, List<LayoutPersistenceProfile>> grouped =
				LayoutGroups.groupLayouts(
						List.of("Empty"),
						List.of(layout("one", "Wide", null))
				);

		assertThat(grouped)
				.describedAs("grouped layouts")
				.containsOnlyKeys("Empty");
		assertThat(grouped.get("Empty"))
				.describedAs("layouts in 'Empty'")
				.isEmpty();
	}

	@Test
	void keepsTheGroupsInTheOrderTheyWereGiven() {
		final Map<String, List<LayoutPersistenceProfile>> grouped =
				LayoutGroups.groupLayouts(
						List.of("Zebra", "Aardvark"),
						List.of()
				);

		assertThat(grouped.keySet())
				.describedAs("group order")
				.containsExactly("Zebra", "Aardvark");
	}

	@Test
	void reportsTheLayoutsInNoGroup() {
		assertThat(LayoutGroups.ungroupedLayouts(
				List.of("Debugging"),
				List.of(
						layout("one", "Wide", "Debugging"),
						layout("two", "Narrow", null)
				)
		))
				.describedAs("ungrouped layouts")
				.extracting(LayoutPersistenceProfile::layoutIdentifier)
				.containsExactly("two");
	}

	/**
	 * A layout naming a group that is not being shown has to appear somewhere.
	 * Listing it loose is worse than listing it in its group and better than a
	 * stored layout a user can neither see nor delete.
	 */
	@Test
	void listsALayoutLooseWhenItsGroupIsNotBeingShown() {
		assertThat(LayoutGroups.ungroupedLayouts(
				List.of(),
				List.of(layout("one", "Wide", "Missing"))
		))
				.describedAs("ungrouped layouts")
				.extracting(LayoutPersistenceProfile::layoutIdentifier)
				.containsExactly("one");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   ", "\t"})
	void refusesAGroupNameWithNothingInIt(final String groupName) {
		assertThat(LayoutGroups.findGroupNameProblem(groupName, List.of()))
				.describedAs("problem with '%s'", groupName)
				.contains(GroupNameProblem.BLANK);
	}

	@Test
	void refusesAGroupNameLongerThanTheCap() {
		final String groupName =
				"g".repeat(LayoutGroups.MAX_GROUP_NAME_LENGTH + 1);

		assertThat(LayoutGroups.findGroupNameProblem(groupName, List.of()))
				.describedAs("problem with a name one over the cap")
				.contains(GroupNameProblem.TOO_LONG);
	}

	@Test
	void acceptsAGroupNameExactlyAtTheCap() {
		final String groupName =
				"g".repeat(LayoutGroups.MAX_GROUP_NAME_LENGTH);

		assertThat(LayoutGroups.findGroupNameProblem(groupName, List.of()))
				.describedAs("problem with a name at the cap")
				.isEmpty();
	}

	@Test
	void refusesAGroupNameAlreadyTakenWhateverItsCase() {
		assertThat(LayoutGroups.findGroupNameProblem(
				"debugging",
				List.of("Debugging")
		))
				.describedAs("problem with a name already taken")
				.contains(GroupNameProblem.DUPLICATE);
	}

	@Test
	void comparesAGroupNameAfterTrimmingIt() {
		assertThat(LayoutGroups.findGroupNameProblem(
				"  Debugging  ",
				List.of("Debugging")
		))
				.describedAs("problem with a padded name already taken")
				.contains(GroupNameProblem.DUPLICATE);
	}

	/**
	 * Without excusing the group being renamed, correcting the case of its own
	 * name would be refused as a duplicate of itself.
	 */
	@Test
	void letsAGroupKeepItsOwnNameWhenRenamed() {
		assertThat(LayoutGroups.findGroupNameProblem(
				"DEBUGGING",
				List.of("Debugging"),
				"Debugging"
		))
				.describedAs("problem renaming a group to its own name")
				.isEmpty();
	}

	@Test
	void stillRefusesRenamingAGroupOntoAnother() {
		assertThat(LayoutGroups.findGroupNameProblem(
				"Presentation",
				List.of("Debugging", "Presentation"),
				"Debugging"
		))
				.describedAs("problem renaming a group onto another")
				.contains(GroupNameProblem.DUPLICATE);
	}

	@Test
	// Suppress warnings for passing null argument to parameter annotated as
	// non-null; that's what we're testing.
	@SuppressWarnings("NullAway")
	void refusesMissingArguments() {
		assertThatNullPointerException()
				.describedAs("null catalog groups")
				.isThrownBy(() -> LayoutGroups.mergeGroupNames(null, List.of()))
				.withMessageContaining("catalogGroups");
		assertThatNullPointerException()
				.describedAs("null group name")
				.isThrownBy(() -> LayoutGroups.findGroupNameProblem(null, List.of()))
				.withMessageContaining("groupName");
	}

	/**
	 * {@return a stored layout as the menu receives one from the provider.}
	 *
	 * @param layoutIdentifier addresses the layout.
	 * @param displayName what it is called.
	 * @param group the group it is in, or {@code null} for none.
	 */
	private static LayoutPersistenceProfile layout(
			final String layoutIdentifier,
			final String displayName,
			final @Nullable String group
	) {
		return new LayoutPersistenceProfile(
				layoutIdentifier,
				null,
				null,
				displayName,
				group
		);
	}
}
