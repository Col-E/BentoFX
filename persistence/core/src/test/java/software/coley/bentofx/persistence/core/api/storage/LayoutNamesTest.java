package software.coley.bentofx.persistence.core.api.storage;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.storage.LayoutIdentifierProblem.Rule;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Coverage for turning a name a user typed into a layout identifier.
 *
 * @author Phil Bryant
 */
class LayoutNamesTest {

	private static final int MAX_LAYOUT_IDENTIFIER_LENGTH = 64;

	@Test
	void derivesAnIdentifierFromWhatAUserWouldType() {
		assertThat(LayoutNames.toIdentifier("Multi-Monitor"))
				.describedAs("a hyphenated name")
				.isEqualTo("multi-monitor");

		assertThat(LayoutNames.toIdentifier("Compact"))
				.describedAs("a one-word name")
				.isEqualTo("compact");

		assertThat(LayoutNames.toIdentifier("Wide Editor 2"))
				.describedAs("a name with spaces and a digit")
				.isEqualTo("wide-editor-2");

		assertThat(LayoutNames.toIdentifier("  Debugging  "))
				.describedAs("a name padded with spaces")
				.isEqualTo("debugging");

		assertThat(LayoutNames.toIdentifier("Wide   Editor"))
				.describedAs("a run of separators between words")
				.isEqualTo("wide-editor");
	}

	@Test
	void reducesANameWithNothingUsableInItToNothing() {
		assertThat(LayoutNames.toIdentifier("!!!"))
				.describedAs("a name of punctuation only")
				.isEmpty();

		assertThat(LayoutNames.toIdentifier("   "))
				.describedAs("a name of spaces only")
				.isEmpty();
	}

	@Test
	void boundsTheIdentifierWithoutLeavingASeparatorAtTheEnd() {
		// Cut mid-punctuation: the characters at the boundary all become the
		// separator, and stripping happens after the cut, so none survives.
		final String longName = "a".repeat(MAX_LAYOUT_IDENTIFIER_LENGTH - 1)
				+ "!!!!!!!!!!" + "b".repeat(20);

		final String layoutIdentifier = LayoutNames.toIdentifier(longName);

		assertThat(layoutIdentifier)
				.describedAs("identifier derived from an over-long name")
				.hasSizeLessThanOrEqualTo(MAX_LAYOUT_IDENTIFIER_LENGTH)
				.doesNotEndWith("-")
				.doesNotStartWith("-");
	}

	/**
	 * The derivation's contract: whatever it returns, the only complaints the
	 * framework can still have are about what the name means rather than about
	 * the characters in it.
	 */
	@Test
	void derivedIdentifiersOnlyEverBreakRulesAboutMeaning() {

		// Rules about the characters in an identifier. A derivation that keeps
		// only letters and digits cannot produce a name that breaks one, so any
		// of these appearing means the derivation let something through.
		final Set<Rule> characterRules = EnumSet.of(
				Rule.MISSING,
				Rule.PATH,
				Rule.DIRECTORY,
				Rule.FORBIDDEN_CHARACTER,
				Rule.TRAILING_SPACE_OR_PERIOD,
				Rule.TOO_LONG
		);

		final List<String> displayNames = List.of(
				"Multi-Monitor",
				"C:\\Work\\Compact?",
				"Wide/Editor",
				"Debugging.",
				"Debugging ",
				".",
				"..",
				"<Compact>",
				"Wide|Editor",
				"\"Quoted\"",
				"!!!",
				"Session",
				"session",
				"NUL",
				"com1.txt",
				"Проект 12"
		);

		for (final String displayName : displayNames) {
			final Optional<Rule> rule = LayoutIdentifiers
					.findUserLayoutProblem(
							LayoutNames.toIdentifier(displayName)
					)
					.map(LayoutIdentifierProblem::rule);

			assertThat(rule)
					.describedAs(
							"rule broken by the identifier derived from '%s'",
							displayName
					)
					.isNotIn(characterRules.stream()
							.map(Optional::of)
							.toList());
		}
	}

	@Test
	void leavesTheNamesOnlyValidationCanRefuse() {
		assertThat(findProblemRule("Session"))
				.describedAs("a name reducing to the reserved identifier")
				.contains(Rule.RESERVED);

		assertThat(findProblemRule("NUL"))
				.describedAs("a name reducing to a device name")
				.contains(Rule.DEVICE_NAME);

		assertThat(findProblemRule("!!!"))
				.describedAs("a name reducing to nothing")
				.contains(Rule.BLANK);

		assertThat(findProblemRule("Multi-Monitor"))
				.describedAs("a name a user may take")
				.isEmpty();
	}

	/**
	 * {@return the rule the identifier derived from the name breaks, or an
	 * empty {@link Optional} when it breaks none.}
	 *
	 * @param displayName the name to derive an identifier from.
	 */
	private static Optional<Rule> findProblemRule(
			final String displayName
	) {
		return LayoutIdentifiers
				.findUserLayoutProblem(LayoutNames.toIdentifier(displayName))
				.map(LayoutIdentifierProblem::rule);
	}

	/**
	 * A utility class with only static members has no reason to be
	 * instantiated; the private constructor exists to say so rather than to
	 * silently allow it.
	 */
	@Test
	void utilityClassConstructorThrowsUnsupportedOperationException() throws Exception {
		final Constructor<LayoutNames> constructor =
				LayoutNames.class.getDeclaredConstructor();
		constructor.setAccessible(true);

		assertThatThrownBy(constructor::newInstance)
				.describedAs("reflective instantiation of the utility class")
				.isInstanceOf(InvocationTargetException.class)
				.cause()
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessage("Utility classes should not be instantiated.");
	}
}
