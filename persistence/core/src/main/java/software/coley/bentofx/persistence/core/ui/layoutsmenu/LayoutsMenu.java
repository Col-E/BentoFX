package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Window;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutRestorable;
import software.coley.bentofx.persistence.core.api.provider.PersistedDockingLayoutOrganizationProvider;
import software.coley.bentofx.persistence.core.api.storage.LayoutIdentifierProblem;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static software.coley.bentofx.persistence.core.ui.layoutsmenu.LayoutsMenuTextKeys.*;

/**
 * A {@code Layouts} menu for any application that keeps its docking layout in
 * BentoFX persistence: switch between the layout the application builds for
 * itself and the layouts a user has named, and save, rename, and delete those.
 *
 * <p>Nothing here is particular to one application. What an application
 * supplies is a {@link DockingLayoutRestorable} to act on and the window
 * dialogs belong to; the providers that reach storage come from the
 * restorable, which cannot be implemented without them anyway. Drop the menu
 * wherever it belongs - a menu bar, a {@code Window} menu, a context menu:</p>
 *
 * <pre>{@code
 * windowMenu.getItems().add(new LayoutsMenu(application, stage));
 * }</pre>
 *
 * <p>This menu owns which named layout is showing, reading and writing which
 * one through its persistence provider's optional
 * {@link PersistedDockingLayoutOrganizationProvider} support. A provider that does not
 * implement it still restores and saves layouts normally, but this menu
 * offers no group management and starts every launch with no named layout
 * active.</p>
 *
 * <p>Users organize their saved layouts into groups from this menu: create one,
 * rename one, move layouts in and out, and delete one without losing the layouts
 * that were in it. A group is stored as a field of its own. A group also exists
 * before there is anything in it.</p>
 *
 * <p>Every word a user reads comes from a {@link ResourceBundle}, so an
 * application in another language either drops a translation beside the one
 * this module provides or hands over a bundle of its own. See
 * {@code LayoutsMenu.properties}.</p>
 *
 * @author Phil Bryant
 */
public class LayoutsMenu extends Menu {

	private static final String CHECK_MARK = "✓";

	/**
	 * The bundle this menu reads when an application does not supply one.
	 *
	 * <p>Resolved against this module, which is where its own
	 * {@code LayoutsMenu.properties} lives. A bundle in the application's own
	 * module cannot be found by this name - resources in a named module are not
	 * visible to another - which is why the way to substitute text is to pass a
	 * {@link ResourceBundle} rather than to shadow this one.</p>
	 */
	private static final String BUNDLE_BASE_NAME =
			"software.coley.bentofx.persistence.core.ui.layoutsmenu.LayoutsMenu";

	private final DockingLayoutRestorable dockingLayoutRestorable;

	/** The window the dialogs raised from this menu belong to. */
	private final Window owner;

	/** Every word a user reads from this menu. */
	private final ResourceBundle resourceBundle;

	/**
	 * The four collaborators that drive this menu's behaviour, in one place.
	 * Replaces the four separate collaborator fields that existed before
	 * {@link LayoutsController} grouped them.
	 */
	private final LayoutsController controller;

	/**
	 * Builds a menu that reads the text this framework provides, in the
	 * default locale.
	 *
	 * @param owner the window the dialogs these items raise belong to.
	 * @param dockingLayoutRestorable the application whose docking layout these
	 * items switch, and whose providers they read and write it through.
	 */
	public LayoutsMenu(
			final Window owner,
			final DockingLayoutRestorable dockingLayoutRestorable
	) {
		this(owner, dockingLayoutRestorable, ResourceBundle.getBundle(BUNDLE_BASE_NAME));
	}

