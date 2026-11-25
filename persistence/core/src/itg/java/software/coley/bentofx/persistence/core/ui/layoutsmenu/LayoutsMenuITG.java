package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;
import software.coley.bentofx.Bento;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.DockingLayout;
import software.coley.bentofx.persistence.core.api.DockingLayout.DockingLayoutBuilder;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.LayoutRestorer;
import software.coley.bentofx.persistence.core.api.LayoutSaver;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.provider.DockContainerLeafMenuFactoryProvider;
import software.coley.bentofx.persistence.core.api.provider.DockableStateProvider;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutRestorable;
import software.coley.bentofx.persistence.core.api.provider.PersistedDockingLayoutOrganizationProvider;
import software.coley.bentofx.persistence.core.api.provider.StageIconImageProvider;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers.*;

/**
 * Drives {@link LayoutsMenu} through its real menu items and the modal
 * dialogs they raise.
 *
 * <p>{@code activeCustomLayoutProfile} is private to the menu, so these tests
 * cannot set or read it directly; every test instead drives it the only way
 * anything outside the menu can - through {@link #makeActiveLayout()}, which
 * fires a real, successful restore - and asserts on what the menu makes
 * observable from the state it produces: a restored item's checkmark, and
 * whether {@code Save changes}/{@code Rename} are enabled.</p>
 *
 * <p>A menu item's action handler that opens an {@link javafx.scene.control.Alert}
 * or {@link javafx.scene.control.TextInputDialog} calls {@code showAndWait()},
 * which enters a nested JavaFX event loop that blocks the calling frame until
 * the dialog is dismissed. Firing such an item from inside a plain
 * {@code robot.interact(...)} would therefore never return. Instead, {@link #fireMenuAction}
 * queues each dialog's dismissal with {@link Platform#runLater} <em>before</em>
 * firing the item that opens the first one, all from a single outer task: a
 * dialog's nested loop still pumps the platform's task queue while it runs, so
 * each queued dismissal runs - in order, one dialog at a time - and closes its
 * dialog before {@code showAndWait()} (and eventually {@code item.fire()})
 * returns. A disabled item's {@code fire()} does nothing at all, which is why
 * every test that needs {@code Save changes} or {@code Rename} enabled goes
 * through {@link #makeActiveLayout()} first.</p>
 *
 * @author Phil Bryant
 */
@ExtendWith(ApplicationExtension.class)
class LayoutsMenuITG {

	private static final String OTHER_LAYOUT_ID = "other-layout";
	private static final String WIDE_LAYOUT_ID = "debugging-wide";
	private static final String ALIGNED_LAYOUT_ID = "debugging-aligned";
	private static final String GROUPED_ACTIVE_LAYOUT_ID = "debugging-active";
	private static final String GROUPED_ACTIVE_DISPLAY_NAME = "Active Grouped";
	private static final String GROUP_NAME = "Debugging";
	private static final String OTHER_GROUP_NAME = "Presentation";
	private static final String ACTIVE_LAYOUT_ID = "active-layout";

	/**
	 * Duplicated from {@code LayoutsMenu.properties} rather than read from it: a
	 * test that took the label from the same bundle the menu reads would pass
	 * whatever either said.
	 */
	private static final String NO_GROUP_CHOICE = "(No Group)";
	private static final String ACTIVE_LAYOUT_DISPLAY_NAME = "Active Layout";
	private static final String NEW_LAYOUT_DISPLAY_NAME = "My New Layout";
	private static final String NEW_LAYOUT_IDENTIFIER = "my-new-layout";

	/*
	 * Assigned by setUp, but inside the robot.interact() lambda that puts the
	 * work on the FX Application Thread, which NullAway cannot follow. Every
	 * @Test body runs after that lambda has completed.
	 */
	@SuppressWarnings("NullAway.Init")
	private Stage owner;

	private RecordingPersistenceProvider persistenceProvider;
	private RecordingRestorable restorable;

	@SuppressWarnings("NullAway.Init")
	private LayoutsMenu menu;

	@BeforeEach
	void setUp(final FxRobot robot) {
		persistenceProvider = new RecordingPersistenceProvider();
		restorable = new RecordingRestorable(persistenceProvider);

		robot.interact(() -> {
			owner = new Stage();
			owner.setScene(new Scene(new Pane(), 200, 200));
			owner.show();
			menu = new LayoutsMenu(owner, restorable);
		});
	}

	@AfterEach
	void tearDown(final FxRobot robot) {
		robot.interact(() -> owner.hide());
	}

	@Test
	void defaultItemIsCheckedWhenNoCustomLayoutIsActive() {
		assertThat(topItems().getFirst().getText())
				.describedAs("defaultItem.getText()")
				.startsWith("✓");
	}

	/**
	 * A restored session layout the user arranged without naming is not the
	 * default layout: {@code Custom} is checked, and nothing under it is,
	 * because no named layout is showing either.
	 */
	@Test
	void anUnnamedArrangementChecksCustomAndNothingUnderIt(final FxRobot robot) {
		persistenceProvider.storedLayouts.add(
				LayoutPersistenceProfile.named(OTHER_LAYOUT_ID, "Other", null, null)
		);
		// The default layout was recorded with nothing in it, so the root below
		// is not the default layout.
		persistenceProvider.defaultArrangement = List.of();

		robot.interact(() -> {
			final Bento bento = new Bento("menu-bento");
			final DockContainerRootBranch root = bento.dockBuilding().root("root");
			root.addContainer(bento.dockBuilding().leaf("leaf"));
			// A root branch registers with its Bento once it is in a Scene.
			new Scene(root);
			restorable.bentoProvider = BentoProvider.of(bento);
			menu = new LayoutsMenu(owner, restorable);
		});
		repopulate();

		assertThat(topItems().getFirst().getText())
				.describedAs("defaultItem.getText()")
				.doesNotStartWith("✓");
		assertThat(customMenu().getText())
				.describedAs("customMenu.getText()")
				.startsWith("✓");
		assertThat(restoreMenu().getItems().getFirst().getText())
				.describedAs("the stored layout's own item text")
				.doesNotStartWith("✓");
	}

	/**
	 * After a restart the active layout is known only by the identifier that
	 * was recorded, but saving changes to it must keep the name and group it
	 * was stored with.
	 */
	@Test
	void savingChangesAfterARestartKeepsTheLayoutsNameAndGroup(final FxRobot robot) {
		persistenceProvider.storedGroups.add(GROUP_NAME);
		storeLayout(GROUPED_ACTIVE_LAYOUT_ID, GROUPED_ACTIVE_DISPLAY_NAME, GROUP_NAME);
		persistenceProvider.storedIdentifiers.add(GROUPED_ACTIVE_LAYOUT_ID);
		persistenceProvider.activeLayoutIdentifier = GROUPED_ACTIVE_LAYOUT_ID;

		robot.interact(() -> menu = new LayoutsMenu(owner, restorable));
		repopulate();

		fireMenuAction(saveChangesItem());

		assertThat(persistenceProvider.savedProfiles)
				.describedAs("layouts saved by Save changes after a restart")
				.singleElement()
				.satisfies(saved -> {
					assertThat(saved.findDisplayName())
							.describedAs("saved display name")
							.contains(GROUPED_ACTIVE_DISPLAY_NAME);
					assertThat(saved.group())
							.describedAs("saved group")
							.isEqualTo(GROUP_NAME);
				});
	}

