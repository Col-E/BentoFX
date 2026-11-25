package software.coley.bentofx.persistence.core.api.state;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.state.DockContainerBranchState.DockContainerBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState.DockContainerLeafStateBuilder;

import static javafx.geometry.Side.BOTTOM;
import static javafx.geometry.Side.TOP;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Equality for {@link DockContainerBranchState}, including the fields it inherits.
 */
class DockContainerBranchStateTest {

	@Test
	void branchStateHonoursTheEqualsContract() {
		StateVerifiers.configured()
				.forClass(DockContainerBranchState.class)
				.withNonnullFields(
						"identifier",
						"childDockableStates",
						"dividerPositions",
						"childDockContainerStates"
				)
				.verify();
	}

	@Test
	void branchChildOrderIsPartOfEquality() {
		final DockContainerLeafState first =
				new DockContainerLeafStateBuilder("leaf-1").setSide(TOP).build();
		final DockContainerLeafState second =
				new DockContainerLeafStateBuilder("leaf-2").setSide(BOTTOM).build();

		final DockContainerBranchState firstThenSecond =
				new DockContainerBranchStateBuilder("branch-1")
						.addDockContainerState(first)
						.addDockContainerState(second)
						.build();

		final DockContainerBranchState secondThenFirst =
				new DockContainerBranchStateBuilder("branch-1")
						.addDockContainerState(second)
						.addDockContainerState(first)
						.build();

		// Child order is layout, not incidental: the same containers left-to-right
		// versus right-to-left restore differently.
		assertThat(firstThenSecond)
				.describedAs("branch children in opposite orders")
				.isNotEqualTo(secondThenFirst);
	}
}
