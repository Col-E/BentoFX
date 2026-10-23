package software.coley.bentofx.persistence.core.api.state;

import javafx.geometry.Orientation;
import javafx.geometry.Side;
import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.state.DockContainerBranchState.DockContainerBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState.DockContainerLeafStateBuilder;

import java.util.List;
import java.util.Map;

import static javafx.geometry.Orientation.VERTICAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DockContainerBranchStateBuilderTest {

	@Test
	void branchBuilderCapturesOrientationDividerPositionsAndNestedChildren() {

		final String expectedBranchName = "branch-1";
		final boolean expectedPruneWhenEmpty = false;
		final Orientation expectedOrientation = VERTICAL;
		final double expectedDividerPosition0 = 0.25;
		final double expectedDividerPosition1 = 0.75;

		DockContainerLeafState childLeaf =
				new DockContainerLeafStateBuilder("leaf-child")
						.setSide(Side.BOTTOM)
						.build();

		DockContainerBranchState branchState =
				new DockContainerBranchStateBuilder(expectedBranchName)
						.setPruneWhenEmpty(expectedPruneWhenEmpty)
						.setOrientation(expectedOrientation)
						.addDividerPosition(0, expectedDividerPosition0)
						.addDividerPosition(1, expectedDividerPosition1)
						.addDockContainerState(childLeaf)
						.build();

		assertThat(branchState.getIdentifier())
				.describedAs("branchState.getIdentifier()")
				.isEqualTo(expectedBranchName);

		assertThat(branchState.doPruneWhenEmpty())
				.describedAs("branchState.doPruneWhenEmpty()")
				.contains(expectedPruneWhenEmpty);

		assertThat(branchState.getOrientation())
				.describedAs("branchState.getOrientation()")
				.contains(expectedOrientation);

		assertThat(branchState.getDividerPositions())
				.describedAs("branchState.getDividerPositions()")
				.containsEntry(0, expectedDividerPosition0)
				.containsEntry(1, expectedDividerPosition1);

		assertThat(branchState.getChildDockContainerStates())
				.describedAs("branchState.getChildDockContainerStates()")
				.containsExactly(childLeaf);

		final Map<Integer, Double> dividerPositions =
				branchState.getDividerPositions();
		assertThatThrownBy(() -> dividerPositions.put(2, 0.5))
				.describedAs("exception thrown by () -> dividerPositions.put(2, 0.5)")
				.isInstanceOf(UnsupportedOperationException.class);

		final List<DockContainerState> dockContainerStates =
				branchState.getChildDockContainerStates();
		assertThatThrownBy(() -> dockContainerStates.add(childLeaf))
				.describedAs("exception thrown by () -> dockContainerStates.add(childLeaf)")
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void builtStateIsNotAffectedByLaterBuilderMutation() {
		DockContainerLeafState firstLeaf =
				new DockContainerLeafStateBuilder("leaf:first")
						.build();
		DockContainerLeafState secondLeaf =
				new DockContainerLeafStateBuilder("leaf:second")
						.build();

		DockContainerBranchStateBuilder builder =
				new DockContainerBranchStateBuilder("branch")
						.addDividerPosition(0, 0.25)
						.addDockContainerState(firstLeaf);

		DockContainerBranchState state = builder.build();

		builder.addDividerPosition(1, 0.75)
				.addDockContainerState(secondLeaf);

		assertThat(state.getDividerPositions())
				.describedAs("state.getDividerPositions()")
				.containsOnly(Map.entry(0, 0.25));
		assertThat(state.getChildDockContainerStates())
				.describedAs("state.getChildDockContainerStates()")
				.containsExactly(firstLeaf);
	}

}