	/**
	 * With no session layout saved, the application fell back to its default
	 * layout, so the menu records what is showing as the default arrangement -
	 * once the application has had a chance to attach it.
	 */
	@Test
	void constructingWithNoSessionLayoutRecordsTheDefaultArrangement(final FxRobot robot) {
		persistenceProvider.isSessionStored = false;

		robot.interact(() -> menu = new LayoutsMenu(owner, restorable));
		// Recording is deferred to the next pass of the event loop.
		robot.interact(() -> { /* fence */ });

		assertThat(persistenceProvider.savedProfiles)
				.describedAs("layouts saved after constructing with no session layout")
				.extracting(LayoutPersistenceProfile::layoutIdentifier)
				.containsExactly(DEFAULT_LAYOUT_IDENTIFIER);
	}

	@Test
	void restoringTheDefaultLayoutRecordsTheDefaultArrangement(final FxRobot robot) {
		restorable.switchSucceeds = true;

		fireMenuAction(topItems().getFirst());
		robot.interact(() -> { /* fence */ });

		assertThat(persistenceProvider.savedProfiles)
				.describedAs("layouts saved after restoring the default layout")
				.extracting(LayoutPersistenceProfile::layoutIdentifier)
				.containsExactly(DEFAULT_LAYOUT_IDENTIFIER);
	}

	/**
	 * An application that restores the session layout at startup - which
	 * carries no memory of which named layout it mirrors - has this menu read
	 * the identifier back through {@link PersistedDockingLayoutOrganizationProvider} at
	 * construction instead. This is what lets {@code Custom} and the layout's
	 * own item show selected from the moment the menu opens, without a
	 * restore having gone through this menu at all. {@code Restore} itself
	 * stays unmarked - only {@code Custom}, a group (if any), and the layout
	 * item are.
	 */
	@Test
	void constructingWhenAnActiveLayoutIsRecordedMarksCustomAndTheLayoutItem(
			final FxRobot robot
	) {
		persistenceProvider.storedLayouts.add(
				LayoutPersistenceProfile.named(
						ACTIVE_LAYOUT_ID, ACTIVE_LAYOUT_DISPLAY_NAME, null, null
				)
		);
		persistenceProvider.storedIdentifiers.add(ACTIVE_LAYOUT_ID);
		persistenceProvider.activeLayoutIdentifier = ACTIVE_LAYOUT_ID;

		robot.interact(() -> menu = new LayoutsMenu(owner, restorable));
		repopulate();

		assertThat(topItems().getFirst().getText())
				.describedAs("defaultItem.getText()")
				.doesNotStartWith("✓");
		assertThat(customMenu().getText())
				.describedAs("customMenu.getText()")
				.startsWith("✓");
		assertThat(restoreMenu().getText())
				.describedAs("restoreMenu.getText()")
				.doesNotStartWith("✓");
		assertThat(restoreMenu().getItems().getFirst().getText())
				.describedAs("the active layout's own item text")
				.startsWith("✓");
	}

	/**
	 * The counterpart to
	 * {@link #constructingWhenAnActiveLayoutIsRecordedMarksCustomAndTheLayoutItem}
	 * for a layout that belongs to a group: the group's own submenu is marked
	 * too, and {@code Restore} still is not.
	 */
	@Test
	void constructingWhenAnActiveLayoutInAGroupIsRecordedMarksTheGroup(
			final FxRobot robot
	) {
		persistenceProvider.storedGroups.add(GROUP_NAME);
		storeLayout(
				GROUPED_ACTIVE_LAYOUT_ID, GROUPED_ACTIVE_DISPLAY_NAME, GROUP_NAME
		);
		persistenceProvider.storedIdentifiers.add(GROUPED_ACTIVE_LAYOUT_ID);
		persistenceProvider.activeLayoutIdentifier = GROUPED_ACTIVE_LAYOUT_ID;

		robot.interact(() -> menu = new LayoutsMenu(owner, restorable));
		repopulate();

		assertThat(restoreMenu().getText())
				.describedAs("restoreMenu.getText()")
				.doesNotStartWith("✓");
		assertThat(restoreMenu().getItems().getFirst().getText())
				.describedAs("the layout's group submenu text")
				.startsWith("✓");
	}

	/**
	 * {@code LayoutsMenu#setActiveCustomLayoutProfile} is the one place every
	 * change to the active layout goes through, so exercising it from two
	 * different call sites - a restore, and a return to {@code Default} - is
	 * representative of all of them.
	 */
	@Test
	void switchingLayoutsRecordsTheActiveLayoutEachTime() {
		persistenceProvider.storedLayouts.add(
				LayoutPersistenceProfile.named(
						ACTIVE_LAYOUT_ID, ACTIVE_LAYOUT_DISPLAY_NAME, null, null
				)
		);
		persistenceProvider.storedIdentifiers.add(ACTIVE_LAYOUT_ID);
		restorable.switchSucceeds = true;
		repopulate();

		fireMenuAction(restoreMenu().getItems().getFirst());

		assertThat(persistenceProvider.activeLayoutIdentifier)
				.describedAs("active layout identifier after a restore")
				.isEqualTo(ACTIVE_LAYOUT_ID);

		repopulate();
		fireMenuAction(topItems().getFirst());

		assertThat(persistenceProvider.activeLayoutIdentifier)
				.describedAs("active layout identifier after restoring the default layout")
				.isNull();
	}

	@Test
	void customMenuShowsAListFailedItemWhenStorageCannotBeListed() {
		persistenceProvider.listFails = true;
		repopulate();

		assertThat(restoreMenu().getItems())
				.describedAs("restoreMenu.getItems()")
				.hasSize(1);
		assertThat(restoreMenu().getItems().getFirst().isDisable())
				.describedAs("restoreMenu list-failed item disabled")
				.isTrue();
		assertThat(deleteMenu().getItems())
				.describedAs("deleteMenu.getItems()")
				.hasSize(1);
	}

	@Test
	void customMenuShowsANoLayoutsItemWhenNothingIsStored() {
		repopulate();

		assertThat(restoreMenu().getItems())
				.describedAs("restoreMenu.getItems()")
				.hasSize(1);
		assertThat(restoreMenu().getItems().getFirst().isDisable())
				.describedAs("restoreMenu no-layouts item disabled")
				.isTrue();
	}

