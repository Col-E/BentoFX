package software.coley.bentofx.persistence.core.api.state;

import javafx.geometry.Orientation;
import javafx.geometry.Side;
import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState.DockContainerLeafStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState.DockContainerRootBranchStateBuilder;

import java.util.List;
import java.util.Map;

import static javafx.geometry.Orientation.VERTICAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The root branch builder had no test of its own, which is how it could duplicate
 * {@link DockContainerBranchState.DockContainerBranchStateBuilder} and drift from it
 * unnoticed. It now delegates to that builder, so these cover every field
 * surviving the hand-off.
 */
class DockContainerRootBranchStateBuilderTest {

	@Test
	void rootBranchBuilderCapturesEveryFieldItDelegates() {

		final String expectedRootBranchName = "root-branch-1";
		final boolean expectedPruneWhenEmpty = false;
		final Orientation expectedOrientation = VERTICAL;
		final double expectedDividerPosition0 = 0.25;
		final double expectedDividerPosition1 = 0.75;

		final DockContainerLeafState childLeaf =
				new DockContainerLeafStateBuilder("leaf-child")
						.setSide(Side.BOTTOM)
						.build();

		final DockContainerRootBranchState rootBranchState =
				new DockContainerRootBranchStateBuilder(expectedRootBranchName)
						.setPruneWhenEmpty(expectedPruneWhenEmpty)
						.setOrientation(expectedOrientation)
						.addDividerPosition(0, expectedDividerPosition0)
						.addDividerPosition(1, expectedDividerPosition1)
						.addDockContainerState(childLeaf)
						.build();

		assertThat(rootBranchState.getIdentifier())
				.describedAs("rootBranchState.getIdentifier()")
				.isEqualTo(expectedRootBranchName);

		assertThat(rootBranchState.doPruneWhenEmpty())
				.describedAs("rootBranchState.doPruneWhenEmpty()")
				.contains(expectedPruneWhenEmpty);

		assertThat(rootBranchState.getOrientation())
				.describedAs("rootBranchState.getOrientation()")
				.contains(expectedOrientation);

		assertThat(rootBranchState.getDividerPositions())
				.describedAs("rootBranchState.getDividerPositions()")
				.containsEntry(0, expectedDividerPosition0)
				.containsEntry(1, expectedDividerPosition1);

		assertThat(rootBranchState.getChildDockableStates())
				.describedAs("rootBranchState.getChildDockableStates()")
				.isEmpty();

		assertThat(rootBranchState.getChildDockContainerStates())
				.describedAs("rootBranchState.getChildDockContainerStates()")
				.containsExactly(childLeaf);
	}

	@Test
	void rootBranchStateExposesImmutableCollections() {
		final DockContainerLeafState childLeaf =
				new DockContainerLeafStateBuilder("leaf-child").build();

		final DockContainerRootBranchState rootBranchState =
				new DockContainerRootBranchStateBuilder("root-branch")
						.addDividerPosition(0, 0.5)
						.addDockContainerState(childLeaf)
						.build();

		final Map<Integer, Double> dividerPositions =
				rootBranchState.getDividerPositions();
		assertThatThrownBy(() -> dividerPositions.put(1, 0.5))
				.describedAs("exception thrown by () -> dividerPositions.put(1, 0.5)")
				.isInstanceOf(UnsupportedOperationException.class);

		final List<DockContainerState> dockContainerStates =
				rootBranchState.getChildDockContainerStates();
		assertThatThrownBy(() -> dockContainerStates.add(childLeaf))
				.describedAs("exception thrown by () -> dockContainerStates.add(childLeaf)")
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void builtStateIsNotAffectedByLaterBuilderMutation() {
		final DockContainerLeafState firstLeaf =
				new DockContainerLeafStateBuilder("leaf:first").build();
		final DockContainerLeafState secondLeaf =
				new DockContainerLeafStateBuilder("leaf:second").build();

		final DockContainerRootBranchStateBuilder builder =
				new DockContainerRootBranchStateBuilder("root-branch")
						.addDividerPosition(0, 0.25)
						.addDockContainerState(firstLeaf);

		final DockContainerRootBranchState rootBranchState = builder.build();

		builder.addDividerPosition(1, 0.75)
				.addDockContainerState(secondLeaf);

		assertThat(rootBranchState.getDividerPositions())
				.describedAs("rootBranchState.getDividerPositions()")
				.containsOnly(Map.entry(0, 0.25));
		assertThat(rootBranchState.getChildDockContainerStates())
				.describedAs("rootBranchState.getChildDockContainerStates()")
				.containsExactly(firstLeaf);
	}
}
