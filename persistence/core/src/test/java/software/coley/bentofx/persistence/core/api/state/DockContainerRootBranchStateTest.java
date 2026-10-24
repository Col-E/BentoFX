package software.coley.bentofx.persistence.core.api.state;

import javafx.geometry.Side;
import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.state.DockContainerBranchState.DockContainerBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState.DockContainerLeafStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState.DockContainerRootBranchStateBuilder;

import static javafx.geometry.Orientation.VERTICAL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Equality for {@link DockContainerRootBranchState}, which declares no field of its
 * own and no {@code equals} override.
 */
class DockContainerRootBranchStateTest {

	@Test
	void rootBranchStateHonoursTheEqualsContract() {
		StateVerifiers.configured()
				.forClass(DockContainerRootBranchState.class)
				.withNonnullFields(
						"identifier",
						"childDockableStates",
						"dividerPositions",
						"childDockContainerStates"
				)
				.verify();
	}

	/**
	 * Why {@link IdentifiableState#equals(Object)} compares exact runtime types. A
	 * root branch state adds no field, so any value-based comparison would call these
	 * two equal, and they restore into different containers.
	 */
	@Test
	void rootBranchStateIsNotEqualToBranchStateWithTheSameValues() {
		final DockContainerLeafState child =
				new DockContainerLeafStateBuilder("leaf-1")
						.setSide(Side.TOP)
						.build();

		final DockContainerBranchState branchState =
				new DockContainerBranchStateBuilder("branch-1")
						.setPruneWhenEmpty(true)
						.setOrientation(VERTICAL)
						.addDividerPosition(0, 0.25)
						.addDockContainerState(child)
						.build();

		final DockContainerRootBranchState rootBranchState =
				new DockContainerRootBranchStateBuilder("branch-1")
						.setPruneWhenEmpty(true)
						.setOrientation(VERTICAL)
						.addDividerPosition(0, 0.25)
						.addDockContainerState(child)
						.build();

		assertThat((Object) branchState)
				.describedAs("branch state versus root branch state, same values")
				.isNotEqualTo(rootBranchState);

		assertThat((Object) rootBranchState)
				.describedAs("root branch state versus branch state, same values")
				.isNotEqualTo(branchState);
	}
}