	@Test
	void customMenuListsStoredLayoutsSortedByDisplayNameWithTheActiveOneChecked() {
		makeActiveLayout();
		persistenceProvider.storedLayouts.add(
				LayoutPersistenceProfile.named(OTHER_LAYOUT_ID, "Zebra", null, null)
		);
		repopulate();

		assertThat(restoreMenu().getItems())
				.describedAs("restoreMenu.getItems()")
				.hasSize(2);
		assertThat(restoreMenu().getItems().getFirst().getText())
				.describedAs("first (sorted) restore item")
				.startsWith("✓")
				.contains(ACTIVE_LAYOUT_DISPLAY_NAME);
		assertThat(restoreMenu().getItems().get(1).getText())
				.describedAs("second (sorted) restore item")
				.doesNotContain("✓")
				.contains("Zebra");
		assertThat(deleteMenu().getItems())
				.describedAs("deleteMenu.getItems()")
				.hasSize(2);
	}

	/**
	 * A layout's group comes from its own field, so the display name is only ever
	 * a name - including one holding the character that used to split the two.
	 */
	@Test
	void customMenuNestsGroupedLayoutsIntoASubmenuBeforeTheLooseOnes() {
		persistenceProvider.storedGroups.add(GROUP_NAME);
		storeLayout(WIDE_LAYOUT_ID, "TCP/IP Debug", GROUP_NAME);
		storeLayout(ALIGNED_LAYOUT_ID, "Aligned", GROUP_NAME);
		storeLayout(OTHER_LAYOUT_ID, "Zebra", null);
		repopulate();

		assertThat(restoreMenu().getItems())
				.describedAs("restoreMenu.getItems()")
				.hasSize(2);

		final MenuItem groupItem = restoreMenu().getItems().getFirst();

		assertThat(groupItem)
				.describedAs("first restore item, the group")
				.isInstanceOf(Menu.class);
		assertThat(groupItem.getText())
				.describedAs("group submenu text")
				.isEqualTo(GROUP_NAME);
		assertThat(((Menu) groupItem).getItems())
				.describedAs("layouts within the group, whole names and all")
				.extracting(MenuItem::getText)
				.containsExactly("Aligned", "TCP/IP Debug");

		assertThat(restoreMenu().getItems().get(1))
				.describedAs("second restore item, in no group")
				.isNotInstanceOf(Menu.class);
		assertThat(restoreMenu().getItems().get(1).getText())
				.describedAs("loose restore item text")
				.isEqualTo("Zebra");
	}

	/**
	 * The group catalog is stored as an ordinary layout entry, so listing what a
	 * destination holds reports it like anything else. It holds no docking state
	 * and nothing could restore it, so it has to be filtered out of every list a
	 * user acts on.
	 */
	@Test
	void customMenuLeavesTheGroupCatalogOutOfEveryLayoutList() {
		storeLayout(GROUP_CATALOG_LAYOUT_IDENTIFIER, null, null);
		storeLayout(OTHER_LAYOUT_ID, "Zebra", null);
		repopulate();

		assertThat(restoreMenu().getItems())
				.describedAs("restoreMenu.getItems()")
				.extracting(MenuItem::getText)
				.containsExactly("Zebra");
		assertThat(deleteMenu().getItems())
				.describedAs("deleteMenu.getItems()")
				.extracting(MenuItem::getText)
				.containsExactly("Zebra");
		assertThat(renameMenu().getItems())
				.describedAs("renameMenu.getItems()")
				.extracting(MenuItem::getText)
				.containsExactly("Zebra");
	}

	/**
	 * A group a user created exists before there is a layout in it, which is the
	 * whole reason the catalog is stored apart from the layouts.
	 */
	@Test
	void customMenuShowsAGroupWithNoLayoutsInIt() {
		persistenceProvider.storedGroups.add(GROUP_NAME);
		repopulate();

		final MenuItem groupItem = restoreMenu().getItems().getFirst();

		assertThat(groupItem.getText())
				.describedAs("empty group submenu text")
				.isEqualTo(GROUP_NAME);
		assertThat(((Menu) groupItem).getItems())
				.describedAs("items inside an empty group")
				.hasSize(1);
		assertThat(((Menu) groupItem).getItems().getFirst().isDisable())
				.describedAs("stand-in inside an empty group disabled")
				.isTrue();
	}

	/**
	 * A group is marked when it holds the layout showing now, so that finding
	 * that layout does not mean opening every group in turn.
	 */
	@Test
	void customMenuMarksTheGroupHoldingTheActiveLayout() {
		makeActiveGroupedLayout();
		persistenceProvider.storedGroups.add(OTHER_GROUP_NAME);
		storeLayout(OTHER_LAYOUT_ID, "Wide", OTHER_GROUP_NAME);
		repopulate();

		final Menu activeGroup = (Menu) restoreMenu().getItems().getFirst();

		assertThat(activeGroup.getText())
				.describedAs("group holding the active layout")
				.startsWith("✓")
				.contains(GROUP_NAME);
		assertThat(activeGroup.getItems().getFirst().getText())
				.describedAs("the active layout's own item")
				.startsWith("✓")
				.contains(GROUPED_ACTIVE_DISPLAY_NAME);

		assertThat(restoreMenu().getItems().get(1).getText())
				.describedAs("group holding no active layout")
				.doesNotContain("✓")
				.contains(OTHER_GROUP_NAME);
	}

	/**
	 * The delete menu groups the same way and marks nothing: a check mark there
	 * would read as naming the layout about to be deleted.
	 */
	@Test
	void deleteMenuGroupsTheSameWayWithoutMarkingTheActiveLayout() {
		makeActiveGroupedLayout();
		repopulate();

		final MenuItem groupItem = deleteMenu().getItems().getFirst();

		assertThat(groupItem)
				.describedAs("first delete item, the group")
				.isInstanceOf(Menu.class);
		assertThat(groupItem.getText())
				.describedAs("delete group submenu text")
				.isEqualTo(GROUP_NAME);
		assertThat(((Menu) groupItem).getItems())
				.describedAs("layouts within the delete group")
				.extracting(MenuItem::getText)
				.containsExactly(GROUPED_ACTIVE_DISPLAY_NAME);
	}

	@Test
	void newGroupAddsToTheCatalogWithoutTouchingAnyLayout() {
		repopulate();

		fireMenuAction(newGroupItem(), typeAndDismiss(GROUP_NAME, ButtonType.OK));

		assertThat(persistenceProvider.storedGroups)
				.describedAs("stored group catalog")
				.containsExactly(GROUP_NAME);
		assertThat(persistenceProvider.renamedProfiles)
				.describedAs("layouts rewritten by creating a group")
				.isEmpty();
	}

	@Test
	void newGroupDoesNothingWhenTheDialogIsCancelled() {
		repopulate();

		fireMenuAction(newGroupItem(), typeAndDismiss(GROUP_NAME, ButtonType.CANCEL));

		assertThat(persistenceProvider.storedGroups)
				.describedAs("stored group catalog")
				.isEmpty();
	}

	@ParameterizedTest(name = "rejects \"{0}\"")
	@ValueSource(strings = {"", "   "})
	void newGroupShowsAProblemErrorForAnUnusableName(final String groupName) {
		repopulate();

		fireMenuAction(
				newGroupItem(),
				typeAndDismiss(groupName, ButtonType.OK),
				dismiss(ButtonType.OK)
		);

		assertThat(persistenceProvider.storedGroups)
				.describedAs("stored group catalog")
				.isEmpty();
	}

