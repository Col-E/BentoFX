package software.coley.bentofx.persistence.testfixtures.codec.state;

import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.BentoState.BentoStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerBranchState;
import software.coley.bentofx.persistence.core.api.state.DockContainerBranchState.DockContainerBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState.DockContainerLeafStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState.DockContainerRootBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockableState;
import software.coley.bentofx.persistence.core.api.state.DockableState.DockableStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DragDropStageState;
import software.coley.bentofx.persistence.core.api.state.DragDropStageState.DragDropStageStateBuilder;

import java.util.List;

import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Side.*;
import static javafx.stage.Modality.APPLICATION_MODAL;

/**
 * Creates a whole docking layout as {@code *State} objects, for tests that need
 * to compare a layout against itself rather than against a handful of
 * identifiers.
 *
 * <p>When a property is set, it is set to a value distinct from its neighbors',
 * so a round-trip that drops one, or crosses two, shows up as an inequality. A
 * dockable's node, factories and consumer are unset. The application supplies
 * those at restore time.</p>
 *
 * <p>Two containers are deliberately sparser than the rest: the last leaf of the
 * root branch holds no dockables at all, and the leaf inside the drag/drop stage
 * sets only its side. An empty container and a barely-configured one are cases a
 * round trip has to survive, so the layout carries one of each.</p>
 *
 * @author Phil Bryant
 */
public final class SampleBentoStateFactory {

	public static final String BENTO_IDENTIFIER = "bento-1";
	public static final String ROOT_IDENTIFIER = "root-1";
	public static final String FIRST_LEAF_IDENTIFIER = "leaf-A";
	public static final String NESTED_BRANCH_IDENTIFIER = "branch-B";
	public static final String NESTED_LEAF_IDENTIFIER = "leaf-B1";
	public static final String LAST_LEAF_IDENTIFIER = "leaf-C";
	public static final String STAGE_ROOT_IDENTIFIER = "stage-root";
	public static final String STAGE_LEAF_IDENTIFIER = "stage-leaf";
	public static final String STAGE_TITLE = "Floating Stage";

	/**
	 * The one property {@code DragDropStageStateBuilder} requires at construction,
	 * named here rather than passed as a bare literal.
	 */
	private static final boolean IS_AUTO_CLOSED_WHEN_EMPTY = true;

	private static final String FIRST_DOCKABLE_IDENTIFIER = "dockable-A1";
	private static final String SECOND_DOCKABLE_IDENTIFIER = "dockable-A2";
	private static final String NESTED_DOCKABLE_IDENTIFIER = "dockable-B1";
	private static final String STAGE_DOCKABLE_IDENTIFIER = "dockable-S1";

	private SampleBentoStateFactory() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * {@return one Bento holding a root branch of three child dock containers - a
	 * leaf, a branch, and a second leaf - plus a drag/drop stage with a root
	 * branch of its own.}
	 */
	public static List<BentoState> createBentoStates() {
		return List.of(
				new BentoStateBuilder(BENTO_IDENTIFIER)
						.addRootBranchState(createRootBranchState())
						.addDragDropStageState(createDragDropStageState())
						.build()
		);
	}

	private static DockContainerRootBranchState createRootBranchState() {
		return new DockContainerRootBranchStateBuilder(ROOT_IDENTIFIER)
				.setOrientation(VERTICAL)
				.setPruneWhenEmpty(false)
				.addDividerPosition(0, 0.25)
				.addDividerPosition(1, 0.75)
				.addDockContainerState(createFirstLeafState())
				.addDockContainerState(createNestedBranchState())
				.addDockContainerState(createLastLeafState())
				.build();
	}

	private static DockContainerLeafState createFirstLeafState() {
		return new DockContainerLeafStateBuilder(FIRST_LEAF_IDENTIFIER)
				.setPruneWhenEmpty(true)
				.setSide(TOP)
				.setSelectedDockableStateIdentifier(SECOND_DOCKABLE_IDENTIFIER)
				.setResizableWithParent(true)
				.setCanSplit(false)
				.setUncollapsedSizePx(321.5)
				.setCollapsed(false)
				.addChildDockableState(dockableState(FIRST_DOCKABLE_IDENTIFIER))
				.addChildDockableState(dockableState(SECOND_DOCKABLE_IDENTIFIER))
				.build();
	}

	private static DockContainerBranchState createNestedBranchState() {
		return new DockContainerBranchStateBuilder(NESTED_BRANCH_IDENTIFIER)
				.setOrientation(HORIZONTAL)
				.setPruneWhenEmpty(true)
				.addDividerPosition(0, 0.5)
				.addDockContainerState(createNestedLeafState())
				.build();
	}

	private static DockContainerLeafState createNestedLeafState() {
		return new DockContainerLeafStateBuilder(NESTED_LEAF_IDENTIFIER)
				.setPruneWhenEmpty(false)
				.setSide(BOTTOM)
				.setSelectedDockableStateIdentifier(NESTED_DOCKABLE_IDENTIFIER)
				.setResizableWithParent(false)
				.setCanSplit(true)
				.setUncollapsedSizePx(120.0)
				.setCollapsed(true)
				.addChildDockableState(dockableState(NESTED_DOCKABLE_IDENTIFIER))
				.build();
	}

	private static DockContainerLeafState createLastLeafState() {
		return new DockContainerLeafStateBuilder(LAST_LEAF_IDENTIFIER)
				.setPruneWhenEmpty(true)
				.setSide(LEFT)
				.setResizableWithParent(true)
				.setCanSplit(true)
				.setUncollapsedSizePx(64.25)
				.setCollapsed(false)
				.build();
	}

	private static DragDropStageState createDragDropStageState() {
		return new DragDropStageStateBuilder(IS_AUTO_CLOSED_WHEN_EMPTY)
				.setTitle(STAGE_TITLE)
				.setX(10.5)
				.setY(20.5)
				.setWidth(800.0)
				.setHeight(600.0)
				.setModality(APPLICATION_MODAL)
				.setOpacity(0.9)
				.setIconified(false)
				.setFullScreen(false)
				.setMaximized(true)
				.setAlwaysOnTop(true)
				.setResizable(false)
				.setShowing(true)
				.setFocused(false)
				.setDockContainerRootBranchState(createStageRootBranchState())
				.build();
	}

	private static DockContainerRootBranchState createStageRootBranchState() {
		return new DockContainerRootBranchStateBuilder(STAGE_ROOT_IDENTIFIER)
				.setOrientation(HORIZONTAL)
				.setPruneWhenEmpty(true)
				.addDockContainerState(
						new DockContainerLeafStateBuilder(STAGE_LEAF_IDENTIFIER)
								.setSide(TOP)
								.addChildDockableState(
										dockableState(STAGE_DOCKABLE_IDENTIFIER)
								)
								.build()
				)
				.build();
	}

	/**
	 * {@return a dockable whose every persistable property is set, each derived
	 * from its identifier so no two dockables in the layout share a value.}
	 *
	 * @param identifier the dockable's identifier.
	 */
	private static DockableState dockableState(final String identifier) {
		return new DockableStateBuilder(identifier)
				.setTitle("Title of " + identifier)
				.setTooltipText("Tooltip for " + identifier)
				.setDragGroupMask(identifier.length())
				.setClosable(identifier.endsWith("1"))
				.build();
	}
}