	/**
	 * Builds a menu around a pre-loaded resource bundle. Private because the
	 * public constructor's {@code super(...)} call must be its first statement,
	 * so the bundle has to be loaded before the body runs and then referenced
	 * both for the menu's own title and for every item it builds. Delegating
	 * through this constructor is the one way to share one load between the
	 * two without reading the bundle twice.
	 *
	 * @param owner the window the dialogs these items raise belong to.
	 * @param dockingLayoutRestorable the application whose docking layout these
	 * items switch, and whose providers they read and write it through.
	 * @param resourceBundle every word a user reads from this menu.
	 */
	private LayoutsMenu(
			final Window owner,
			final DockingLayoutRestorable dockingLayoutRestorable,
			final ResourceBundle resourceBundle
	) {
		super(resourceBundle.getString(LAYOUTS_MENU_KEY));

		this.dockingLayoutRestorable =
				Objects.requireNonNull(dockingLayoutRestorable, "dockingLayoutRestorable");
		this.owner = Objects.requireNonNull(owner, "owner");
		this.resourceBundle = resourceBundle;

		final ActiveLayoutState activeLayoutState =
				new ActiveLayoutState(dockingLayoutRestorable);
		final DefaultArrangementState defaultArrangementState =
				new DefaultArrangementState(dockingLayoutRestorable);
		final LayoutGroupOperations groupOperations = new LayoutGroupOperations(
				dockingLayoutRestorable,
				owner,
				resourceBundle,
				new LayoutGroupOperationsContext() {
					@Override
					public boolean confirmLayoutAction(
							final String header,
							final String content
					) {
						return LayoutsMenu.this.confirmLayoutAction(header, content);
					}

					@Override
					public void showLayoutError(
							final String header,
							final @Nullable String content
					) {
						LayoutsMenu.this.showLayoutError(header, content);
					}

					@Override
					public String problemText(final LayoutIdentifierProblem problem) {
						return LayoutsMenu.this.problemText(problem);
					}

					@Override
					public void populate() {
						LayoutsMenu.this.populate();
					}

					@Override
					public void followNamingChange(final LayoutPersistenceProfile renamedLayout) {
						activeLayoutState.followNamingChange(renamedLayout);
					}
				}
		);
		final LayoutOperations layoutOperations = new LayoutOperations(
				dockingLayoutRestorable,
				owner,
				resourceBundle,
				groupOperations,
				activeLayoutState,
				defaultArrangementState,
				new LayoutOperationsContext() {
					@Override
					public boolean confirmLayoutAction(
							final String header,
							final String content
					) {
						return LayoutsMenu.this.confirmLayoutAction(header, content);
					}

					@Override
					public void showLayoutError(
							final String header,
							final @Nullable String content
					) {
						LayoutsMenu.this.showLayoutError(header, content);
					}
				}
		);
		this.controller = new LayoutsController(
				groupOperations,
				activeLayoutState,
				defaultArrangementState,
				layoutOperations
		);

		wireMenu();
	}

	/**
	 * Wires the showing handler and fills the menu once so it opens with
	 * items. A menu with no items never opens, and so would never reach the
	 * handler that fills it. Rebuilding on every showing is what keeps the
	 * check marks and both layout lists in step with storage, which changes
	 * while the application runs.
	 */
	private void wireMenu() {
		setOnShowing(event -> populate());
		populate();
	}

	/**
	 * Fills this menu from the layout showing now and what is in storage.
	 */
	private void populate() {
		// Default is checked only when what is showing is arranged as the
		// default layout. Anything else is custom: a named layout, whose own item
		// is checked too, or an arrangement a user made without naming it, such
		// as the session layout the application starts with, which checks
		// Custom and nothing under it.
		final boolean isDefaultShowing =
				controller.activeLayoutState().getActiveLayoutProfile() == null && controller.defaultArrangementState().isDefaultLayoutShowing();

		final MenuItem defaultItem = new MenuItem(
				markedText(
						getTextFromResourceBundle(DEFAULT_ITEM_KEY),
						isDefaultShowing
				)
		);
		defaultItem.setOnAction(event -> controller.layoutOperations().restoreDefaultLayout());

		final Menu customMenu = new Menu(
				markedText(
						getTextFromResourceBundle(CUSTOM_MENU_KEY),
						!isDefaultShowing
				)
		);
		populateCustomMenu(customMenu);

		final MenuItem saveAsNewItem =
				new MenuItem(getTextFromResourceBundle(SAVE_AS_NEW_ITEM_KEY));
		saveAsNewItem.setOnAction(event -> controller.layoutOperations().saveCurrentLayoutAsNew());

		getItems().setAll(defaultItem, customMenu, saveAsNewItem);
	}