	@Test
	void newGroupRefusesANameAlreadyTaken() {
		persistenceProvider.storedGroups.add(GROUP_NAME);
		repopulate();

		fireMenuAction(
				newGroupItem(),
				typeAndDismiss(GROUP_NAME.toUpperCase(Locale.ROOT), ButtonType.OK),
				dismiss(ButtonType.OK)
		);

		assertThat(persistenceProvider.storedGroups)
				.describedAs("stored group catalog")
				.containsExactly(GROUP_NAME);
	}

	@Test
	void renameGroupRenamesTheCatalogEntryAndEveryLayoutInIt() {
		persistenceProvider.storedGroups.add(GROUP_NAME);
		storeLayout(WIDE_LAYOUT_ID, "Wide", GROUP_NAME);
		storeLayout(OTHER_LAYOUT_ID, "Zebra", null);
		repopulate();

		fireMenuAction(
				renameGroupMenu().getItems().getFirst(),
				typeAndDismiss(OTHER_GROUP_NAME, ButtonType.OK)
		);

		assertThat(persistenceProvider.storedGroups)
				.describedAs("stored group catalog")
				.containsExactly(OTHER_GROUP_NAME);
		assertThat(persistenceProvider.storedLayouts)
				.describedAs("groups the layouts record")
				.extracting(LayoutPersistenceProfile::group)
				.containsExactly(OTHER_GROUP_NAME, null);
	}

	/**
	 * The layouts in the group are kept and end up in no group. Deleting a group
	 * that deleted layouts would be a data loss no confirmation makes acceptable.
	 */
	@Test
	void deleteGroupKeepsItsLayoutsAndTakesThemOutOfTheGroup() {
		persistenceProvider.storedGroups.add(GROUP_NAME);
		storeLayout(WIDE_LAYOUT_ID, "Wide", GROUP_NAME);
		repopulate();

		fireMenuAction(deleteGroupMenu().getItems().getFirst(), dismiss(ButtonType.YES));

		assertThat(persistenceProvider.storedGroups)
				.describedAs("stored group catalog")
				.isEmpty();
		assertThat(persistenceProvider.deletedProfiles)
				.describedAs("layouts deleted along with the group")
				.isEmpty();
		assertThat(persistenceProvider.storedLayouts)
				.describedAs("groups the layouts record")
				.extracting(LayoutPersistenceProfile::group)
				.containsExactly((String) null);
	}

	@Test
	void deleteGroupDoesNothingWhenTheUserDeclinesToConfirm() {
		persistenceProvider.storedGroups.add(GROUP_NAME);
		storeLayout(WIDE_LAYOUT_ID, "Wide", GROUP_NAME);
		repopulate();

		fireMenuAction(deleteGroupMenu().getItems().getFirst(), dismiss(ButtonType.NO));

		assertThat(persistenceProvider.storedGroups)
				.describedAs("stored group catalog")
				.containsExactly(GROUP_NAME);
		assertThat(persistenceProvider.storedLayouts)
				.describedAs("groups the layouts record")
				.extracting(LayoutPersistenceProfile::group)
				.containsExactly(GROUP_NAME);
	}

	/**
	 * Moving needs a group to move into, and an item opening a picker with
	 * nothing in it is worse than one that says so.
	 */
	@Test
	void moveToGroupSaysSoWhenNoGroupExists() {
		storeLayout(WIDE_LAYOUT_ID, "Wide", null);
		repopulate();

		assertThat(moveToGroupMenu().getItems())
				.describedAs("moveToGroupMenu.getItems()")
				.hasSize(1);
		assertThat(moveToGroupMenu().getItems().getFirst().isDisable())
				.describedAs("no-groups stand-in disabled")
				.isTrue();
	}

	@Test
	void moveToGroupFilesTheLayoutWithoutRestoringIt() {
		persistenceProvider.storedGroups.add(GROUP_NAME);
		storeLayout(WIDE_LAYOUT_ID, "Wide", null);
		repopulate();

		// Groups come first, so the empty 'Debugging' submenu is item 0 and the
		// ungrouped layout is item 1. Firing the submenu would open no dialog.
		fireMenuAction(
				moveToGroupMenu().getItems().get(1),
				selectChoiceAndDismiss(GROUP_NAME)
		);

		assertThat(persistenceProvider.storedLayouts)
				.describedAs("groups the layouts record")
				.extracting(LayoutPersistenceProfile::group)
				.containsExactly(GROUP_NAME);
		assertThat(restorable.switchCalls)
				.describedAs("layouts restored in order to move one")
				.isEmpty();
	}

	@Test
	void moveToGroupTakesALayoutOutOfItsGroup() {
		persistenceProvider.storedGroups.add(GROUP_NAME);
		storeLayout(WIDE_LAYOUT_ID, "Wide", GROUP_NAME);
		repopulate();

		fireMenuAction(
				((Menu) moveToGroupMenu().getItems().getFirst()).getItems().getFirst(),
				selectChoiceAndDismiss(NO_GROUP_CHOICE)
		);

		assertThat(persistenceProvider.storedLayouts)
				.describedAs("groups the layouts record")
				.extracting(LayoutPersistenceProfile::group)
				.containsExactly((String) null);
	}

	@Test
	void restoreDefaultLayoutSwitchesAndClearsTheActiveProfile() {
		makeActiveLayout();
		restorable.switchSucceeds = true;

		fireMenuAction(topItems().getFirst());
		repopulate();

		assertThat(topItems().getFirst().getText())
				.describedAs("defaultItem.getText() after restoring the default layout")
				.startsWith("✓");
	}

	@Test
	void restoreDefaultLayoutShowsAnErrorAndLeavesTheActiveProfileWhenSwitchingFails() {
		makeActiveLayout();
		restorable.switchSucceeds = false;

		fireMenuAction(topItems().getFirst(), dismiss(ButtonType.OK));
		repopulate();

		assertThat(topItems().getFirst().getText())
				.describedAs("defaultItem.getText() after a failed default-layout restore")
				.doesNotContain("✓");
		assertThat(restoreMenu().getItems().getFirst().getText())
				.describedAs("active layout's restore item text after a failed default-layout restore")
				.startsWith("✓");
	}

	@Test
	void restoreStoredLayoutSwitchesAndSetsTheActiveProfile() {
		persistenceProvider.storedLayouts.add(
				LayoutPersistenceProfile.named(OTHER_LAYOUT_ID, "Other", null, null)
		);
		restorable.switchSucceeds = true;
		repopulate();

		fireMenuAction(restoreMenu().getItems().getFirst());
		repopulate();

		assertThat(restoreMenu().getItems().getFirst().getText())
				.describedAs("restore item text after restoring a stored layout")
				.startsWith("✓");
		assertThat(saveChangesItem().isDisable())
				.describedAs("saveChangesItem.isDisable() after restoring a stored layout")
				.isFalse();
	}

