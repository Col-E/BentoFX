package software.coley.bentofx.persistence.core.api.storage;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.storage.LayoutIdentifierProblem.Parameter;
import software.coley.bentofx.persistence.core.api.storage.LayoutIdentifierProblem.Rule;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.*;
import static software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers.MAX_JOINED_LENGTH;

class LayoutIdentifiersTest {

	private static final String LAYOUT_IDENTIFIER = "main-layout";
	private static final String CODEC_IDENTIFIER = "json";

	@Test
	void acceptsAnOrdinaryPair() {
		assertThatCode(() ->
				LayoutIdentifiers.requireValid(LAYOUT_IDENTIFIER, CODEC_IDENTIFIER)
		)
				.describedAs("an ordinary pair")
				.doesNotThrowAnyException();
	}

	@Test
	void acceptsACodecIdentifierWrittenAsAnExtension() {
		assertThatCode(() ->
				LayoutIdentifiers.requireValid(LAYOUT_IDENTIFIER, ".json")
		)
				.describedAs("a codec identifier with a leading dot")
				.doesNotThrowAnyException();
	}

	@Test
	void acceptsANameBeginningWithAPeriod() {
		assertThatCode(() ->
				LayoutIdentifiers.requireValid(".hidden-layout", CODEC_IDENTIFIER)
		)
				.describedAs("a layout identifier beginning with a period")
				.doesNotThrowAnyException();
	}

	@Test
	void requiresALayoutIdentifier() {
		assertThatNullPointerException()
				.describedAs("null layout identifier")
				.isThrownBy(() -> LayoutIdentifiers.requireValid(null, CODEC_IDENTIFIER))
				.withMessage("layoutIdentifier");
	}

	@Test
	void requiresACodecIdentifier() {
		assertThatNullPointerException()
				.describedAs("null codec identifier")
				.isThrownBy(() -> LayoutIdentifiers.requireValid(LAYOUT_IDENTIFIER, null))
				.withMessage("codecIdentifier");
	}

	@Test
	void rejectsABlankLayoutIdentifier() {
		assertThatIllegalArgumentException()
				.describedAs("blank layout identifier")
				.isThrownBy(() -> LayoutIdentifiers.requireValid("  ", CODEC_IDENTIFIER))
				.withMessageContaining("layoutIdentifier")
				.withMessageContaining("blank");
	}

	@Test
	void rejectsAPathSeparatorInEitherIdentifier() {
		assertThatIllegalArgumentException()
				.describedAs("layout identifier containing a forward slash")
				.isThrownBy(() -> LayoutIdentifiers.requireValid("nested/layout", CODEC_IDENTIFIER))
				.withMessageContaining("layoutIdentifier");

		assertThatIllegalArgumentException()
				.describedAs("codec identifier containing a backslash")
				.isThrownBy(() -> LayoutIdentifiers.requireValid(LAYOUT_IDENTIFIER, "json\\x"))
				.withMessageContaining("codecIdentifier");
	}

	@Test
	void rejectsAnIdentifierThatNamesADirectory() {
		assertThatIllegalArgumentException()
				.describedAs("layout identifier that is a parent directory")
				.isThrownBy(() -> LayoutIdentifiers.requireValid("..", CODEC_IDENTIFIER))
				.withMessageContaining("layoutIdentifier");
	}

	@Test
	void rejectsALayoutIdentifierReservedForADevice() {
		assertThatIllegalArgumentException()
				.describedAs("layout identifier naming a device")
				.isThrownBy(() -> LayoutIdentifiers.requireValid("nul", CODEC_IDENTIFIER))
				.withMessageContaining("device");

		assertThatIllegalArgumentException()
				.describedAs("device name ahead of a suffix")
				.isThrownBy(() -> LayoutIdentifiers.requireValid("COM1.old", CODEC_IDENTIFIER))
				.withMessageContaining("device");
	}