	/**
	 * Fills the {@code Custom} menu.
	 *
	 * <p>Saving changes is offered only for the layout showing now, because it
	 * writes the containers on screen. Renaming and moving to a group are offered
	 * for any stored layout: both change only what a layout is called, and
	 * {@link DockingLayoutPersistenceProvider#updateStoredLayoutNaming} does that
	 * without reading the scene graph.</p>
	 *
	 * @param customMenu the menu to fill.
	 */
	private void populateCustomMenu(final Menu customMenu) {
		final Menu restoreMenu = new Menu(getTextFromResourceBundle(RESTORE_MENU_KEY));
		final Menu renameMenu = new Menu(getTextFromResourceBundle(RENAME_MENU_KEY));
		final Menu moveToGroupMenu = new Menu(getTextFromResourceBundle(MOVE_TO_GROUP_MENU_KEY));
		final Menu deleteMenu = new Menu(getTextFromResourceBundle(DELETE_MENU_KEY));
		final Menu groupsMenu = new Menu(getTextFromResourceBundle(GROUPS_MENU_KEY));

		final MenuItem saveChangesItem = new MenuItem(getTextFromResourceBundle(SAVE_CHANGES_ITEM_KEY));
		saveChangesItem.setDisable(controller.activeLayoutState().getActiveLayoutProfile() == null);
		saveChangesItem.setOnAction(event -> controller.layoutOperations().saveChangesToActiveLayout());

		// One reading of storage fills every list below. Listing with display
		// names decodes every stored layout, so asking again would decode again.
		final Optional<List<LayoutPersistenceProfile>> storedLayouts =
				StoredLayouts.findStoredCustomLayouts(persistenceProvider());
		// flatMap, so that layouts which could not be listed give no groups
		// either: with no layouts to reconcile against, a catalog on its own
		// would report groups for a list of layouts this menu does not have.
		final Optional<List<String>> groupNames =
				storedLayouts.flatMap(layouts ->
						StoredLayouts.findGroupNames(persistenceProvider(), layouts));

		final List<Menu> layoutMenus =
				List.of(restoreMenu, renameMenu, moveToGroupMenu, deleteMenu);

		if (storedLayouts.isEmpty() || groupNames.isEmpty()) {
			layoutMenus.forEach(
					menu -> addDisabledItem(menu, getTextFromResourceBundle(LIST_FAILED_ITEM_KEY))
			);
			addDisabledItem(groupsMenu, getTextFromResourceBundle(LIST_FAILED_ITEM_KEY));
		} else {
			addLayoutItems(
					restoreMenu,
					storedLayouts.get(),
					groupNames.get(),
					true,
					controller.layoutOperations()::restoreStoredLayout
			);
			addLayoutItems(
					renameMenu,
					storedLayouts.get(),
					groupNames.get(),
					false,
					controller.layoutOperations()::renameStoredLayout
			);
			addLayoutItems(
					deleteMenu,
					storedLayouts.get(),
					groupNames.get(),
					false,
					controller.layoutOperations()::deleteStoredLayout
			);

			// Nothing to move a layout into until a group exists, and an item
			// that opens a dialog offering no choice is worse than one that says
			// so.
			if (groupNames.get().isEmpty()) {
				addDisabledItem(moveToGroupMenu, getTextFromResourceBundle(NO_GROUPS_ITEM_KEY));
			} else {
				addLayoutItems(
						moveToGroupMenu,
						storedLayouts.get(),
						groupNames.get(),
						false,
						controller.layoutOperations()::moveStoredLayoutToGroup
				);
			}

			populateGroupsMenu(groupsMenu, storedLayouts.get(), groupNames.get());
		}

		customMenu.getItems().setAll(
				restoreMenu,
				saveChangesItem,
				renameMenu,
				moveToGroupMenu,
				deleteMenu,
				groupsMenu
		);
	}

	/**
	 * Fills the {@code Groups} menu, which acts on the groups themselves rather
	 * than on any layout.
	 *
	 * <p>FX Application Thread only.</p>
	 *
	 * @param groupsMenu the menu to fill.
	 * @param storedLayouts the layouts a rename or a delete has to carry along.
	 * @param groupNames the groups that exist.
	 */
	private void populateGroupsMenu(
			final Menu groupsMenu,
			final List<LayoutPersistenceProfile> storedLayouts,
			final List<String> groupNames
	) {
		final MenuItem newGroupItem = new MenuItem(getTextFromResourceBundle(NEW_GROUP_ITEM_KEY));
		newGroupItem.setOnAction(event -> controller.groupOperations().createGroup(groupNames));

		final Menu renameGroupMenu = new Menu(getTextFromResourceBundle(RENAME_GROUP_MENU_KEY));
		final Menu deleteGroupMenu = new Menu(getTextFromResourceBundle(DELETE_GROUP_MENU_KEY));

		if (groupNames.isEmpty()) {
			addDisabledItem(renameGroupMenu, getTextFromResourceBundle(NO_GROUPS_ITEM_KEY));
			addDisabledItem(deleteGroupMenu, getTextFromResourceBundle(NO_GROUPS_ITEM_KEY));
		} else {
			for (final String groupName : groupNames) {
				final MenuItem renameItem = layoutItem(groupName);
				renameItem.setOnAction(event ->
						controller.groupOperations().renameGroup(groupName, groupNames, storedLayouts)
				);
				renameGroupMenu.getItems().add(renameItem);

				final MenuItem deleteItem = layoutItem(groupName);
				deleteItem.setOnAction(event ->
						controller.groupOperations().deleteGroup(groupName, storedLayouts)
				);
				deleteGroupMenu.getItems().add(deleteItem);
			}
		}

		groupsMenu.getItems().setAll(
				newGroupItem,
				renameGroupMenu,
				deleteGroupMenu
		);
	}

