package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.DockingLayout;
import software.coley.bentofx.persistence.core.api.DockingLayout.DockingLayoutBuilder;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutRestorable;
import software.coley.bentofx.persistence.core.api.storage.LayoutIdentifierProblem;
import software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers;
import software.coley.bentofx.persistence.core.api.storage.LayoutNames;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import static software.coley.bentofx.persistence.core.ui.layoutsmenu.LayoutsMenuTextKeys.*;

/**
 * Layout CRUD operations: save, restore, rename, move to a group, delete,
 * and write a layout.
 *
 * <p>Confirmation and error-reporting callbacks are reached through
 * the {@link LayoutOperationsContext} passed at construction.</p>
 *
 * <p>Every method here that raises a dialog must be called on the FX Application
 * Thread. Methods that only read or write state without opening a dialog have no
 * thread requirement.</p>
 *
 * @author Phil Bryant
 */
class LayoutOperations {

	private static final Logger logger =
			LoggerFactory.getLogger(LayoutOperations.class);

	private final DockingLayoutRestorable dockingLayoutRestorable;
	private final Window owner;
	private final ResourceBundle resourceBundle;
	private final LayoutGroupOperations groupOperations;
	private final ActiveLayoutState activeLayoutState;
	private final DefaultArrangementState defaultArrangementState;
	private final LayoutOperationsContext context;

	/**
	 * @param dockingLayoutRestorable the application whose docking layout these
	 * operations switch, and whose providers they read and write it through.
	 * @param owner the window the dialogs raised here belong to.
	 * @param resourceBundle every word a user reads from the dialogs here.
	 * @param groupOperations handles the group-choice dialog shown during
	 * save-as-new and move-to-group.
	 * @param activeLayoutState tracks which named layout is currently active.
	 * @param defaultArrangementState tracks whether the default layout is
	 * currently showing.
	 * @param context supplies the confirmation and error-reporting callbacks
	 * this class cannot own itself.
	 */
	LayoutOperations(
			final DockingLayoutRestorable dockingLayoutRestorable,
			final Window owner,
			final ResourceBundle resourceBundle,
			final LayoutGroupOperations groupOperations,
			final ActiveLayoutState activeLayoutState,
			final DefaultArrangementState defaultArrangementState,
			final LayoutOperationsContext context
	) {
		this.dockingLayoutRestorable =
				Objects.requireNonNull(dockingLayoutRestorable, "dockingLayoutRestorable");
		this.owner = Objects.requireNonNull(owner, "owner");
		this.resourceBundle = Objects.requireNonNull(resourceBundle, "resourceBundle");
		this.groupOperations = Objects.requireNonNull(groupOperations, "groupOperations");
		this.activeLayoutState =
				Objects.requireNonNull(activeLayoutState, "activeLayoutState");
		this.defaultArrangementState =
				Objects.requireNonNull(defaultArrangementState, "defaultArrangementState");
		this.context = Objects.requireNonNull(context, "context");
	}

	/**
	 * Switches to the layout the application builds for itself.
	 *
	 * <p>FX Application Thread only.</p>
	 */
	void restoreDefaultLayout() {
		if (switchToLayout(
				dockingLayoutRestorable::getDefaultDockingLayout
		)) {
			activeLayoutState.setActiveCustomLayoutProfile(null);
			defaultArrangementState.recordDefaultArrangementLater();
		}
	}

	/**
	 * Switches to a layout in storage.
	 *
	 * <p>FX Application Thread only.</p>
	 *
	 * @param layoutPersistenceProfile identifies the layout to switch to. Comes
	 * from the catalog, so it already carries the codec and storage it was
	 * listed with.
	 */
	void restoreStoredLayout(
			final LayoutPersistenceProfile layoutPersistenceProfile
	) {
		final boolean isSwitched = switchToLayout(
				() -> dockingLayoutRestorable.getDockingLayout(
						layoutPersistenceProfile,

						// An empty layout rather than the default one. A read
						// that fails should leave what is on screen alone;
						// falling back here would answer "restore this" with a
						// different layout.
						() -> new DockingLayoutBuilder().build()
				)
		);

		if (isSwitched) {
			activeLayoutState.setActiveCustomLayoutProfile(layoutPersistenceProfile);
		}
	}