	@Test
	void rejectsALayoutIdentifierReservedForASuperscriptDevice() {
		// Built from a code point rather than written as a literal so that the
		// test does not depend on the encoding this source is read with, which is
		// also why the names it checks are escapes.
		final String superscriptDevice = "COM" + (char) 0x00B9;

		assertThatIllegalArgumentException()
				.describedAs("layout identifier naming a superscript device")
				.isThrownBy(() ->
						LayoutIdentifiers.requireValid(superscriptDevice, CODEC_IDENTIFIER)
				)
				.withMessageContaining("device");
	}

	@Test
	void rejectsEveryCharacterNoFileNameMayHold() {
		for (final char forbidden : "<>:\"|?*".toCharArray()) {
			assertThatIllegalArgumentException()
					.describedAs("layout identifier containing " + forbidden)
					.isThrownBy(() ->
							LayoutIdentifiers.requireValid(
									"main" + forbidden + "layout",
									CODEC_IDENTIFIER
							)
					)
					.withMessageContaining("layoutIdentifier");
		}
	}

	@Test
	void rejectsAControlCharacter() {
		assertThatIllegalArgumentException()
				.describedAs("codec identifier containing a zero byte")
				.isThrownBy(() ->
						LayoutIdentifiers.requireValid(
								LAYOUT_IDENTIFIER,
								"js" + (char) 0 + "on"
						)
				)
				.withMessageContaining("code 0");

		assertThatIllegalArgumentException()
				.describedAs("layout identifier containing a tab")
				.isThrownBy(() ->
						LayoutIdentifiers.requireValid("main\tlayout", CODEC_IDENTIFIER)
				)
				.withMessageContaining("code 9");
	}

	@Test
	void rejectsANameEndingWithASpaceOrPeriod() {
		assertThatIllegalArgumentException()
				.describedAs("layout identifier ending with a space")
				.isThrownBy(() ->
						LayoutIdentifiers.requireValid("main-layout ", CODEC_IDENTIFIER)
				)
				.withMessageContaining("space or a period");

		assertThatIllegalArgumentException()
				.describedAs("codec identifier ending with a period")
				.isThrownBy(() ->
						LayoutIdentifiers.requireValid(LAYOUT_IDENTIFIER, "json.")
				)
				.withMessageContaining("space or a period");
	}

	@Test
	void acceptsTheSessionLayoutIdentifierAsValid() {
		assertThatCode(() ->
				LayoutIdentifiers.requireValid(
						LayoutIdentifiers.SESSION_LAYOUT_IDENTIFIER,
						CODEC_IDENTIFIER
				)
		)
				.describedAs("the session layout identifier")
				.doesNotThrowAnyException();
	}

	@Test
	void reportsTheSessionLayoutIdentifierAsReservedWhateverTheCase() {
		assertThat(LayoutIdentifiers.isReserved(
				LayoutIdentifiers.SESSION_LAYOUT_IDENTIFIER
		))
				.describedAs("isReserved for the session layout identifier")
				.isTrue();
		assertThat(LayoutIdentifiers.isReserved("SeSsIoN"))
				.describedAs("isReserved for the session layout identifier in mixed case")
				.isTrue();
		assertThat(LayoutIdentifiers.isReserved(LAYOUT_IDENTIFIER))
				.describedAs("isReserved for an ordinary layout identifier")
				.isFalse();
	}

	/**
	 * The group catalog is stored as an ordinary layout entry, so it turns up in
	 * any listing of what a destination holds. Reserving its identifier is what
	 * keeps a caller from offering it as a layout to restore, and keeps a user
	 * from taking the name for one of their own.
	 */
	@Test
	void reportsTheGroupCatalogIdentifierAsReservedWhateverTheCase() {
		assertThat(LayoutIdentifiers.isReserved(
				LayoutIdentifiers.GROUP_CATALOG_LAYOUT_IDENTIFIER
		))
				.describedAs("isReserved for the group catalog identifier")
				.isTrue();
		assertThat(LayoutIdentifiers.isReserved("GrOuPs"))
				.describedAs("isReserved for the group catalog identifier in mixed case")
				.isTrue();
	}

