package software.coley.bentofx.persistence.core.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class LayoutPersistenceProfileTest {

	private static final String LAYOUT_IDENTIFIER = "main-layout";

	@Test
	void factoryCreatesProfileWithOnlyLayoutIdentifier() {
		final LayoutPersistenceProfile profile = LayoutPersistenceProfile.of(LAYOUT_IDENTIFIER);

		assertThat(profile.layoutIdentifier())
				.describedAs("profile.layoutIdentifier()")
				.isEqualTo(LAYOUT_IDENTIFIER);
		assertThat(profile.codecIdentifier())
				.describedAs("profile.codecIdentifier()")
				.isNull();
		assertThat(profile.storageIdentifier())
				.describedAs("profile.storageIdentifier()")
				.isNull();
	}

	@Test
	// Suppress warnings for passing null argument to parameter annotated as
	// non-null; that's what we're testing.
	@SuppressWarnings("NullAway")
	void profileRequiresLayoutIdentifier() {
		assertThatNullPointerException()
				.describedAs("null pointer validation")
				.isThrownBy(() -> new LayoutPersistenceProfile(null, "json", "file"))
				.withMessage("layoutIdentifier");
	}

	@Test
	void profileRejectsBlankLayoutIdentifier() {
		assertThatIllegalArgumentException()
				.describedAs("blank layout identifier")
				.isThrownBy(() -> new LayoutPersistenceProfile("   ", "json", "file"))
				.withMessageContaining("layoutIdentifier")
				.withMessageContaining("blank");
	}

	@Test
	void profileRejectsLayoutIdentifierWithPathSeparator() {
		assertThatIllegalArgumentException()
				.describedAs("layout identifier with path separator")
				.isThrownBy(() -> new LayoutPersistenceProfile("sub/layout", "json", "file"))
				.withMessageContaining("layoutIdentifier");
	}

	@Test
	void profileRejectsLayoutIdentifierNamingADevice() {
		assertThatIllegalArgumentException()
				.describedAs("layout identifier naming a reserved device")
				.isThrownBy(() -> new LayoutPersistenceProfile("NUL", "json", "file"))
				.withMessageContaining("device");
	}

	@Test
	void profileAcceptsFrameworkReservedLayoutIdentifier() {
		// 'session', 'groups', 'activeLayout', 'defaultLayout' are reserved for
		// the framework's own layouts, not forbidden. The compact constructor
		// must accept them, because the framework constructs profiles with them.
		assertThat(new LayoutPersistenceProfile("session", null, null).layoutIdentifier())
				.describedAs("reserved identifier 'session'")
				.isEqualTo("session");
	}

	@Test
	void profileRejectsBlankCodecIdentifier() {
		assertThatIllegalArgumentException()
				.describedAs("blank codec identifier")
				.isThrownBy(() -> new LayoutPersistenceProfile(LAYOUT_IDENTIFIER, "", "file"))
				.withMessageContaining("codecIdentifier");
	}

	@Test
	void profileRejectsBlankStorageIdentifier() {
		assertThatIllegalArgumentException()
				.describedAs("blank storage identifier")
				.isThrownBy(() -> new LayoutPersistenceProfile(LAYOUT_IDENTIFIER, "json", "   "))
				.withMessageContaining("storageIdentifier");
	}

	@Test
	void profileAcceptsBlankDisplayNameAndGroup() {
		// displayName and group are user data that may arrive from hand-edited
		// storage. Defensive handling lives above the record (for example,
		// LayoutGroups.mergeGroupNames drops blank group names), so the record
		// stores what it was given rather than second-guessing the layer above.
		final LayoutPersistenceProfile profile = new LayoutPersistenceProfile(
				LAYOUT_IDENTIFIER, "json", "file", "   ", "   ");

		assertThat(profile.displayName()).isEqualTo("   ");
		assertThat(profile.group()).isEqualTo("   ");
	}

	@Test
	void profileAcceptsNullOptionalFields() {
		// Null carries documented meaning on each optional field: framework
		// selects codec/storage; no display name stored; no group.
		final LayoutPersistenceProfile profile = new LayoutPersistenceProfile(
				LAYOUT_IDENTIFIER, null, null, null, null);

		assertThat(profile.codecIdentifier()).isNull();
		assertThat(profile.storageIdentifier()).isNull();
		assertThat(profile.displayName()).isNull();
		assertThat(profile.group()).isNull();
	}
}