	@Test
	void restoreStoredLayoutShowsAnErrorAndLeavesTheActiveProfileWhenSwitchingFails() {
		makeActiveLayout();
		persistenceProvider.storedLayouts.add(
				LayoutPersistenceProfile.named(OTHER_LAYOUT_ID, "Other", null, null)
		);
		restorable.switchSucceeds = false;
		repopulate();

		// Sorted by display name: "Active Layout" then "Other".
		fireMenuAction(restoreMenu().getItems().get(1), dismiss(ButtonType.OK));
		repopulate();

		assertThat(restoreMenu().getItems().getFirst().getText())
				.describedAs("active layout's restore item text after a failed restore of another layout")
				.startsWith("✓");
		assertThat(restoreMenu().getItems().get(1).getText())
				.describedAs("other layout's restore item text after a failed restore attempt")
				.doesNotContain("✓");
	}

	@Test
	void saveAsNewDoesNothingWhenTheDialogIsCancelled() {
		fireMenuAction(topItems().get(2), dismiss(ButtonType.CANCEL));

		assertThat(persistenceProvider.savedProfiles)
				.describedAs("persistenceProvider.savedProfiles")
				.isEmpty();
	}

	/**
	 * Covers three distinct reasons a typed name can be rejected before
	 * anything is saved: blank, derives the framework's reserved identifier,
	 * and derives a Windows reserved device name. Each name exercises a
	 * different branch of the same validation but the same reaction, so one
	 * parameterized test replaces what used to be three copies of it.
	 */
	@ParameterizedTest(name = "rejects \"{0}\"")
	@ValueSource(strings = {"   ", "Session", "CON"})
	void saveAsNewShowsAProblemErrorForAnInvalidName(final String invalidName) {
		fireMenuAction(
				topItems().get(2),
				typeAndDismiss(invalidName, ButtonType.OK),
				dismiss(ButtonType.OK)
		);

		assertThat(persistenceProvider.savedProfiles)
				.describedAs("persistenceProvider.savedProfiles")
				.isEmpty();
	}

	@Test
	void saveAsNewShowsAnErrorWhenCheckingExistenceFails() {
		persistenceProvider.isLayoutStoredFails = true;

		fireMenuAction(
				topItems().get(2),
				typeAndDismiss(NEW_LAYOUT_DISPLAY_NAME, ButtonType.OK),
				dismiss(ButtonType.OK)
		);

		assertThat(persistenceProvider.savedProfiles)
				.describedAs("persistenceProvider.savedProfiles")
				.isEmpty();
	}

	@Test
	void saveAsNewWritesTheLayoutWhenNothingIsStoredUnderTheDerivedIdentifier() {
		fireMenuAction(topItems().get(2), typeAndDismiss(NEW_LAYOUT_DISPLAY_NAME, ButtonType.OK));
		repopulate();

		assertThat(persistenceProvider.savedProfiles)
				.describedAs("persistenceProvider.savedProfiles")
				.extracting(LayoutPersistenceProfile::layoutIdentifier)
				.containsExactly(NEW_LAYOUT_IDENTIFIER);
		assertThat(saveChangesItem().isDisable())
				.describedAs("saveChangesItem.isDisable() after saving a new layout")
				.isFalse();
	}

	@Test
	void saveAsNewDoesNothingWhenAlreadyStoredAndTheUserDeclinesToReplace() {
		persistenceProvider.storedIdentifiers.add(NEW_LAYOUT_IDENTIFIER);

		fireMenuAction(
				topItems().get(2),
				typeAndDismiss(NEW_LAYOUT_DISPLAY_NAME, ButtonType.OK),
				dismiss(ButtonType.NO)
		);

		assertThat(persistenceProvider.savedProfiles)
				.describedAs("persistenceProvider.savedProfiles")
				.isEmpty();
	}

	@Test
	void saveAsNewWritesTheLayoutWhenAlreadyStoredAndTheUserConfirmsReplace() {
		persistenceProvider.storedIdentifiers.add(NEW_LAYOUT_IDENTIFIER);

		fireMenuAction(
				topItems().get(2),
				typeAndDismiss(NEW_LAYOUT_DISPLAY_NAME, ButtonType.OK),
				dismiss(ButtonType.YES)
		);

		assertThat(persistenceProvider.savedProfiles)
				.describedAs("persistenceProvider.savedProfiles")
				.extracting(LayoutPersistenceProfile::layoutIdentifier)
				.containsExactly(NEW_LAYOUT_IDENTIFIER);
	}

	/**
	 * The groups come from the catalog, which is read separately from the
	 * layouts, so listing them can fail on its own. Saving stops rather than
	 * filing the layout under no group, which would look like a deliberate
	 * choice the user did not make.
	 */
	@Test
	void saveAsNewShowsAnErrorWhenTheGroupsCannotBeListed() {
		persistenceProvider.groupsFail = true;

		fireMenuAction(
				topItems().get(2),
				typeAndDismiss(NEW_LAYOUT_DISPLAY_NAME, ButtonType.OK),
				dismiss(ButtonType.OK)
		);

		assertThat(persistenceProvider.savedProfiles)
				.describedAs("persistenceProvider.savedProfiles")
				.isEmpty();
	}

	/**
	 * The group picker only appears when a group exists, and backing out of it
	 * abandons the save rather than falling back to no group.
	 */
	@Test
	void saveAsNewDoesNothingWhenTheGroupChoiceIsCancelled() {
		persistenceProvider.storedGroups.add(GROUP_NAME);

		fireMenuAction(
				topItems().get(2),
				typeAndDismiss(NEW_LAYOUT_DISPLAY_NAME, ButtonType.OK),
				dismiss(ButtonType.CANCEL)
		);

		assertThat(persistenceProvider.savedProfiles)
				.describedAs("persistenceProvider.savedProfiles")
				.isEmpty();
	}

	@Test
	void saveAsNewFilesTheLayoutInTheChosenGroup() {
		persistenceProvider.storedGroups.add(GROUP_NAME);

		fireMenuAction(
				topItems().get(2),
				typeAndDismiss(NEW_LAYOUT_DISPLAY_NAME, ButtonType.OK),
				selectChoiceAndDismiss(GROUP_NAME)
		);

		assertThat(persistenceProvider.savedProfiles)
				.describedAs("persistenceProvider.savedProfiles")
				.extracting(LayoutPersistenceProfile::group)
				.containsExactly(GROUP_NAME);
	}

	@Test
	void moveToGroupShowsAnErrorWhenTheGroupsCannotBeListed() {
		persistenceProvider.storedGroups.add(GROUP_NAME);
		storeLayout(WIDE_LAYOUT_ID, "Wide", null);
		repopulate();

		// Set after repopulate: building the menu lists the groups too, and a
		// failure then would leave no move-to-group item to fire.
		persistenceProvider.groupsFail = true;

		fireMenuAction(moveToGroupMenu().getItems().get(1), dismiss(ButtonType.OK));

		assertThat(persistenceProvider.storedLayouts)
				.describedAs("groups the layouts record")
				.extracting(LayoutPersistenceProfile::group)
				.containsExactly((String) null);
	}

