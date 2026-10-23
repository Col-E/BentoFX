package software.coley.bentofx.persistence.core.api.state;

import org.junit.jupiter.api.Test;

/**
 * Equality for {@link DragDropStageState}, which sits outside the
 * {@link IdentifiableState} hierarchy and carries its own exact-runtime-type
 * check.
 *
 * <p>Only the EqualsVerifier test here: it already covers every field,
 * including the nested {@link DockContainerRootBranchState}, so a handwritten
 * field-by-field test would add nothing.</p>
 */
class DragDropStageStateTest {

	@Test
	void dragDropStageStateHonoursTheEqualsContract() {
		StateVerifiers.configured()
				.forClass(DragDropStageState.class)
				// Objects.requireNonNull in the constructor; every other field
				// is genuinely nullable and stays under null test.
				.withNonnullFields("isAutoClosedWhenEmpty")
				.verify();
	}
}