	@Test
	void refusesAUserLayoutNamedAfterTheGroupCatalog() {
		assertThat(LayoutIdentifiers.findUserLayoutProblem(
				LayoutIdentifiers.GROUP_CATALOG_LAYOUT_IDENTIFIER
		))
				.describedAs("findUserLayoutProblem for the group catalog identifier")
				.isPresent();
	}

	@Test
	void findProblemReportsNothingForAUsablePair() {
		assertThat(LayoutIdentifiers.findProblem(LAYOUT_IDENTIFIER, CODEC_IDENTIFIER))
				.describedAs("findProblem for an ordinary pair")
				.isEmpty();
	}

	@Test
	void findProblemNamesTheRuleAndTheParameter() {
		assertThat(LayoutIdentifiers.findProblem("  ", CODEC_IDENTIFIER))
				.describedAs("findProblem for a blank layout identifier")
				.get()
				.extracting(LayoutIdentifierProblem::rule, LayoutIdentifierProblem::parameter)
				.containsExactly(Rule.BLANK, Parameter.LAYOUT_IDENTIFIER);

		assertThat(LayoutIdentifiers.findProblem(LAYOUT_IDENTIFIER, "json/x"))
				.describedAs("findProblem for a codec identifier holding a separator")
				.get()
				.extracting(LayoutIdentifierProblem::rule, LayoutIdentifierProblem::parameter)
				.containsExactly(Rule.PATH, Parameter.CODEC_IDENTIFIER);

		assertThat(LayoutIdentifiers.findProblem("nul", CODEC_IDENTIFIER))
				.describedAs("findProblem for a layout identifier naming a device")
				.get()
				.extracting(LayoutIdentifierProblem::rule)
				.isEqualTo(Rule.DEVICE_NAME);

		assertThat(LayoutIdentifiers.findProblem("main-layout.", CODEC_IDENTIFIER))
				.describedAs("findProblem for a layout identifier ending with a period")
				.get()
				.extracting(LayoutIdentifierProblem::rule)
				.isEqualTo(Rule.TRAILING_SPACE_OR_PERIOD);
	}

	@Test
	void findProblemReportsThePairWhenTheRuleIsAboutBothTogether() {
		final String tooLong =
				"a".repeat(MAX_JOINED_LENGTH - CODEC_IDENTIFIER.length());

		assertThat(LayoutIdentifiers.findProblem(tooLong, CODEC_IDENTIFIER))
				.describedAs("findProblem for a pair that is too long together")
				.get()
				.extracting(LayoutIdentifierProblem::rule, LayoutIdentifierProblem::parameter)
				.containsExactly(Rule.TOO_LONG, Parameter.BOTH);
	}

	@Test
	void findProblemReportsAMissingIdentifierRatherThanThrowing() {
		assertThat(LayoutIdentifiers.findProblem(null, CODEC_IDENTIFIER))
				.describedAs("findProblem for a null layout identifier")
				.get()
				.extracting(LayoutIdentifierProblem::rule, LayoutIdentifierProblem::parameter)
				.containsExactly(Rule.MISSING, Parameter.LAYOUT_IDENTIFIER);

		assertThat(LayoutIdentifiers.findProblem(LAYOUT_IDENTIFIER, null))
				.describedAs("findProblem for a null codec identifier")
				.get()
				.extracting(LayoutIdentifierProblem::rule, LayoutIdentifierProblem::parameter)
				.containsExactly(Rule.MISSING, Parameter.CODEC_IDENTIFIER);
	}

	@Test
	void findProblemAndRequireValidCannotDisagree() {
		final String blank = "  ";

		final LayoutIdentifierProblem problem =
				LayoutIdentifiers.findProblem(blank, CODEC_IDENTIFIER).orElseThrow();

		assertThatIllegalArgumentException()
				.describedAs("what requireValid throws for the same pair")
				.isThrownBy(() -> LayoutIdentifiers.requireValid(blank, CODEC_IDENTIFIER))
				.withMessage(problem.message());
	}