	@Test
	void moveToGroupDoesNothingWhenTheDialogIsCancelled() {
		persistenceProvider.storedGroups.add(GROUP_NAME);
		storeLayout(WIDE_LAYOUT_ID, "Wide", null);
		repopulate();

		fireMenuAction(moveToGroupMenu().getItems().get(1), dismiss(ButtonType.CANCEL));

		assertThat(persistenceProvider.storedLayouts)
				.describedAs("groups the layouts record")
				.extracting(LayoutPersistenceProfile::group)
				.containsExactly((String) null);
	}

	@Test
	void saveChangesWritesTheActiveLayout() {
		makeActiveLayout();

		fireMenuAction(saveChangesItem());

		assertThat(persistenceProvider.savedProfiles)
				.describedAs("persistenceProvider.savedProfiles")
				.extracting(LayoutPersistenceProfile::layoutIdentifier)
				.containsExactly(ACTIVE_LAYOUT_ID);
	}

	@Test
	void saveChangesShowsAnErrorWhenTheWriteFails() {
		makeActiveLayout();
		persistenceProvider.saveFails = true;

		fireMenuAction(saveChangesItem(), dismiss(ButtonType.OK));

		assertThat(persistenceProvider.savedProfiles)
				.describedAs("persistenceProvider.savedProfiles")
				.isEmpty();
	}

	@Test
	void renameDoesNothingWhenTheDialogIsCancelled() {
		makeActiveLayout();

		fireMenuAction(renameMenu().getItems().getFirst(), dismiss(ButtonType.CANCEL));

		assertThat(persistenceProvider.renamedProfiles)
				.describedAs("persistenceProvider.renamedProfiles")
				.isEmpty();
	}

	@Test
	void renameShowsAnErrorForABlankName() {
		makeActiveLayout();

		fireMenuAction(
				renameMenu().getItems().getFirst(),
				typeAndDismiss("   ", ButtonType.OK),
				dismiss(ButtonType.OK)
		);

		assertThat(persistenceProvider.renamedProfiles)
				.describedAs("persistenceProvider.renamedProfiles")
				.isEmpty();
	}

	/**
	 * A rename rewrites the name and nothing else. It used to be a save as well,
	 * which both restricted it to the layout on screen and stored whatever
	 * arrangement happened to be showing, so this asserts that no save happened.
	 */
	@Test
	void renameRewritesOnlyTheDisplayNameUnderTheSameIdentifier() {
		makeActiveLayout();

		fireMenuAction(
				renameMenu().getItems().getFirst(),
				typeAndDismiss("Renamed Layout", ButtonType.OK)
		);

		assertThat(persistenceProvider.renamedProfiles)
				.describedAs("persistenceProvider.renamedProfiles")
				.hasSize(1);
		assertThat(persistenceProvider.renamedProfiles.getFirst().layoutIdentifier())
				.describedAs("renamed profile's identifier")
				.isEqualTo(ACTIVE_LAYOUT_ID);
		assertThat(persistenceProvider.renamedProfiles.getFirst().displayName())
				.describedAs("renamed profile's display name")
				.isEqualTo("Renamed Layout");
		assertThat(persistenceProvider.savedProfiles)
				.describedAs("layouts saved by a rename")
				.isEmpty();
	}

	/**
	 * Saving changes writes the active layout's name, so a rename of the active
	 * layout has to reach what the next save writes, or the save undoes it.
	 */
	@Test
	void savingChangesAfterRenamingTheActiveLayoutKeepsTheNewName() {
		makeActiveLayout();

		fireMenuAction(
				renameMenu().getItems().getFirst(),
				typeAndDismiss("Renamed Layout", ButtonType.OK)
		);
		repopulate();
		fireMenuAction(saveChangesItem());

		assertThat(persistenceProvider.savedProfiles)
				.describedAs("layouts saved by Save changes after a rename")
				.singleElement()
				.extracting(LayoutPersistenceProfile::displayName)
				.isEqualTo("Renamed Layout");
	}

	/**
	 * Deleting a group takes its layouts out of it. Saving changes to one of them
	 * afterwards must not put it back in the group, which would also bring the
	 * deleted group back.
	 */
	@Test
	void savingChangesAfterDeletingTheActiveLayoutsGroupKeepsItOutOfTheGroup() {
		makeActiveGroupedLayout();

		fireMenuAction(deleteGroupMenu().getItems().getFirst(), dismiss(ButtonType.YES));
		repopulate();
		fireMenuAction(saveChangesItem());

		assertThat(persistenceProvider.savedProfiles)
				.describedAs("layouts saved by Save changes after deleting their group")
				.singleElement()
				.extracting(LayoutPersistenceProfile::group)
				.isNull();
	}

	/**
	 * Renaming used to be offered only for the layout on screen. It is now
	 * offered for any stored layout, which is the same capability group rename
	 * needs.
	 */
	@Test
	void renameIsOfferedForALayoutThatIsNotShowing() {
		storeLayout(OTHER_LAYOUT_ID, "Zebra", null);
		repopulate();

		fireMenuAction(
				renameMenu().getItems().getFirst(),
				typeAndDismiss("Renamed Layout", ButtonType.OK)
		);

		assertThat(persistenceProvider.renamedProfiles)
				.describedAs("persistenceProvider.renamedProfiles")
				.extracting(LayoutPersistenceProfile::layoutIdentifier)
				.containsExactly(OTHER_LAYOUT_ID);
	}

	/**
	 * Renaming a layout does not move it, because the group travels with the
	 * layout rather than being read out of its name.
	 */
	@Test
	void renameLeavesTheLayoutInItsGroup() {
		persistenceProvider.storedGroups.add(GROUP_NAME);
		storeLayout(WIDE_LAYOUT_ID, "Wide", GROUP_NAME);
		repopulate();

		fireMenuAction(
				((Menu) renameMenu().getItems().getFirst()).getItems().getFirst(),
				typeAndDismiss("Renamed Layout", ButtonType.OK)
		);

		assertThat(persistenceProvider.renamedProfiles)
				.describedAs("persistenceProvider.renamedProfiles")
				.extracting(LayoutPersistenceProfile::group)
				.containsExactly(GROUP_NAME);
	}

	@Test
	void deleteDoesNothingWhenTheUserDeclinesToConfirm() {
		persistenceProvider.storedLayouts.add(
				LayoutPersistenceProfile.named(OTHER_LAYOUT_ID, "Other", null, null)
		);
		repopulate();

		fireMenuAction(deleteMenu().getItems().getFirst(), dismiss(ButtonType.NO));

		assertThat(persistenceProvider.deletedProfiles)
				.describedAs("persistenceProvider.deletedProfiles")
				.isEmpty();
	}

	@Test
	void deleteClearsTheActiveProfileWhenTheDeletedLayoutWasActive() {
		makeActiveLayout();

		fireMenuAction(deleteMenu().getItems().getFirst(), dismiss(ButtonType.YES));
		repopulate();

		assertThat(persistenceProvider.deletedProfiles)
				.describedAs("persistenceProvider.deletedProfiles")
				.extracting(LayoutPersistenceProfile::layoutIdentifier)
				.containsExactly(ACTIVE_LAYOUT_ID);
		assertThat(topItems().getFirst().getText())
				.describedAs("defaultItem.getText() after deleting the active layout")
				.startsWith("✓");
	}