	/**
	 * Fills a menu with one item per stored layout, nesting the layouts that
	 * belong to a group into a submenu for it.
	 *
	 * <p>Mnemonic parsing is off because a group name is a name a user typed.</p>
	 *
	 * <p>FX Application Thread only, like everything that builds these items.</p>
	 *
	 * @param targetMenu the menu to fill.
	 * @param storedLayouts the layouts to list, in label order.
	 * @param groupNames the groups that exist, in the order to show them.
	 * @param marksActiveLayout whether to mark the layout showing now, which
	 * suits a menu that switches layouts and misleads on one that deletes them.
	 * @param action what to do with the layout an item names.
	 */
	private void addLayoutItems(
			final Menu targetMenu,
			final List<LayoutPersistenceProfile> storedLayouts,
			final List<String> groupNames,
			final boolean marksActiveLayout,
			final Consumer<LayoutPersistenceProfile> action
	) {
		LayoutGroups.groupLayouts(groupNames, storedLayouts)
				.forEach((groupName, groupLayouts) -> targetMenu.getItems().add(
						groupMenu(
								groupName,
								groupLayouts,
								marksActiveLayout,
								action
						)
				));

		for (final LayoutPersistenceProfile ungroupedLayout :
				LayoutGroups.ungroupedLayouts(groupNames, storedLayouts)) {

			targetMenu.getItems().add(
					createLayoutItem(ungroupedLayout, marksActiveLayout, action)
			);
		}

		// An empty menu opens as an empty popup, which reads as a fault rather
		// than as an answer. Reached when nothing is stored and no group exists.
		if (targetMenu.getItems().isEmpty()) {
			addDisabledItem(targetMenu, getTextFromResourceBundle(NO_LAYOUTS_ITEM_KEY));
		}
	}

	/**
	 * {@return one group's submenu, holding an item per layout in it.}
	 *
	 * <p>FX Application Thread only.</p>
	 *
	 * @param groupName the group to build for.
	 * @param groupLayouts the layouts in it, which may be none.
	 * @param marksActiveLayout whether to mark the layout showing now.
	 * @param action what to do with the layout an item names.
	 */
	private Menu groupMenu(
			final String groupName,
			final List<LayoutPersistenceProfile> groupLayouts,
			final boolean marksActiveLayout,
			final Consumer<LayoutPersistenceProfile> action
	) {
		final Menu groupMenu = new Menu(markedText(
				groupName,
				marksActiveLayout
						&& groupLayouts.stream().anyMatch(controller.activeLayoutState()::isActiveLayout)
		));
		groupMenu.setMnemonicParsing(false);

		if (groupLayouts.isEmpty()) {
			addDisabledItem(groupMenu, getTextFromResourceBundle(NO_LAYOUTS_ITEM_KEY));

			return groupMenu;
		}

		for (final LayoutPersistenceProfile groupLayout : groupLayouts) {
			groupMenu.getItems().add(
					createLayoutItem(groupLayout, marksActiveLayout, action)
			);
		}

		return groupMenu;
	}

	/**
	 * {@return an item naming one stored layout, which carries out the supplied
	 * action on it.}
	 *
	 * <p>FX Application Thread only.</p>
	 *
	 * @param storedLayout the layout the item names.
	 * @param marksActiveLayout whether to mark the layout showing now.
	 * @param action what to do with the layout when the item is chosen.
	 */
	private MenuItem createLayoutItem(
			final LayoutPersistenceProfile storedLayout,
			final boolean marksActiveLayout,
			final Consumer<LayoutPersistenceProfile> action
	) {
		final MenuItem item = layoutItem(markedText(
				StoredLayouts.getLayoutLabel(storedLayout),
				marksActiveLayout && controller.activeLayoutState().isActiveLayout(storedLayout)
		));
		item.setOnAction(event -> action.accept(storedLayout));

		return item;
	}