	@Test
	void onlyTheUserLayoutCheckReportsAReservedIdentifier() {
		final String reserved = LayoutIdentifiers.SESSION_LAYOUT_IDENTIFIER;

		assertThat(LayoutIdentifiers.findProblem(reserved, CODEC_IDENTIFIER))
				.describedAs("findProblem for the session layout identifier")
				.isEmpty();

		assertThat(LayoutIdentifiers.findUserLayoutProblem(reserved, CODEC_IDENTIFIER))
				.describedAs("findUserLayoutProblem for the session layout identifier")
				.get()
				.extracting(LayoutIdentifierProblem::rule, LayoutIdentifierProblem::parameter)
				.containsExactly(Rule.RESERVED, Parameter.LAYOUT_IDENTIFIER);

		assertThat(LayoutIdentifiers.findUserLayoutProblem("SeSsIoN", CODEC_IDENTIFIER))
				.describedAs("findUserLayoutProblem for the reserved identifier in mixed case")
				.get()
				.extracting(LayoutIdentifierProblem::rule)
				.isEqualTo(Rule.RESERVED);

		assertThat(LayoutIdentifiers.findUserLayoutProblem(LAYOUT_IDENTIFIER, CODEC_IDENTIFIER))
				.describedAs("findUserLayoutProblem for a name a user may take")
				.isEmpty();
	}

	@Test
	void theUserLayoutCheckReportsAnUnusableNameBeforeAReservedOne() {
		assertThat(LayoutIdentifiers.findUserLayoutProblem("  ", CODEC_IDENTIFIER))
				.describedAs("findUserLayoutProblem for a blank name")
				.get()
				.extracting(LayoutIdentifierProblem::rule)
				.isEqualTo(Rule.BLANK);
	}

	@Test
	void rejectsAPairThatIsTooLongTogetherEvenWhenEachHalfFits() {
		// Each half is inside the limit; joined with the separator they are one
		// character over it, which is the case a per-identifier check misses.
		final String longLayoutIdentifier =
				"a".repeat(MAX_JOINED_LENGTH - CODEC_IDENTIFIER.length());

		assertThatCode(() ->
				LayoutIdentifiers.requireValid(
						longLayoutIdentifier.substring(1),
						CODEC_IDENTIFIER
				)
		)
				.describedAs("a pair that exactly fits")
				.doesNotThrowAnyException();

		assertThatIllegalArgumentException()
				.describedAs("a pair one character too long together")
				.isThrownBy(() ->
						LayoutIdentifiers.requireValid(longLayoutIdentifier, CODEC_IDENTIFIER)
				)
				.withMessageContaining(String.valueOf(MAX_JOINED_LENGTH));
	}

	@Test
	void theLayoutOnlyUserCheckReportsEveryRuleThatOneIdentifierCanBreak() {
		assertThat(LayoutIdentifiers.findUserLayoutProblem(LAYOUT_IDENTIFIER))
				.describedAs("a name a user may take")
				.isEmpty();

		assertThat(LayoutIdentifiers.findUserLayoutProblem(null))
				.describedAs("a missing name")
				.get()
				.extracting(LayoutIdentifierProblem::rule)
				.isEqualTo(Rule.MISSING);

		assertThat(LayoutIdentifiers.findUserLayoutProblem("  "))
				.describedAs("a blank name")
				.get()
				.extracting(LayoutIdentifierProblem::rule)
				.isEqualTo(Rule.BLANK);

		assertThat(LayoutIdentifiers.findUserLayoutProblem("a/b"))
				.describedAs("a name holding a path separator")
				.get()
				.extracting(LayoutIdentifierProblem::rule)
				.isEqualTo(Rule.PATH);

		assertThat(LayoutIdentifiers.findUserLayoutProblem(".."))
				.describedAs("a name naming a directory")
				.get()
				.extracting(LayoutIdentifierProblem::rule)
				.isEqualTo(Rule.DIRECTORY);

		assertThat(LayoutIdentifiers.findUserLayoutProblem("a?b"))
				.describedAs("a name holding a forbidden character")
				.get()
				.extracting(LayoutIdentifierProblem::rule)
				.isEqualTo(Rule.FORBIDDEN_CHARACTER);

		assertThat(LayoutIdentifiers.findUserLayoutProblem("layout."))
				.describedAs("a name ending in a period")
				.get()
				.extracting(LayoutIdentifierProblem::rule)
				.isEqualTo(Rule.TRAILING_SPACE_OR_PERIOD);

		assertThat(LayoutIdentifiers.findUserLayoutProblem("NUL"))
				.describedAs("a name Windows resolves to a device")
				.get()
				.extracting(LayoutIdentifierProblem::rule)
				.isEqualTo(Rule.DEVICE_NAME);

		assertThat(LayoutIdentifiers.findUserLayoutProblem("SeSsIoN"))
				.describedAs("the reserved identifier in mixed case")
				.get()
				.extracting(
						LayoutIdentifierProblem::rule,
						LayoutIdentifierProblem::parameter
				)
				.containsExactly(Rule.RESERVED, Parameter.LAYOUT_IDENTIFIER);
	}