	@Test
	void deleteLeavesTheActiveProfileWhenTheDeletedLayoutWasNotActive() {
		makeActiveLayout();
		persistenceProvider.storedLayouts.add(
				LayoutPersistenceProfile.named(OTHER_LAYOUT_ID, "Other", null, null)
		);
		repopulate();

		// Sorted by display name: "Active Layout" then "Other".
		fireMenuAction(deleteMenu().getItems().get(1), dismiss(ButtonType.YES));
		repopulate();

		assertThat(persistenceProvider.deletedProfiles)
				.describedAs("persistenceProvider.deletedProfiles")
				.extracting(LayoutPersistenceProfile::layoutIdentifier)
				.containsExactly(OTHER_LAYOUT_ID);
		assertThat(restoreMenu().getItems().getFirst().getText())
				.describedAs("active layout's restore item text after deleting an unrelated layout")
				.startsWith("✓");
	}

	@Test
	void deleteShowsAnErrorWhenTheDeleteFails() {
		persistenceProvider.storedLayouts.add(
				LayoutPersistenceProfile.named(OTHER_LAYOUT_ID, "Other", null, null)
		);
		persistenceProvider.deleteFails = true;
		repopulate();

		fireMenuAction(
				deleteMenu().getItems().getFirst(),
				dismiss(ButtonType.YES),
				dismiss(ButtonType.OK)
		);

		assertThat(persistenceProvider.deletedProfiles)
				.describedAs("persistenceProvider.deletedProfiles")
				.isEmpty();
	}

	// --- Helpers -----------------------------------------------------------

	/**
	 * Stores and then successfully restores {@code ACTIVE_LAYOUT_ID}, so the
	 * menu's own {@code activeCustomLayoutProfile} - unreachable directly from
	 * a test - becomes that layout, the only way anything outside the menu
	 * can put it in that state.
	 */
	private void makeActiveLayout() {
		persistenceProvider.storedLayouts.add(
				LayoutPersistenceProfile.named(
						ACTIVE_LAYOUT_ID, ACTIVE_LAYOUT_DISPLAY_NAME, null, null
				)
		);
		persistenceProvider.storedIdentifiers.add(ACTIVE_LAYOUT_ID);
		restorable.switchSucceeds = true;
		repopulate();

		fireMenuAction(restoreMenu().getItems().getFirst());
		repopulate();
	}

	/**
	 * Stores and then successfully restores a layout whose name puts it in a
	 * group, so that the menu's active layout is one nested in a submenu.
	 *
	 * <p>The counterpart to {@link #makeActiveLayout()}, and it fires from
	 * inside the group rather than from the restore menu directly: a group's
	 * submenu is what sits at the top of that menu, and firing a
	 * {@link Menu} restores nothing.</p>
	 */
	private void makeActiveGroupedLayout() {
		persistenceProvider.storedGroups.add(GROUP_NAME);
		storeLayout(
				GROUPED_ACTIVE_LAYOUT_ID,
				GROUPED_ACTIVE_DISPLAY_NAME,
				GROUP_NAME
		);
		persistenceProvider.storedIdentifiers.add(GROUPED_ACTIVE_LAYOUT_ID);
		restorable.switchSucceeds = true;
		repopulate();

		fireMenuAction(((Menu) restoreMenu().getItems().getFirst()).getItems().getFirst());
		repopulate();
	}

	/**
	 * Puts one layout in storage under the supplied name and group.
	 *
	 * @param layoutIdentifier addresses the layout.
	 * @param displayName the name the menu labels it with.
	 * @param group the group it is in, or {@code null} for none.
	 */
	private void storeLayout(
			final String layoutIdentifier,
			final @Nullable String displayName,
			final @Nullable String group
	) {
		persistenceProvider.storedLayouts.add(new LayoutPersistenceProfile(
				layoutIdentifier, null, null, displayName, group
		));
	}

	private List<MenuItem> topItems() {
		return menu.getItems();
	}

	private Menu customMenu() {
		return (Menu) topItems().get(1);
	}

	private Menu restoreMenu() {
		return (Menu) customMenu().getItems().getFirst();
	}

	private MenuItem saveChangesItem() {
		return customMenu().getItems().get(1);
	}

	private Menu renameMenu() {
		return (Menu) customMenu().getItems().get(2);
	}

	private Menu moveToGroupMenu() {
		return (Menu) customMenu().getItems().get(3);
	}

	private Menu deleteMenu() {
		return (Menu) customMenu().getItems().get(4);
	}

	private Menu groupsMenu() {
		return (Menu) customMenu().getItems().get(5);
	}

	private MenuItem newGroupItem() {
		return groupsMenu().getItems().getFirst();
	}

	private Menu renameGroupMenu() {
		return (Menu) groupsMenu().getItems().get(1);
	}

	private Menu deleteGroupMenu() {
		return (Menu) groupsMenu().getItems().get(2);
	}

	private void repopulate() {
		Platform.runLater(() -> menu.getOnShowing().handle(null));
		WaitForAsyncUtils.waitForFxEvents();
	}

	/**
	 * Fires a menu item's action on the FX thread, dismissing any dialogs it
	 * raises with the supplied steps, one dialog at a time and in order - see
	 * the class documentation for why queuing them ahead of {@code item.fire()}
	 * works.
	 */
	private static void fireMenuAction(final MenuItem item, final Runnable... dialogSteps) {
		Platform.runLater(() -> {
			for (final Runnable step : dialogSteps) {
				Platform.runLater(step);
			}
			item.fire();
		});
		WaitForAsyncUtils.waitForFxEvents();
	}

	/** A step that dismisses whatever dialog is currently showing with one button. */
	private static Runnable dismiss(final ButtonType buttonType) {
		return () -> fireDialogButton(findShowingDialogPane(), buttonType);
	}

	/** A step that types into the currently-showing text-input dialog, then dismisses it. */
	private static Runnable typeAndDismiss(final String text, final ButtonType buttonType) {
		return () -> {
			final DialogPane pane = findShowingDialogPane();
			((TextField) pane.lookup(".text-field")).setText(text);
			fireDialogButton(pane, buttonType);
		};
	}

	/**
	 * A step that picks a value in the currently-showing choice dialog, then
	 * dismisses it.
	 *
	 * <p>The selection goes through the {@link ComboBox} the dialog builds, which
	 * is what the dialog reads its result from - setting it is the same act as a
	 * user picking from the list.</p>
	 */
	private static Runnable selectChoiceAndDismiss(
			final String choice
	) {
		return () -> {
			final DialogPane pane = findShowingDialogPane();

			@SuppressWarnings("unchecked")
			final ComboBox<String> choices =
					(ComboBox<String>) pane.lookup(".combo-box");

			assertThat(choices.getItems())
					.describedAs("choices offered by the dialog")
					.contains(choice);

			choices.getSelectionModel().select(choice);
			fireDialogButton(pane, ButtonType.OK);
		};
	}

