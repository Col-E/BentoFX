package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutRestorable;
import software.coley.bentofx.persistence.core.api.provider.PersistedDockingLayoutOrganizationProvider;
import software.coley.bentofx.persistence.core.ui.layoutsmenu.LayoutGroups.GroupNameProblem;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

import static software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers.SESSION_LAYOUT_IDENTIFIER;
import static software.coley.bentofx.persistence.core.ui.layoutsmenu.LayoutsMenuTextKeys.*;

/**
 * Group CRUD and catalog operations: creating, renaming, deleting, and moving
 * layouts between groups, plus the underlying catalog reads and writes.
 *
 * <p>Confirmation, error-reporting, and post-operation callbacks that this
 * class cannot own — {@code confirmLayoutAction}, {@code showLayoutError},
 * {@code problemText}, {@code populate}, {@code followNamingChange} — are
 * reached through the {@link LayoutGroupOperationsContext} passed at
 * construction.</p>
 *
 * @author Phil Bryant
 */
class LayoutGroupOperations {

	private static final Logger logger =
			LoggerFactory.getLogger(LayoutGroupOperations.class);

	private final DockingLayoutRestorable dockingLayoutRestorable;
	private final Window owner;
	private final ResourceBundle resourceBundle;
	private final LayoutGroupOperationsContext context;

	/**
	 * @param dockingLayoutRestorable the application whose persistence providers
	 * storage operations go through.
	 * @param owner the window the dialogs raised here belong to.
	 * @param resourceBundle every word a user reads from the dialogs here.
	 * @param context supplies the confirmation, error-reporting, and naming
	 * callbacks this class cannot own itself.
	 */
	LayoutGroupOperations(
			final DockingLayoutRestorable dockingLayoutRestorable,
			final Window owner,
			final ResourceBundle resourceBundle,
			final LayoutGroupOperationsContext context
	) {
		this.dockingLayoutRestorable =
				Objects.requireNonNull(dockingLayoutRestorable, "dockingLayoutRestorable");
		this.owner = Objects.requireNonNull(owner, "owner");
		this.resourceBundle = Objects.requireNonNull(resourceBundle, "resourceBundle");
		this.context = Objects.requireNonNull(context, "context");
	}

	/**
	 * {@return the group a user picked, or an empty {@link Optional} when the
	 * dialog was dismissed.}
	 *
	 * <p>What comes back is the label that was picked, which is
	 * {@code choice.noGroup} when the user chose to leave the layout in no group -
	 * put it through {@link #chosenGroup(String)} to get the group to store.</p>
	 *
	 * <p>A {@link ChoiceDialog} rather than a submenu of groups, because the menu
	 * item this opens from already names a layout and an item cannot both act and
	 * hold a submenu.</p>
	 *
	 * <p>FX Application Thread only, and blocks in a nested event loop until the
	 * dialog is dismissed.</p>
	 *
	 * @param title the dialog's title.
	 * @param prompt what to ask for.
	 * @param currentGroup the group to start on, or {@code null} to start on no
	 * group.
	 * @param groupNames the groups to choose between.
	 */
	Optional<String> findGroupChoice(
			final String title,
			final String prompt,
			final @Nullable String currentGroup,
			final List<String> groupNames
	) {
		final String noGroupChoice = getTextFromResourceBundle(NO_GROUP_CHOICE_KEY);
		final List<String> choices = new ArrayList<>();
		choices.add(noGroupChoice);
		choices.addAll(groupNames);

		// Starting on a group that is not offered would show the dialog with a
		// selection the list does not hold, so an unknown group starts on none.
		// Matching is case-insensitive to agree with LayoutGroups' CASE_INSENSITIVE_ORDER
		// ordering, and the starting choice is taken from the list rather than
		// from currentGroup so its spelling matches an entry ChoiceDialog
		// recognizes.
		final String startingChoice =
				currentGroup == null
						? noGroupChoice
						: choices.stream()
								.filter(choice -> choice.equalsIgnoreCase(currentGroup))
								.findFirst()
								.orElse(noGroupChoice);

		final ChoiceDialog<String> dialog =
				new ChoiceDialog<>(startingChoice, choices);

		dialog.initOwner(owner);
		dialog.setTitle(title);
		dialog.setHeaderText(null);
		dialog.setContentText(prompt);

		return dialog.showAndWait();
	}

	/**
	 * {@return the group to store for a label a user picked, which is
	 * {@code null} when they picked no group.}
	 *
	 * @param choice the label {@link #findGroupChoice} returned.
	 */
	@Nullable String chosenGroup(final String choice) {
		return choice.equals(getTextFromResourceBundle(NO_GROUP_CHOICE_KEY)) ? null : choice;
	}