	/**
	 * Asks for a name, asks which group it belongs in, and saves the layout
	 * showing now under both.
	 *
	 * <p>The group is asked for only when there is a group to choose, so an
	 * application whose users never make one never sees a second dialog.</p>
	 *
	 * <p>FX Application Thread only.</p>
	 */
	void saveCurrentLayoutAsNew() {
		final Optional<String> displayName = findLayoutName(
				getTextFromResourceBundle(TITLE_SAVE_AS_NEW_DIALOG_KEY),
				getTextFromResourceBundle(PROMPT_SAVE_AS_NEW_DIALOG_KEY),
				""
		);

		if (displayName.isEmpty()) {
			return;
		}

		final String layoutIdentifier =
				LayoutNames.toIdentifier(displayName.get());

		// The name a user types is not an identifier, and deriving one is this
		// menu's step rather than the framework's. What the framework does is
		// say which rule a derived identifier breaks.
		final Optional<LayoutIdentifierProblem> problem =
				LayoutIdentifiers.findUserLayoutProblem(layoutIdentifier);

		if (problem.isPresent()) {
			context.showLayoutError(
					getTextFromResourceBundle(HEADER_CANNOT_SAVE_NAMED_ERROR_KEY, displayName.get()),
					problemText(problem.get())
			);
			return;
		}

		final Optional<List<String>> groupNames =
				StoredLayouts.findStoredCustomLayouts(persistenceProvider())
						.flatMap(layouts -> StoredLayouts.findGroupNames(persistenceProvider(), layouts));

		if (groupNames.isEmpty()) {
			context.showLayoutError(
					getTextFromResourceBundle(HEADER_LIST_GROUPS_FAILED_ERROR_KEY), null);
			return;
		}

		final String group;

		if (groupNames.get().isEmpty()) {
			group = null;
		} else {
			final Optional<String> choice = groupOperations.findGroupChoice(
					getTextFromResourceBundle(TITLE_SAVE_AS_NEW_DIALOG_KEY),
					getTextFromResourceBundle(GROUP_PROMPT_SAVE_AS_NEW_DIALOG_KEY),
					null,
					groupNames.get()
			);

			if (choice.isEmpty()) {
				return;
			}

			group = groupOperations.chosenGroup(choice.get());
		}

		final LayoutPersistenceProfile newLayout = newProfile(
				layoutIdentifier,
				displayName.get(),
				group
		);

		final boolean isAlreadyStored;

		try {
			isAlreadyStored = persistenceProvider().isLayoutStored(newLayout);
		} catch (final BentoStateException e) {
			logger.warn(
					"Could not tell whether the layout '{}' is stored.",
					layoutIdentifier,
					e
			);
			context.showLayoutError(
					getTextFromResourceBundle(HEADER_SAVE_FAILED_ERROR_KEY), e.getMessage());
			return;
		}

		// Two names can derive one identifier, so this catches a collision the
		// user cannot see coming as well as the same name typed twice.
		if (isAlreadyStored && !context.confirmLayoutAction(
				getTextFromResourceBundle(HEADER_REPLACE_CONFIRM_KEY, layoutIdentifier),
				getTextFromResourceBundle(CONTENT_REPLACE_CONFIRM_KEY)
		)) {
			return;
		}

		writeLayout(newLayout);
	}

	/**
	 * Saves the layout showing now over the stored layout it came from.
	 *
	 * <p>FX Application Thread only.</p>
	 */
	void saveChangesToActiveLayout() {
		final LayoutPersistenceProfile activeLayout =
				activeLayoutState.getActiveLayoutProfile();

		if (activeLayout == null) {
			return;
		}

		writeLayout(activeLayout);
	}

	/**
	 * Asks for a new name for a stored layout and stores it under that name.
	 *
	 * <p>The layout identifier does not change, so the layout keeps the place
	 * in storage it already had - a display name is stored with a layout, not
	 * what the layout is addressed by. Nor does the group, so renaming a layout
	 * does not move it.</p>
	 *
	 * <p>Only the name is written. A rename used to be a save as well, because
	 * nothing could rewrite the stored metadata on its own, which both restricted
	 * it to the layout on screen and meant renaming a layout quietly stored
	 * whatever arrangement happened to be showing.
	 * {@link DockingLayoutPersistenceProvider#updateStoredLayoutNaming} rewrites
	 * the name and leaves the docking state alone, so neither is true now.</p>
	 *
	 * <p>FX Application Thread only.</p>
	 *
	 * @param storedLayout identifies the layout to rename.
	 */
	void renameStoredLayout(
			final LayoutPersistenceProfile storedLayout
	) {
		final Optional<String> displayName = findLayoutName(
				getTextFromResourceBundle(TITLE_RENAME_DIALOG_KEY),
				getTextFromResourceBundle(PROMPT_RENAME_DIALOG_KEY),
				StoredLayouts.getLayoutLabel(storedLayout)
		);

		if (displayName.isEmpty()) {
			return;
		}

		if (displayName.get().isBlank()) {
			context.showLayoutError(
					getTextFromResourceBundle(HEADER_BLANK_NAME_ERROR_KEY),
					getTextFromResourceBundle(CONTENT_BLANK_NAME_ERROR_KEY)
			);
			return;
		}

		updateStoredLayoutNaming(
				storedLayout,
				displayName.get(),
				storedLayout.group()
		);
	}