	private static void fireDialogButton(final DialogPane pane, final ButtonType buttonType) {
		((Button) pane.lookupButton(buttonType)).fire();
	}

	private static DialogPane findShowingDialogPane() {
		return Window.getWindows().stream()
				.filter(Window::isShowing)
				.map(Window::getScene)
				.filter(scene -> scene != null && scene.getRoot() instanceof DialogPane)
				.map(scene -> (DialogPane) scene.getRoot())
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("No dialog is currently showing"));
	}

	/** {@link DockingLayoutRestorable} test double controlling every branch {@link LayoutsMenu} reacts to. */
	private static final class RecordingRestorable implements DockingLayoutRestorable {

		private final DockingLayoutPersistenceProvider persistenceProvider;
		private BentoProvider bentoProvider = new DefaultBentoProvider();
		private final List<Supplier<DockingLayout>> switchCalls = new ArrayList<>();
		private boolean switchSucceeds = true;

		private RecordingRestorable(final DockingLayoutPersistenceProvider persistenceProvider) {
			this.persistenceProvider = persistenceProvider;
		}

		@Override
		public DockingLayout getDefaultDockingLayout() {
			return new DockingLayoutBuilder().build();
		}

		@Override
		public DockingLayout getDockingLayout(
				final LayoutPersistenceProfile layoutPersistenceProfile,
				final Supplier<DockingLayout> fallbackLayoutSupplier
		) {
			return fallbackLayoutSupplier.get();
		}

		@Override
		public boolean switchToLayout(final Supplier<DockingLayout> dockingLayoutSupplier) {
			switchCalls.add(dockingLayoutSupplier);
			if (switchSucceeds) {
				@SuppressWarnings("unused")
				final DockingLayout unused = dockingLayoutSupplier.get();
			}
			return switchSucceeds;
		}

		@Override
		public DockingLayoutPersistenceProvider getPersistenceProvider() {
			return persistenceProvider;
		}

		@Override
		public BentoProvider getBentoProvider() {
			return bentoProvider;
		}
	}

	/** {@link DockingLayoutPersistenceProvider} test double; only the members {@link LayoutsMenu} calls do anything. */
	private static final class RecordingPersistenceProvider
			implements DockingLayoutPersistenceProvider,
			PersistedDockingLayoutOrganizationProvider {

		private final List<LayoutPersistenceProfile> storedLayouts = new ArrayList<>();
		private final Set<String> storedIdentifiers = new HashSet<>();
		private final List<LayoutPersistenceProfile> savedProfiles = new ArrayList<>();
		private final List<LayoutPersistenceProfile> deletedProfiles = new ArrayList<>();
		private final List<LayoutPersistenceProfile> renamedProfiles = new ArrayList<>();
		private final List<String> storedGroups = new ArrayList<>();
		private @Nullable String activeLayoutIdentifier;
		private @Nullable List<BentoState> defaultArrangement;
		private boolean isSessionStored = true;
		private boolean listFails;
		private boolean groupsFail;
		private boolean isLayoutStoredFails;
		private boolean saveFails;
		private boolean deleteFails;

		@Override
		public LayoutSaver getLayoutSaver(
				final LayoutPersistenceProfile layoutPersistenceProfile,
				final BentoProvider bentoProvider
		) {
			throw new UnsupportedOperationException();
		}

		@Override
		public LayoutRestorer getLayoutRestorer(
				final LayoutPersistenceProfile layoutPersistenceProfile,
				final BentoProvider bentoProvider,
				final DockableStateProvider dockableStateProvider,
				final @Nullable StageIconImageProvider stageIconImageProvider,
				final @Nullable DockContainerLeafMenuFactoryProvider dockContainerLeafMenuFactoryProvider
		) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void saveLayout(
				final LayoutPersistenceProfile layoutPersistenceProfile,
				final BentoProvider bentoProvider
		) throws BentoStateException {
			if (saveFails) {
				throw new BentoStateException("save failed");
			}
			savedProfiles.add(layoutPersistenceProfile);
		}

		@Override
		public List<String> getStoredLayoutIdentifiers(
				final LayoutPersistenceProfile layoutPersistenceProfile
		) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<LayoutPersistenceProfile> getStoredLayouts(
				final LayoutPersistenceProfile layoutPersistenceProfile
		) throws BentoStateException {
			if (listFails) {
				throw new BentoStateException("list failed");
			}
			return List.copyOf(storedLayouts);
		}

		@Override
		public boolean isLayoutStored(
				final LayoutPersistenceProfile layoutPersistenceProfile
		) throws BentoStateException {
			if (isLayoutStoredFails) {
				throw new BentoStateException("isLayoutStored failed");
			}
			if (SESSION_LAYOUT_IDENTIFIER.equals(layoutPersistenceProfile.layoutIdentifier())) {
				return isSessionStored;
			}
			return storedIdentifiers.contains(layoutPersistenceProfile.layoutIdentifier());
		}

		@Override
		public boolean deleteLayout(
				final LayoutPersistenceProfile layoutPersistenceProfile
		) throws BentoStateException {
			if (deleteFails) {
				throw new BentoStateException("delete failed");
			}
			deletedProfiles.add(layoutPersistenceProfile);
			return true;
		}

		/**
		 * Rewrites the naming of a layout in {@link #storedLayouts}, so that a
		 * test can assert on what the menu did by reading the list back the way
		 * a reopened menu would.
		 */
		@Override
		public boolean updateStoredLayoutNaming(
				final LayoutPersistenceProfile layoutPersistenceProfile
		) {
			renamedProfiles.add(layoutPersistenceProfile);

			for (int index = 0; index < storedLayouts.size(); index++) {
				final boolean isSameLayout = storedLayouts.get(index)
						.layoutIdentifier()
						.equals(layoutPersistenceProfile.layoutIdentifier());

				if (isSameLayout) {
					storedLayouts.set(index, layoutPersistenceProfile);
					return true;
				}
			}

			return false;
		}

		@Override
		public List<String> getStoredGroups(
				final LayoutPersistenceProfile layoutPersistenceProfile
		) throws BentoStateException {
			if (groupsFail) {
				throw new BentoStateException("groups failed");
			}
			return List.copyOf(storedGroups);
		}

		@Override
		public void setStoredGroups(
				final LayoutPersistenceProfile layoutPersistenceProfile,
				final List<String> groups
		) {
			storedGroups.clear();
			storedGroups.addAll(groups);
		}

		@Override
		public Optional<String> getActiveLayoutIdentifier(
				final LayoutPersistenceProfile layoutPersistenceProfile
		) {
			return Optional.ofNullable(activeLayoutIdentifier);
		}

		@Override
		public void setActiveLayoutIdentifier(
				final LayoutPersistenceProfile layoutPersistenceProfile,
				final @Nullable String layoutIdentifier
		) {
			activeLayoutIdentifier = layoutIdentifier;
		}

		@Override
		public Optional<List<BentoState>> getStoredBentoStates(
				final LayoutPersistenceProfile layoutPersistenceProfile
		) {
			return Optional.ofNullable(defaultArrangement);
		}
	}
}