	/**
	 * Asks for a name and adds a group under it.
	 *
	 * <p>The group is written to the catalog and holds nothing, which is what
	 * lets it be created before there is a layout to put in it.</p>
	 *
	 * @param groupNames the groups that already exist, which a new name may not
	 * repeat.
	 */
	void createGroup(final List<String> groupNames) {
		final Optional<String> groupName = findLayoutName(
				getTextFromResourceBundle(TITLE_NEW_GROUP_DIALOG_KEY),
				getTextFromResourceBundle(PROMPT_NEW_GROUP_DIALOG_KEY),
				""
		);

		if (groupName.isEmpty() || !isUsableGroupName(
				groupName.get(),
				groupNames,
				null
		)) {
			return;
		}

		// Read the catalog rather than reusing the merged list: merging folds in
		// the groups the layouts name, and writing those back would quietly
		// promote a group into the catalog that nobody asked to create.
		writeGroupCatalog(catalog -> {
			final List<String> updated = new ArrayList<>(catalog);
			updated.add(groupName.get().trim());
			return updated;
		}, getTextFromResourceBundle(HEADER_GROUP_FAILED_ERROR_KEY));
	}

	/**
	 * Asks for a new name for a group and renames it, along with every layout in
	 * it.
	 *
	 * <p>The catalog is rewritten first and the layouts after. Either order can be
	 * interrupted, and neither loses a layout: the group a layout records and the
	 * catalog are read together as a union, so a layout left behind under the old
	 * name still appears - under that name - rather than vanishing.</p>
	 *
	 * @param groupName the group to rename.
	 * @param groupNames the groups that exist.
	 * @param storedLayouts the layouts to carry across.
	 */
	void renameGroup(
			final String groupName,
			final List<String> groupNames,
			final List<LayoutPersistenceProfile> storedLayouts
	) {
		final Optional<String> newGroupName = findLayoutName(
				getTextFromResourceBundle(TITLE_RENAME_GROUP_DIALOG_KEY),
				getTextFromResourceBundle(PROMPT_RENAME_GROUP_DIALOG_KEY),
				groupName
		);

		if (newGroupName.isEmpty() || !isUsableGroupName(
				newGroupName.get(),
				groupNames,
				groupName
		)) {
			return;
		}

		final String trimmedName = newGroupName.get().trim();

		final boolean catalogWritten = writeGroupCatalog(
				catalog -> catalog.stream()
						.map(stored -> stored.equalsIgnoreCase(groupName)
								? trimmedName
								: stored)
						.toList(),
				getTextFromResourceBundle(HEADER_RENAME_GROUP_FAILED_ERROR_KEY)
		);

		if (catalogWritten) {
			moveGroupMembers(
					groupName,
					trimmedName,
					storedLayouts,
					getTextFromResourceBundle(HEADER_RENAME_GROUP_FAILED_ERROR_KEY)
			);
		}
	}

	/**
	 * Confirms, then removes a group and takes the layouts in it out of it.
	 *
	 * <p>The layouts are not deleted. They are moved out of the group first and
	 * the catalog is rewritten after, so an interrupted delete leaves a group that
	 * is empty rather than one that appears to hold layouts no longer in it.</p>
	 *
	 * @param groupName the group to remove.
	 * @param storedLayouts the layouts to take out of it.
	 */
	void deleteGroup(
			final String groupName,
			final List<LayoutPersistenceProfile> storedLayouts
	) {
		if (!context.confirmLayoutAction(
				getTextFromResourceBundle(HEADER_DELETE_GROUP_CONFIRM_KEY, groupName),
				getTextFromResourceBundle(CONTENT_DELETE_GROUP_CONFIRM_KEY)
		)) {
			return;
		}

		final boolean membersMoved = moveGroupMembers(
				groupName,
				null,
				storedLayouts,
				getTextFromResourceBundle(HEADER_DELETE_GROUP_FAILED_ERROR_KEY)
		);

		if (membersMoved) {
			writeGroupCatalog(
					catalog -> catalog.stream()
							.filter(stored -> !stored.equalsIgnoreCase(groupName))
							.toList(),
					getTextFromResourceBundle(HEADER_DELETE_GROUP_FAILED_ERROR_KEY)
			);
		}
	}

	/**
	 * Moves every layout in a group to another group, or out of any group.
	 *
	 * @param groupName the group whose layouts to move.
	 * @param newGroupName the group to move them to, or {@code null} to take them
	 * out of any group.
	 * @param storedLayouts the layouts to look through.
	 * @param errorHeader what to tell the user when a layout cannot be written.
	 * @return {@code true} when every layout in the group was moved; otherwise,
	 * {@code false}, the user having been told.
	 */
	private boolean moveGroupMembers(
			final String groupName,
			final @Nullable String newGroupName,
			final List<LayoutPersistenceProfile> storedLayouts,
			final String errorHeader
	) {
		for (final LayoutPersistenceProfile storedLayout : storedLayouts) {
			final String layoutGroup = storedLayout.group();

			if (layoutGroup == null || !layoutGroup.equalsIgnoreCase(groupName)) {
				continue;
			}

			final LayoutPersistenceProfile movedLayout = storedLayout.withNaming(
					storedLayout.displayName(),
					newGroupName
			);

			try {
				if (persistenceProvider().updateStoredLayoutNaming(movedLayout)) {
					context.followNamingChange(movedLayout);
				}
			} catch (final BentoStateException e) {
				logger.warn(
						"Could not move the stored layout '{}' out of the group "
								+ "'{}'.",
						storedLayout.layoutIdentifier(),
						groupName,
						e
				);
				context.showLayoutError(errorHeader, e.getMessage());
				return false;
			}
		}

		return true;
	}

