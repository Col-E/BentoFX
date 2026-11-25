package software.coley.bentofx.persistence.core.ui.layoutsmenu;

/**
 * The {@code LayoutsMenu.properties} resource bundle keys.
 *
 * @author Phil Bryant
 */
final class LayoutsMenuTextKeys {

	private LayoutsMenuTextKeys() {
		throw new UnsupportedOperationException(
				"Utility classes should not be instantiated."
		);
	}

	static final String LAYOUTS_MENU_KEY = "menu.layouts";
	static final String DEFAULT_ITEM_KEY = "item.default";
	static final String CUSTOM_MENU_KEY = "menu.custom";
	static final String SAVE_AS_NEW_ITEM_KEY = "item.saveAsNew";
	static final String RESTORE_MENU_KEY = "menu.restore";
	static final String RENAME_MENU_KEY = "menu.rename";
	static final String MOVE_TO_GROUP_MENU_KEY = "menu.moveToGroup";
	static final String DELETE_MENU_KEY = "menu.delete";
	static final String GROUPS_MENU_KEY = "menu.groups";
	static final String SAVE_CHANGES_ITEM_KEY = "item.saveChanges";
	static final String LIST_FAILED_ITEM_KEY = "item.listFailed";
	static final String NO_GROUPS_ITEM_KEY = "item.noGroups";
	static final String NEW_GROUP_ITEM_KEY = "item.newGroup";
	static final String RENAME_GROUP_MENU_KEY = "menu.renameGroup";
	static final String DELETE_GROUP_MENU_KEY = "menu.deleteGroup";
	static final String NO_LAYOUTS_ITEM_KEY = "item.noLayouts";

	static final String TITLE_DIALOG_KEY = "dialog.title";
	static final String TITLE_SAVE_AS_NEW_DIALOG_KEY = "dialog.saveAsNew.title";
	static final String PROMPT_SAVE_AS_NEW_DIALOG_KEY = "dialog.saveAsNew.prompt";
	static final String GROUP_PROMPT_SAVE_AS_NEW_DIALOG_KEY = "dialog.saveAsNew.groupPrompt";
	static final String TITLE_RENAME_DIALOG_KEY = "dialog.rename.title";
	static final String PROMPT_RENAME_DIALOG_KEY = "dialog.rename.prompt";
	static final String TITLE_MOVE_TO_GROUP_DIALOG_KEY = "dialog.moveToGroup.title";
	static final String PROMPT_MOVE_TO_GROUP_DIALOG_KEY = "dialog.moveToGroup.prompt";
	static final String TITLE_NEW_GROUP_DIALOG_KEY = "dialog.newGroup.title";
	static final String PROMPT_NEW_GROUP_DIALOG_KEY = "dialog.newGroup.prompt";
	static final String TITLE_RENAME_GROUP_DIALOG_KEY = "dialog.renameGroup.title";
	static final String PROMPT_RENAME_GROUP_DIALOG_KEY = "dialog.renameGroup.prompt";
	static final String NO_GROUP_CHOICE_KEY = "choice.noGroup";

	static final String HEADER_RESTORE_FAILED_ERROR_KEY = "error.restoreFailed.header";
	static final String CONTENT_RESTORE_FAILED_ERROR_KEY = "error.restoreFailed.content";
	static final String HEADER_CANNOT_SAVE_NAMED_ERROR_KEY = "error.cannotSaveNamed.header";
	static final String HEADER_LIST_GROUPS_FAILED_ERROR_KEY = "error.listGroupsFailed.header";
	static final String HEADER_SAVE_FAILED_ERROR_KEY = "error.saveFailed.header";
	static final String HEADER_BLANK_NAME_ERROR_KEY = "error.blankName.header";
	static final String CONTENT_BLANK_NAME_ERROR_KEY = "error.blankName.content";
	static final String HEADER_GROUP_FAILED_ERROR_KEY = "error.groupFailed.header";
	static final String HEADER_RENAME_GROUP_FAILED_ERROR_KEY = "error.renameGroupFailed.header";
	static final String HEADER_DELETE_GROUP_FAILED_ERROR_KEY = "error.deleteGroupFailed.header";
	static final String HEADER_CANNOT_NAME_GROUP_ERROR_KEY = "error.cannotNameGroup.header";
	static final String HEADER_DELETE_FAILED_ERROR_KEY = "error.deleteFailed.header";
	static final String HEADER_NOT_STORED_ERROR_KEY = "error.notStored.header";

	static final String HEADER_REPLACE_CONFIRM_KEY = "confirm.replace.header";
	static final String CONTENT_REPLACE_CONFIRM_KEY = "confirm.replace.content";
	static final String HEADER_DELETE_GROUP_CONFIRM_KEY = "confirm.deleteGroup.header";
	static final String CONTENT_DELETE_GROUP_CONFIRM_KEY = "confirm.deleteGroup.content";
	static final String HEADER_DELETE_CONFIRM_KEY = "confirm.delete.header";
	static final String CONTENT_DELETE_CONFIRM_KEY = "confirm.delete.content";

	static final String BLANK_PROBLEM_KEY = "problem.blank";
	static final String RESERVED_PROBLEM_KEY = "problem.reserved";
	static final String DEVICE_NAME_PROBLEM_KEY = "problem.deviceName";
	static final String BLANK_GROUP_PROBLEM_KEY = "problem.blankGroup";
	static final String GROUP_TOO_LONG_PROBLEM_KEY = "problem.groupTooLong";
	static final String DUPLICATE_GROUP_PROBLEM_KEY = "problem.duplicateGroup";
}