	@Test
	void theLayoutOnlyUserCheckCannotReportALengthItCannotMeasure() {
		// Long enough that adding any codec identifier puts the pair over the
		// limit, which is the one rule this overload leaves to the storage
		// boundary because it needs both halves to judge.
		final String longLayoutIdentifier = "a".repeat(MAX_JOINED_LENGTH);

		assertThat(LayoutIdentifiers.findUserLayoutProblem(
				longLayoutIdentifier
		))
				.describedAs("a name that only a joined length would refuse")
				.isEmpty();

		assertThat(LayoutIdentifiers.findUserLayoutProblem(
				longLayoutIdentifier,
				CODEC_IDENTIFIER
		))
				.describedAs("the same name checked against a codec identifier")
				.get()
				.extracting(LayoutIdentifierProblem::rule)
				.isEqualTo(Rule.TOO_LONG);
	}

	@Test
	void requireValidLayoutIdentifierAcceptsAnOrdinaryIdentifier() {
		assertThatCode(() ->
				LayoutIdentifiers.requireValidLayoutIdentifier(LAYOUT_IDENTIFIER)
		)
				.describedAs("an ordinary layout identifier")
				.doesNotThrowAnyException();
	}

	@Test
	void requireValidLayoutIdentifierRejectsNull() {
		assertThatNullPointerException()
				.describedAs("null layout identifier")
				.isThrownBy(() -> LayoutIdentifiers.requireValidLayoutIdentifier(null))
				.withMessage("layoutIdentifier");
	}

	@Test
	void requireValidLayoutIdentifierRejectsBlank() {
		assertThatIllegalArgumentException()
				.describedAs("blank layout identifier")
				.isThrownBy(() -> LayoutIdentifiers.requireValidLayoutIdentifier(" "))
				.withMessageContaining("blank");
	}

	@Test
	void requireValidLayoutIdentifierRejectsDeviceName() {
		assertThatIllegalArgumentException()
				.describedAs("device name as layout identifier")
				.isThrownBy(() -> LayoutIdentifiers.requireValidLayoutIdentifier("CON"))
				.withMessageContaining("device");
	}

	@Test
	void requireValidLayoutIdentifierAcceptsFrameworkReservedIdentifier() {
		// 'session', 'groups', 'activeLayout', 'defaultLayout' pass this check
		// because it skips the reserved-name rule; that rule is for
		// user-chosen identifiers via 'findUserLayoutProblem'.
		assertThatCode(() ->
				LayoutIdentifiers.requireValidLayoutIdentifier("session")
		)
				.describedAs("framework-reserved identifier 'session'")
				.doesNotThrowAnyException();
	}

	/**
	 * A utility class with only static members has no reason to be
	 * instantiated; the private constructor exists to say so rather than to
	 * silently allow it.
	 */
	@Test
	void utilityClassConstructorThrowsIllegalStateException() throws Exception {
		final Constructor<LayoutIdentifiers> constructor =
				LayoutIdentifiers.class.getDeclaredConstructor();
		constructor.setAccessible(true);

		assertThatThrownBy(constructor::newInstance)
				.describedAs("reflective instantiation of the utility class")
				.isInstanceOf(InvocationTargetException.class)
				.cause()
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Utility class");
	}
}
