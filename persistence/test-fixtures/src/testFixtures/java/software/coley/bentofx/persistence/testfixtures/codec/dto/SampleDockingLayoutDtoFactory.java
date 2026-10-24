package software.coley.bentofx.persistence.testfixtures.codec.dto;

import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.BentoStateDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DividerPositionDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerBranchDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerLeafDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerRootBranchDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockableDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockingLayoutDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DragDropStageDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.LayoutMetadataDto;

import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Side.TOP;
import static javafx.stage.Modality.NONE;

/**
 * Creates representative docking layout DTOs for codec and mapper tests.
 *
 * <p>Every container appears in exactly one parent, every identifier is distinct,
 * and no two parents share a divider. Each one gets its own instance at its own
 * position, so a round trip that drops one and duplicates another cannot come back
 * equal.</p>
 *
 * @author Phil Bryant
 */
public final class SampleDockingLayoutDtoFactory {
	public static final String BENTO_IDENTIFIER = "bento-1";
	public static final String BRANCH_IDENTIFIER = "branch-1";
	public static final String DOCKABLE_IDENTIFIER = "dockable-1";
	public static final String LEAF_IDENTIFIER = "leaf-1";
	public static final String ROOT_IDENTIFIER = "root-1";
	public static final String ROOT_LEAF_IDENTIFIER = "leaf-2";
	public static final String ROOT_LEAF_DOCKABLE_IDENTIFIER = "dockable-2";
	public static final String STAGE_ROOT_IDENTIFIER = "root-2";
	public static final String STAGE_LEAF_IDENTIFIER = "leaf-3";
	public static final String STAGE_LEAF_DOCKABLE_IDENTIFIER = "dockable-3";
	public static final String STAGE_TITLE = "Stage";
	public static final String DOCKABLE_TITLE = "Dockable One";
	public static final String DOCKABLE_TOOLTIP_TEXT = "The first dockable";
	public static final int DOCKABLE_DRAG_GROUP_MASK = 6;

	private static final int DIVIDER_INDEX = 0;
	private static final double ROOT_DIVIDER_POSITION = 0.42;
	private static final double BRANCH_DIVIDER_POSITION = 0.58;
	private static final double LEAF_UNCOLLAPSED_SIZE_PX = 321.0;
	private static final double STAGE_X = 10.0;
	private static final double STAGE_Y = 20.0;
	private static final double STAGE_WIDTH = 800.0;
	private static final double STAGE_HEIGHT = 600.0;
	private static final double STAGE_OPACITY = 0.9;

	private SampleDockingLayoutDtoFactory() {
		throw new IllegalStateException("Utility class");
	}

