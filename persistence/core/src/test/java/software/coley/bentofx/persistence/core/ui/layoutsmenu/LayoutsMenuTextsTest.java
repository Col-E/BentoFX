package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import org.junit.jupiter.api.Test;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage for the text {@code LayoutsMenu} reads.
 *
 * <p>The menu itself needs the JavaFX toolkit and shows modal dialogs, so it is
 * not what these tests reach. What they reach is the one part of it that fails
 * at runtime rather than at compile time: a key the bundle does not carry
 * raises {@link java.util.MissingResourceException} the first time the menu
 * opens, and a placeholder a translator quoted or dropped shows the pattern
 * instead of the layout name.</p>
 *
 * @author Phil Bryant
 */
class LayoutsMenuTextsTest {

	/**
	 * Duplicated from {@code LayoutsMenu}, deliberately: the constant there is
	 * private, and a test that read it from the class would load a JavaFX
	 * control. Moving the bundle without changing this fails the first test
	 * below, which is the intended way to find out.
	 */
	private static final String BUNDLE_BASE_NAME =
			"software.coley.bentofx.persistence.core.ui.layoutsmenu.LayoutsMenu";

	/**
	 * Every key the menu asks for. Kept here as a list rather than derived,
	 * because deriving it from the bundle would assert the bundle against
	 * itself.
	 */
	private static final List<String> REQUIRED_KEYS = List.of(
			"menu.layouts",
			"menu.custom",
			"menu.restore",
			"menu.rename",
			"menu.moveToGroup",
			"menu.delete",
			"menu.groups",
			"menu.renameGroup",
			"menu.deleteGroup",
			"item.default",
			"item.saveAsNew",
			"item.saveChanges",
			"item.newGroup",
			"item.listFailed",
			"item.noLayouts",
			"item.noGroups",
			"choice.noGroup",
			"dialog.title",
			"dialog.saveAsNew.title",
			"dialog.saveAsNew.prompt",
			"dialog.saveAsNew.groupPrompt",
			"dialog.rename.title",
			"dialog.rename.prompt",
			"dialog.moveToGroup.title",
			"dialog.moveToGroup.prompt",
			"dialog.newGroup.title",
			"dialog.newGroup.prompt",
			"dialog.renameGroup.title",
			"dialog.renameGroup.prompt",
			"error.restoreFailed.header",
			"error.restoreFailed.content",
			"error.saveFailed.header",
			"error.deleteFailed.header",
			"error.blankName.header",
			"error.blankName.content",
			"error.cannotSaveNamed.header",
			"error.notStored.header",
			"error.listGroupsFailed.header",
			"error.groupFailed.header",
			"error.renameGroupFailed.header",
			"error.deleteGroupFailed.header",
			"error.cannotNameGroup.header",
			"confirm.replace.header",
			"confirm.replace.content",
			"confirm.delete.header",
			"confirm.delete.content",
			"confirm.deleteGroup.header",
			"confirm.deleteGroup.content",
			"problem.blank",
			"problem.reserved",
			"problem.deviceName",
			"problem.blankGroup",
			"problem.groupTooLong",
			"problem.duplicateGroup"
	);

	/**
	 * The keys shown on a disabled item, which carries no mnemonic because
	 * nothing can navigate to it.
	 */
	private static final List<String> STAND_IN_KEYS = List.of(
			"item.listFailed",
			"item.noLayouts",
			"item.noGroups"
	);

	/** The keys whose values are read as {@link MessageFormat} patterns. */
	private static final List<String> PLACEHOLDER_KEYS = List.of(
			"error.cannotSaveNamed.header",
			"error.cannotNameGroup.header",
			"confirm.replace.header",
			"confirm.delete.header",
			"confirm.deleteGroup.header",
			"problem.groupTooLong"
	);

	@Test
	void carriesEveryKeyTheMenuAsksFor() {
		assertThat(bundle().keySet())
				.describedAs("keys in %s", BUNDLE_BASE_NAME)
				.containsExactlyInAnyOrderElementsOf(REQUIRED_KEYS);
	}

	@Test
	void saysSomethingForEveryKey() {
		final ResourceBundle texts = bundle();

		for (final String key : REQUIRED_KEYS) {
			assertThat(texts.getString(key))
					.describedAs("text for '%s'", key)
					.isNotBlank();
		}
	}

	/**
	 * A pattern whose placeholder a translator quoted - {@code '{0}'} - or
	 * dropped still loads, still formats, and shows the user the pattern or a
	 * sentence with the layout name missing from it.
	 */
	@Test
	void substitutesTheLayoutNameIntoEveryPatternThatTakesOne() {
		final ResourceBundle texts = bundle();
		final String layoutName = "Multi-Monitor";

		for (final String key : PLACEHOLDER_KEYS) {
			final String pattern = texts.getString(key);

			assertThat(MessageFormat.format(pattern, layoutName))
					.describedAs("'%s' formatted from \"%s\"", key, pattern)
					.contains(layoutName)
					.doesNotContain("{0}");
		}
	}

	/**
	 * The mnemonic marker is part of the text a translator owns, so it lives in
	 * the bundle rather than in the code that builds the items.
	 */
	@Test
	void marksAMnemonicOnEveryMenuAndItem() {
		final ResourceBundle texts = bundle();

		for (final String key : REQUIRED_KEYS) {
			if (!key.startsWith("menu.") && !key.startsWith("item.")) {
				continue;
			}

			// The stand-ins are shown on disabled items that nothing can
			// navigate to, so a mnemonic on one would go nowhere.
			if (STAND_IN_KEYS.contains(key)) {
				continue;
			}

			assertThat(texts.getString(key))
					.describedAs("mnemonic in '%s'", key)
					.contains("_");
		}
	}

	/**
	 * {@return the bundle the menu reads, failing when it cannot be found.}
	 */
	private static ResourceBundle bundle() {
		return ResourceBundle.getBundle(BUNDLE_BASE_NAME);
	}
}
