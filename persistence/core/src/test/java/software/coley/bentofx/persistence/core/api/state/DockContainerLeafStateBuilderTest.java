package software.coley.bentofx.persistence.core.api.state;

import javafx.geometry.Side;
import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState.DockContainerLeafStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockableState.DockableStateBuilder;

import java.util.List;

import static javafx.geometry.Side.LEFT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DockContainerLeafStateBuilderTest {

	@Test
	void leafBuilderCapturesConfiguredValuesAndExposesImmutableChildren() {

		final String expectedSelectedDockableId = "dockable:selected";
		final String expectedTitle = "Selected";
		final String expectedLeafId = "leaf-1";
		final boolean expectedPruneWhenEmpty = true;
		final Side expectedSide = LEFT;
		final boolean expectedResizableWithParent = false;
		final boolean expectedCanSplit = true;
		final double expectedUncollapsedSizePx = 320.5;
		final boolean expectedCollapsed = false;

		DockableState selectedDockable = new DockableStateBuilder(expectedSelectedDockableId)
				.setTitle(expectedTitle)
				.setDragGroupMask(7)
				.setClosable(true)
				.build();

		DockContainerLeafState leafState = new DockContainerLeafStateBuilder(expectedLeafId)
				.setPruneWhenEmpty(expectedPruneWhenEmpty)
				.setSide(expectedSide)
				.setSelectedDockableStateIdentifier(expectedSelectedDockableId)
				.setResizableWithParent(expectedResizableWithParent)
				.setCanSplit(expectedCanSplit)
				.setUncollapsedSizePx(expectedUncollapsedSizePx)
				.setCollapsed(expectedCollapsed)
				.addChildDockableState(selectedDockable)
				.build();

		assertThat(leafState.getIdentifier())
				.describedAs("leafState.getIdentifier()")
				.isEqualTo(expectedLeafId);

		assertThat(leafState.doPruneWhenEmpty())
				.describedAs("leafState.doPruneWhenEmpty()")
				.contains(expectedPruneWhenEmpty);

		assertThat(leafState.getSide())
				.describedAs("leafState.getSide()")
				.contains(expectedSide);

		assertThat(leafState.getSelectedDockableIdentifier())
				.describedAs("leafState.getSelectedDockableIdentifier()")
				.contains(expectedSelectedDockableId);

		assertThat(leafState.isResizableWithParent())
				.describedAs("leafState.isResizableWithParent()")
				.contains(expectedResizableWithParent);

		assertThat(leafState.isCanSplit())
				.describedAs("leafState.isCanSplit()")
				.contains(expectedCanSplit);

		assertThat(leafState.getUncollapsedSizePx())
				.describedAs("leafState.getUncollapsedSizePx()")
				.contains(expectedUncollapsedSizePx);

		assertThat(leafState.isCollapsed())
				.describedAs("leafState.isCollapsed()")
				.contains(expectedCollapsed);

		assertThat(leafState.getChildDockableStates())
				.describedAs("leafState.getChildDockableStates()")
				.singleElement()
				.extracting(DockableState::getIdentifier)
				.isEqualTo(expectedSelectedDockableId);

		final List<DockableState> childDockableStates = leafState.getChildDockableStates();
		assertThatThrownBy(() -> childDockableStates.add(selectedDockable))
				.describedAs("exception thrown by () -> childDockableStates.add(selectedDockable)")
				.isInstanceOf(UnsupportedOperationException.class);
	}
}