	public static DockingLayoutDto createDockingLayoutDto() {
		final DockableDto dockable = new DockableDto();
		dockable.identifier = DOCKABLE_IDENTIFIER;
		dockable.title = DOCKABLE_TITLE;
		dockable.tooltipText = DOCKABLE_TOOLTIP_TEXT;
		dockable.dragGroupMask = DOCKABLE_DRAG_GROUP_MASK;
		dockable.isClosable = true;

		final DockContainerLeafDto leaf = new DockContainerLeafDto();
		leaf.identifier = LEAF_IDENTIFIER;
		leaf.pruneWhenEmpty = true;
		leaf.selectedDockableIdentifier = DOCKABLE_IDENTIFIER;
		leaf.side = TOP;
		leaf.isResizableWithParent = true;
		leaf.isCanSplit = true;
		leaf.uncollapsedSizePx = LEAF_UNCOLLAPSED_SIZE_PX;
		leaf.isCollapsed = false;
		leaf.dockables.add(dockable);

		final DockContainerBranchDto branch = new DockContainerBranchDto();
		branch.identifier = BRANCH_IDENTIFIER;
		branch.pruneWhenEmpty = false;
		branch.orientation = HORIZONTAL;
		branch.dividerPositions.add(createDividerDto(BRANCH_DIVIDER_POSITION));
		branch.childDockContainers.add(leaf);

		final DockContainerRootBranchDto root = new DockContainerRootBranchDto();
		root.identifier = ROOT_IDENTIFIER;
		root.pruneWhenEmpty = false;
		root.orientation = VERTICAL;
		root.dividerPositions.add(createDividerDto(ROOT_DIVIDER_POSITION));
		root.childDockContainers.add(branch);
		root.childDockContainers.add(
				createLeafDto(ROOT_LEAF_IDENTIFIER, ROOT_LEAF_DOCKABLE_IDENTIFIER)
		);

		final DragDropStageDto stage = new DragDropStageDto();
		stage.title = STAGE_TITLE;
		stage.x = STAGE_X;
		stage.y = STAGE_Y;
		stage.width = STAGE_WIDTH;
		stage.height = STAGE_HEIGHT;
		stage.modality = NONE;
		stage.opacity = STAGE_OPACITY;
		stage.iconified = false;
		stage.fullScreen = false;
		stage.maximized = true;
		stage.alwaysOnTop = false;
		stage.resizable = true;
		stage.showing = true;
		stage.focused = true;
		stage.autoCloseWhenEmpty = true;
		stage.dockContainerRootBranchDto = createStageRootBranchDto();

		final BentoStateDto bento = new BentoStateDto();
		bento.identifier = BENTO_IDENTIFIER;
		bento.rootBranches.add(root);
		bento.dragDropStages.add(stage);

		final LayoutMetadataDto metadata = new LayoutMetadataDto();
		metadata.schemaVersion = DockingLayoutDto.getCurrentSchemaVersion();

		final DockingLayoutDto layout = new DockingLayoutDto();
		layout.metadata = metadata;
		layout.bentoStates.add(bento);

		return layout;
	}

	/**
	 * {@return a divider at the sample index and the supplied position.}
	 *
	 * @param position where the divider sits.
	 */
	private static DividerPositionDto createDividerDto(final double position) {
		final DividerPositionDto divider = new DividerPositionDto();
		divider.index = DIVIDER_INDEX;
		divider.position = position;
		return divider;
	}

	/**
	 * {@return the drag/drop stage's own root branch, holding one leaf of its
	 * own.}
	 */
	private static DockContainerRootBranchDto createStageRootBranchDto() {
		final DockContainerRootBranchDto stageRoot =
				new DockContainerRootBranchDto();
		stageRoot.identifier = STAGE_ROOT_IDENTIFIER;
		stageRoot.pruneWhenEmpty = true;
		stageRoot.orientation = HORIZONTAL;
		stageRoot.childDockContainers.add(
				createLeafDto(STAGE_LEAF_IDENTIFIER, STAGE_LEAF_DOCKABLE_IDENTIFIER)
		);
		return stageRoot;
	}

	/**
	 * {@return a leaf holding a single dockable.}
	 *
	 * @param leafIdentifier the leaf's identifier.
	 * @param dockableIdentifier the identifier of the dockable it holds, which
	 * is also the leaf's selected dockable.
	 */
	private static DockContainerLeafDto createLeafDto(
			final String leafIdentifier,
			final String dockableIdentifier
	) {
		final DockableDto dockable = new DockableDto();
		dockable.identifier = dockableIdentifier;

		final DockContainerLeafDto leaf = new DockContainerLeafDto();
		leaf.identifier = leafIdentifier;
		leaf.pruneWhenEmpty = true;
		leaf.selectedDockableIdentifier = dockableIdentifier;
		leaf.side = TOP;
		leaf.isResizableWithParent = true;
		leaf.isCanSplit = true;
		leaf.uncollapsedSizePx = LEAF_UNCOLLAPSED_SIZE_PX;
		leaf.isCollapsed = false;
		leaf.dockables.add(dockable);

		return leaf;
	}
}