	/**
	 * {@return the provider layouts are read, written, and listed through.}
	 *
	 * <p>Asked of the application at each use rather than held here, so that
	 * this menu cannot be the reason a stale provider stays reachable.</p>
	 */
	private DockingLayoutPersistenceProvider persistenceProvider() {
		return dockingLayoutRestorable.getPersistenceProvider();
	}

/**
	 * {@return an item naming one stored layout.}
	 *
	 * <p>Mnemonic parsing off, because the label is a name a user typed rather
	 * than one this framework wrote. Left on, an underscore in that name would
	 * be eaten as a mnemonic marker and the layout a user called
	 * {@code My_Layout} would be listed as {@code MyLayout}. There is nothing
	 * to lose by it: these items are as many as the user has saved layouts, so
	 * no mnemonic could be assigned to them ahead of time anyway.</p>
	 *
	 * @param label what the item says.
	 */
	private static MenuItem layoutItem(final String label) {
		final MenuItem item = new MenuItem(label);
		item.setMnemonicParsing(false);
		return item;
	}

	/**
	 * {@return the text for a key.}
	 *
	 * @param key names the text in this menu's {@link ResourceBundle}.
	 */
	private String getTextFromResourceBundle(final String key) {
		return resourceBundle.getString(key);
	}

	/**
	 * {@return why a name a user typed cannot address a layout.}
	 *
	 * <p>Rendered here rather than shown as {@link
	 * LayoutIdentifierProblem#message()}, which is the framework's own sentence
	 * and comes in one language. Only the three rules below can be broken by an
	 * identifier derived from a display name (letters and digits only); the rest
	 * are about characters that cannot survive that derivation. The framework's
	 * sentence is still the default arm, because a rule added later would
	 * otherwise arrive here as nothing.</p>
	 *
	 * @param problem which rule the derived identifier broke.
	 */
	private String problemText(final LayoutIdentifierProblem problem) {
		return switch (problem.rule()) {
			case BLANK -> getTextFromResourceBundle(BLANK_PROBLEM_KEY);
			case RESERVED -> getTextFromResourceBundle(RESERVED_PROBLEM_KEY);
			case DEVICE_NAME -> getTextFromResourceBundle(DEVICE_NAME_PROBLEM_KEY);
			default -> problem.message();
		};
	}

	/**
	 * {@return what a menu item says, marked when it is the one in effect.}
	 *
	 * <p>The mark goes in the item's own text rather than into a {@code Label}
	 * set as the item's graphic. A theme styles the text fill of a menu item's
	 * own label for each state it can be in, and a graphic is not that label,
	 * so a mark put there came out in a fill that only stood out from the
	 * background while the item was under the pointer.</p>
	 *
	 * <p>Nothing at all when unmarked, rather than a blank standing in for the
	 * mark. The graphic slot this replaces was a fixed width, so a stand-in
	 * lined the items up in a column; a leading space is not the width of the
	 * mark, so it only shifted the unmarked items instead.</p>
	 *
	 * @param text what the item says.
	 * @param isChecked whether the item is the one in effect.
	 */
	private static String markedText(
			final String text,
			final boolean isChecked
	) {
		return isChecked ? CHECK_MARK + " " + text : text;
	}

	/**
	 * Adds one disabled item to a menu, so that it has something to show.
	 *
	 * <p>An empty menu opens as an empty popup, which reads as a fault rather
	 * than as an answer.</p>
	 *
	 * <p>Mnemonic parsing off for the same reason as on a stored layout's item:
	 * nothing can be navigated to on a disabled item, so an underscore a
	 * translation happened to contain would be eaten for nothing.</p>
	 *
	 * @param menu the menu to add to.
	 * @param text what the item says.
	 */
	private static void addDisabledItem(final Menu menu, final String text) {
		final MenuItem item = new MenuItem(text);
		item.setMnemonicParsing(false);
		item.setDisable(true);
		menu.getItems().add(item);
	}

	/**
	 * Tells the user that something about a layout could not be done.
	 *
	 * @param header what could not be done.
	 * @param content why, in whatever detail is available.
	 */
	private void showLayoutError(
			final String header,
			final @Nullable String content
	) {
		final Alert alert = new Alert(Alert.AlertType.ERROR);

		alert.initOwner(owner);
		alert.setTitle(getTextFromResourceBundle(TITLE_DIALOG_KEY));
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
	}

	/**
	 * {@return {@code true} when the user agreed; otherwise, {@code false}.}
	 *
	 * <p>Defaults to no, so that dismissing the dialog does not carry out
	 * something that cannot be undone.</p>
	 *
	 * @param header what is being asked.
	 * @param content what it means.
	 */
	private boolean confirmLayoutAction(
			final String header,
			final String content
	) {
		final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

		alert.initOwner(owner);
		alert.setTitle(getTextFromResourceBundle(TITLE_DIALOG_KEY));
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

		return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
	}
}