	/**
	 * Asks which group a stored layout belongs in and moves it there.
	 *
	 * <p>Offered for any stored layout rather than only the one showing, and it
	 * rewrites nothing but the group - a layout does not have to be restored to be
	 * filed.</p>
	 *
	 * <p>FX Application Thread only.</p>
	 *
	 * @param storedLayout identifies the layout to move.
	 */
	void moveStoredLayoutToGroup(
			final LayoutPersistenceProfile storedLayout
	) {
		final Optional<List<String>> groupNames =
				StoredLayouts.findStoredCustomLayouts(persistenceProvider())
						.flatMap(layouts -> StoredLayouts.findGroupNames(persistenceProvider(), layouts));

		if (groupNames.isEmpty()) {
			context.showLayoutError(
					getTextFromResourceBundle(HEADER_LIST_GROUPS_FAILED_ERROR_KEY), null);
			return;
		}

		final Optional<String> choice = groupOperations.findGroupChoice(
				getTextFromResourceBundle(TITLE_MOVE_TO_GROUP_DIALOG_KEY),
				getTextFromResourceBundle(PROMPT_MOVE_TO_GROUP_DIALOG_KEY),
				storedLayout.group(),
				groupNames.get()
		);

		if (choice.isEmpty()) {
			return;
		}

		updateStoredLayoutNaming(
				storedLayout,
				storedLayout.displayName(),
				groupOperations.chosenGroup(choice.get())
		);
	}

	/**
	 * Removes a layout from storage.
	 *
	 * <p>The layout on screen is not changed, even when it is the one removed;
	 * what changes is that there is no longer anywhere to save it back to.</p>
	 *
	 * <p>FX Application Thread only.</p>
	 *
	 * @param layoutPersistenceProfile identifies the layout to remove.
	 */
	void deleteStoredLayout(
			final LayoutPersistenceProfile layoutPersistenceProfile
	) {
		final String layoutLabel = StoredLayouts.getLayoutLabel(layoutPersistenceProfile);

		if (!context.confirmLayoutAction(
				getTextFromResourceBundle(HEADER_DELETE_CONFIRM_KEY, layoutLabel),
				getTextFromResourceBundle(CONTENT_DELETE_CONFIRM_KEY)
		)) {
			return;
		}

		try {
			persistenceProvider().deleteLayout(layoutPersistenceProfile);
		} catch (final BentoStateException e) {
			logger.warn(
					"Could not delete the docking layout '{}'.",
					layoutPersistenceProfile.layoutIdentifier(),
					e
			);
			context.showLayoutError(
					getTextFromResourceBundle(HEADER_DELETE_FAILED_ERROR_KEY), e.getMessage());
			return;
		}

		if (activeLayoutState.isActiveLayout(layoutPersistenceProfile)) {
			activeLayoutState.setActiveCustomLayoutProfile(null);
		}
	}

	/**
	 * Writes the layout showing now to storage and makes it the active layout.
	 *
	 * <p>FX Application Thread only.</p>
	 *
	 * @param layoutPersistenceProfile names the layout to write.
	 */
	void writeLayout(
			final LayoutPersistenceProfile layoutPersistenceProfile
	) {
		try {
			// A one-shot write rather than through the running saver: this is a
			// layout of its own, not the session layout auto-save keeps up to
			// date.
			persistenceProvider().saveLayout(
					layoutPersistenceProfile,
					bentoProvider()
			);
			activeLayoutState.setActiveCustomLayoutProfile(layoutPersistenceProfile);
		} catch (final BentoStateException e) {
			logger.warn(
					"Could not save the docking layout as '{}'.",
					layoutPersistenceProfile.layoutIdentifier(),
					e
			);
			context.showLayoutError(
					getTextFromResourceBundle(HEADER_SAVE_FAILED_ERROR_KEY), e.getMessage());
		}
	}