	/**
	 * Reads the group catalog, changes it, and writes it back.
	 *
	 * @param change what the catalog should become, given what it holds now.
	 * @param errorHeader what to tell the user when it cannot be read or written,
	 * or when the persistence provider has no catalog to write to.
	 * @return {@code true} when the catalog was written; otherwise, {@code false},
	 * the user having been told.
	 */
	private boolean writeGroupCatalog(
			final UnaryOperator<List<String>> change,
			final String errorHeader
	) {
		final Optional<PersistedDockingLayoutOrganizationProvider> extendedProvider =
				extendedPersistenceProvider();

		if (extendedProvider.isEmpty()) {
			context.showLayoutError(errorHeader, null);
			return false;
		}

		final PersistedDockingLayoutOrganizationProvider provider = extendedProvider.get();
		final LayoutPersistenceProfile storageProfile =
				LayoutPersistenceProfile.of(SESSION_LAYOUT_IDENTIFIER);

		try {
			provider.setStoredGroups(
					storageProfile,
					change.apply(provider.getStoredGroups(storageProfile))
			);
			return true;
		} catch (final BentoStateException e) {
			logger.warn("Could not write the stored layout groups.", e);
			context.showLayoutError(errorHeader, e.getMessage());
			return false;
		}
	}

	/**
	 * {@return {@code true} when the name can be a group; otherwise,
	 * {@code false}, the user having been told why.}
	 *
	 * @param groupName the name the user typed.
	 * @param groupNames the groups that exist.
	 * @param renamedGroup the group being renamed, or {@code null} when one is
	 * being created.
	 */
	private boolean isUsableGroupName(
			final String groupName,
			final List<String> groupNames,
			final @Nullable String renamedGroup
	) {
		final Optional<GroupNameProblem> problem =
				LayoutGroups.findGroupNameProblem(
						groupName,
						groupNames,
						renamedGroup
				);

		if (problem.isEmpty()) {
			return true;
		}

		context.showLayoutError(
				getTextFromResourceBundle(HEADER_CANNOT_NAME_GROUP_ERROR_KEY, groupName),
				switch (problem.get()) {
					case BLANK -> getTextFromResourceBundle(BLANK_GROUP_PROBLEM_KEY);
					case TOO_LONG -> getTextFromResourceBundle(
							GROUP_TOO_LONG_PROBLEM_KEY,
							LayoutGroups.MAX_GROUP_NAME_LENGTH
					);
					case DUPLICATE -> getTextFromResourceBundle(DUPLICATE_GROUP_PROBLEM_KEY);
				}
		);

		return false;
	}

	/**
	 * {@return the name the user typed, trimmed, or an empty {@link Optional}
	 * when the dialog was cancelled.}
	 *
	 * @param title the dialog's title.
	 * @param prompt what to ask for.
	 * @param initialName what the field starts with.
	 */
	private Optional<String> findLayoutName(
			final String title,
			final String prompt,
			final String initialName
	) {
		final TextInputDialog dialog = new TextInputDialog(initialName);

		dialog.initOwner(owner);
		dialog.setTitle(title);
		dialog.setHeaderText(null);
		dialog.setContentText(prompt);

		return dialog.showAndWait().map(String::trim);
	}

	/**
	 * {@return the provider layouts are read, written, and listed through.}
	 */
	private DockingLayoutPersistenceProvider persistenceProvider() {
		return dockingLayoutRestorable.getPersistenceProvider();
	}

	/**
	 * {@return {@link #persistenceProvider()}, narrowed to
	 * {@link PersistedDockingLayoutOrganizationProvider}, or an empty {@link Optional} when
	 * it does not implement that too.}
	 */
	private Optional<PersistedDockingLayoutOrganizationProvider> extendedPersistenceProvider() {
		return persistenceProvider() instanceof PersistedDockingLayoutOrganizationProvider extendedProvider
				? Optional.of(extendedProvider)
				: Optional.empty();
	}

	/**
	 * {@return the text for a key.}
	 *
	 * @param key names the text in the resource bundle.
	 */
	private String getTextFromResourceBundle(final String key) {
		return resourceBundle.getString(key);
	}

	/**
	 * {@return the text for a key, with its placeholder filled in.}
	 *
	 * <p>Separate from {@link #getTextFromResourceBundle(String)} so that only the values holding a
	 * placeholder go through {@link MessageFormat}. Running every value through
	 * it would make a literal apostrophe an escape character in all of
	 * them, and so make every translated sentence a place to get that
	 * wrong.</p>
	 *
	 * @param key names the text in the resource bundle.
	 * @param argument what to put in place of <code>{0}</code>.
	 */
	private String getTextFromResourceBundle(final String key, final Object argument) {
		return MessageFormat.format(resourceBundle.getString(key), argument);
	}
}
