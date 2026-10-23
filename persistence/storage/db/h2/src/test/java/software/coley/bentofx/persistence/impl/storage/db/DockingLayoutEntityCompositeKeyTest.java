package software.coley.bentofx.persistence.impl.storage.db;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DockingLayoutEntityCompositeKeyTest {

	private static final String LAYOUT_IDENTIFIER = "main-layout";

	private static final String CODEC_IDENTIFIER = "json";

	@Test
	void constructorAssignsBothIdentifiers() {
		final DockingLayoutEntityCompositeKey key =
				new DockingLayoutEntityCompositeKey(LAYOUT_IDENTIFIER, CODEC_IDENTIFIER);

		assertThat(key.layoutIdentifier)
				.describedAs("key.layoutIdentifier")
				.isEqualTo(LAYOUT_IDENTIFIER);
		assertThat(key.codecIdentifier)
				.describedAs("key.codecIdentifier")
				.isEqualTo(CODEC_IDENTIFIER);
	}

	@Test
	// Suppress warnings for passing null argument to parameter annotated as
	// non-null; that's what we're testing.
	@SuppressWarnings("NullAway")
	void constructorRequiresLayoutIdentifier() {
		assertThatNullPointerException()
				.describedAs("null pointer validation")
				.isThrownBy(() -> new DockingLayoutEntityCompositeKey(null, CODEC_IDENTIFIER))
				.withMessage("layoutIdentifier");
	}

	@Test
	// Suppress warnings for passing null argument to parameter annotated as
	// non-null; that's what we're testing.
	@SuppressWarnings("NullAway")
	void constructorRequiresCodecIdentifier() {
		assertThatNullPointerException()
				.describedAs("null pointer validation")
				.isThrownBy(() -> new DockingLayoutEntityCompositeKey(LAYOUT_IDENTIFIER, null))
				.withMessage("codecIdentifier");
	}

	@Test
	void equalsIsReflexive() {
		final DockingLayoutEntityCompositeKey key =
				new DockingLayoutEntityCompositeKey(LAYOUT_IDENTIFIER, CODEC_IDENTIFIER);

		assertThat(key)
				.describedAs("key equals itself")
				.isEqualTo(key);
	}

	@Test
	void equalsIsTrueForMatchingIdentifiers() {
		final DockingLayoutEntityCompositeKey first =
				new DockingLayoutEntityCompositeKey(LAYOUT_IDENTIFIER, CODEC_IDENTIFIER);
		final DockingLayoutEntityCompositeKey second =
				new DockingLayoutEntityCompositeKey(LAYOUT_IDENTIFIER, CODEC_IDENTIFIER);

		assertThat(first)
				.describedAs("keys built from the same identifiers and has same hashCode of equal keys")
				.isEqualTo(second)
				.hasSameHashCodeAs(second);
	}

	@Test
	void equalsIsFalseForDifferentLayoutIdentifier() {
		final DockingLayoutEntityCompositeKey first =
				new DockingLayoutEntityCompositeKey(LAYOUT_IDENTIFIER, CODEC_IDENTIFIER);
		final DockingLayoutEntityCompositeKey second =
				new DockingLayoutEntityCompositeKey("other-layout", CODEC_IDENTIFIER);

		assertThat(first)
				.describedAs("keys with different layout identifiers")
				.isNotEqualTo(second);
	}

	@Test
	void equalsIsFalseForDifferentCodecIdentifier() {
		final DockingLayoutEntityCompositeKey first =
				new DockingLayoutEntityCompositeKey(LAYOUT_IDENTIFIER, CODEC_IDENTIFIER);
		final DockingLayoutEntityCompositeKey second =
				new DockingLayoutEntityCompositeKey(LAYOUT_IDENTIFIER, "xml");

		assertThat(first)
				.describedAs("keys with different codec identifiers")
				.isNotEqualTo(second);
	}

	@Test
	void equalsIsFalseForNull() {
		final DockingLayoutEntityCompositeKey key =
				new DockingLayoutEntityCompositeKey(LAYOUT_IDENTIFIER, CODEC_IDENTIFIER);

		assertThat(key)
				.describedAs("key compared to null")
				.isNotEqualTo(null);
	}

	@Test
	void equalsIsFalseForDifferentType() {
		final DockingLayoutEntityCompositeKey key =
				new DockingLayoutEntityCompositeKey(LAYOUT_IDENTIFIER, CODEC_IDENTIFIER);

		assertThat(key)
				.describedAs("key compared to an unrelated type")
				.isNotEqualTo(LAYOUT_IDENTIFIER);
	}

	@Test
	void noArgsConstructorLeavesIdentifiersNull() {
		final DockingLayoutEntityCompositeKey key = new DockingLayoutEntityCompositeKey();

		assertThat(key.layoutIdentifier)
				.describedAs("key.layoutIdentifier from the JPA no-args constructor")
				.isNull();
		assertThat(key.codecIdentifier)
				.describedAs("key.codecIdentifier from the JPA no-args constructor")
				.isNull();
	}
}