	/**
	 * Stores a different display name and group for a layout already in storage.
	 *
	 * @param storedLayout identifies the layout to rewrite.
	 * @param displayName the name to store.
	 * @param group the group to store, or {@code null} for no group.
	 */
	private void updateStoredLayoutNaming(
			final LayoutPersistenceProfile storedLayout,
			final @Nullable String displayName,
			final @Nullable String group
	) {
		final LayoutPersistenceProfile renamedLayout =
				storedLayout.withNaming(displayName, group);

		try {
			if (persistenceProvider().updateStoredLayoutNaming(renamedLayout)) {
				activeLayoutState.followNamingChange(renamedLayout);
			} else {
				// Deleted from under the menu, which is rebuilt each time it
				// opens, so the next open shows the truth.
				context.showLayoutError(
						getTextFromResourceBundle(HEADER_NOT_STORED_ERROR_KEY), null);
			}
		} catch (final BentoStateException e) {
			logger.warn(
					"Could not rewrite the naming of the stored layout '{}'.",
					storedLayout.layoutIdentifier(),
					e
			);
			context.showLayoutError(
					getTextFromResourceBundle(HEADER_SAVE_FAILED_ERROR_KEY), e.getMessage());
		}
	}

	/**
	 * {@return the name the user typed, trimmed, or an empty {@link Optional}
	 * when the dialog was cancelled.}
	 *
	 * <p>FX Application Thread only, and blocks in a nested event loop until the
	 * dialog is dismissed.</p>
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
	 * {@return why a name a user typed cannot address a layout.}
	 *
	 * <p>Only the three rules below can be broken by an identifier that came
	 * out of {@link LayoutNames#toIdentifier(String)}, which keeps nothing but
	 * letters and digits; the rest are about characters that cannot survive it.
	 * The framework's sentence is still the default arm, because a rule added
	 * later would otherwise arrive here as nothing.</p>
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
	 * {@return a profile naming a layout of the application's own, leaving the
	 * codec and the storage to the framework's selection.}
	 *
	 * @param layoutIdentifier addresses the layout in storage.
	 * @param displayName the name to store with the layout.
	 * @param group the group to store with the layout, or {@code null} for no
	 * group.
	 */
	private static LayoutPersistenceProfile newProfile(
			final String layoutIdentifier,
			final String displayName,
			final @Nullable String group
	) {
		return LayoutPersistenceProfile.named(
				layoutIdentifier,
				displayName,
				null,
				null
		).withNaming(displayName, group);
	}

	/**
	 * {@return the provider layouts are read, written, and listed through.}
	 *
	 * <p>Asked of the application at each use rather than held here, so that
	 * this type cannot be the reason a stale provider stays reachable.</p>
	 */
	private DockingLayoutPersistenceProvider persistenceProvider() {
		return dockingLayoutRestorable.getPersistenceProvider();
	}

/**
	 * {@return the provider naming what a save captures.}
	 *
	 * @see #persistenceProvider()
	 */
	private BentoProvider bentoProvider() {
		return dockingLayoutRestorable.getBentoProvider();
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
	 * <p>Separate from {@link #getTextFromResourceBundle(String)} so that only
	 * the values holding a placeholder go through {@link MessageFormat}. Running
	 * every value through it would make a literal apostrophe an escape character
	 * in all of them, and so make every translated sentence a place to get that
	 * wrong.</p>
	 *
	 * @param key names the text in the resource bundle.
	 * @param argument what to put in place of <code>{0}</code>.
	 */
	private String getTextFromResourceBundle(final String key, final Object argument) {
		return MessageFormat.format(resourceBundle.getString(key), argument);
	}

	/**
	 * {@return {@code true} when the layout was applied; otherwise,
	 * {@code false}, having told the user that what is on screen was left as it
	 * is.}
	 *
	 * <p>FX Application Thread only.</p>
	 *
	 * @param dockingLayoutSupplier supplies the layout to switch to.
	 */
	private boolean switchToLayout(
			final Supplier<DockingLayout> dockingLayoutSupplier
	) {
		if (dockingLayoutRestorable.switchToLayout(dockingLayoutSupplier)) {
			return true;
		}

		context.showLayoutError(
				getTextFromResourceBundle(HEADER_RESTORE_FAILED_ERROR_KEY),
				getTextFromResourceBundle(CONTENT_RESTORE_FAILED_ERROR_KEY)
		);
		return false;
	}
}
