package software.coley.bentofx.persistence.core.impl;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.bentofx.Bento;
import software.coley.bentofx.control.DragDropStage;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.persistence.core.api.BentoStateCapturer;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.BentoState.BentoStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerBranchState;
import software.coley.bentofx.persistence.core.api.state.DockContainerBranchState.DockContainerBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState.DockContainerLeafStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState.DockContainerRootBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerState;
import software.coley.bentofx.persistence.core.api.state.DockableState;
import software.coley.bentofx.persistence.core.api.state.DockableState.DockableStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DragDropStageState.DragDropStageStateBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static software.coley.bentofx.persistence.core.impl.StageUtils.getAllStages;

/**
 * Captures the live BentoFX runtime graph as serializable persistence state.
 *
 * <p>Default implementation of {@link BentoStateCapturer}. Public so the
 * interface's static factory method can construct it, but kept in a
 * non-exported package. Callers obtain it through
 * {@link BentoStateCapturer#create(BentoProvider)}.</p>
 *
 * @author Phil Bryant
 */
public final class DefaultBentoStateCapturer implements BentoStateCapturer {

	private static final Logger logger =
			LoggerFactory.getLogger(DefaultBentoStateCapturer.class);

	private final BentoProvider bentoProvider;

	public DefaultBentoStateCapturer(final BentoProvider bentoProvider) {
		this.bentoProvider = Objects.requireNonNull(bentoProvider);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Drag-and-drop stages are handled separately from ordinary root
	 * branches. A root branch belonging to a {@link DragDropStage} is saved
	 * as part of that stage and is therefore excluded from the Bento's ordinary
	 * root-branch states.</p>
	 */
	@Override
	public List<BentoState> capture() {

		final List<DragDropStageRoot> dragDropStageRoots =
				getAllStages().stream()
						.filter(DragDropStage.class::isInstance)
						.map(DragDropStage.class::cast)
						.map(this::captureDragDropStageRoot)
						.flatMap(Optional::stream)
						.toList();

		return bentoProvider.getAllBentos().stream()
				.map(bento -> captureBentoState(
						bento,
						dragDropStageRoots
				))
				.toList();
	}

	/**
	 * Captures the current state of a Bento.
	 *
	 * <p>Matching drag-and-drop stages are added to the resulting state first.
	 * Their root branches are collected so they can be excluded from the
	 * ordinary root branches saved for the Bento.</p>
	 *
	 * @param bento the Bento whose state is being captured
	 * @param dragDropStageRoots all drag-and-drop stages whose scene roots are
	 * {@link DockContainerRootBranch} instances
	 *
	 * @return the captured state of the supplied Bento
	 */
	private BentoState captureBentoState(
			final Bento bento,
			final List<DragDropStageRoot> dragDropStageRoots
	) {

		final BentoStateBuilder stateBuilder =
				new BentoStateBuilder(bento.getIdentifier());

		final Set<DockContainerRootBranch> dragDropRoots =
				new HashSet<>();

		dragDropStageRoots.stream()
				.filter(stageRoot -> bento.matchesIdentity(
						stageRoot.rootBranch().getBento()
				))
				.forEach(stageRoot -> {
					buildAndAddDragDropStage(
							stageRoot.stage(),
							stateBuilder
					);

					dragDropRoots.add(stageRoot.rootBranch());
				});

		bento.getRootContainers().stream()
				.filter(rootBranch -> !dragDropRoots.contains(rootBranch))
				.map(this::buildRootBranchState)
				.forEach(stateBuilder::addRootBranchState);

		return stateBuilder.build();
	}

	/**
	 * Creates a drag-and-drop stage/root association when the stage's scene
	 * root is a {@link DockContainerRootBranch}.
	 *
	 * @param stage the drag-and-drop stage to inspect
	 *
	 * @return an association containing the stage and its root branch, or an
	 * empty {@link Optional} when the stage has no scene, or its scene root is not
	 * a {@link DockContainerRootBranch}
	 */
	private Optional<DragDropStageRoot> captureDragDropStageRoot(
			final DragDropStage stage
	) {

		final DockContainerRootBranch rootBranch =
				getDockContainerRootBranch(stage);

		if (rootBranch == null) {
			return Optional.empty();
		}

		return Optional.of(
				new DragDropStageRoot(
						stage,
						rootBranch
				)
		);
	}

	/**
	 * Associates a drag-and-drop stage with the root branch displayed by its
	 * scene.
	 *
	 * @param stage the drag-and-drop stage
	 * @param rootBranch the root branch displayed by the stage
	 */
	private record DragDropStageRoot(
			DragDropStage stage,
			DockContainerRootBranch rootBranch
	) {
	}

	/**
	 * Saves the state of a {@link DragDropStage} to a
	 * {@link BentoStateBuilder}.
	 *
	 * @param dragDropStage the {@link DragDropStage} whose state is to be
	 * saved.
	 * @param bentoStateBuilder the {@link BentoStateBuilder} to which the
	 * {@link DragDropStage} state is to be saved.
	 */
	private void buildAndAddDragDropStage(
			DragDropStage dragDropStage,
			BentoStateBuilder bentoStateBuilder
	) {
		// A DragDropStage can only have one rootBranch
		final DockContainerRootBranch rootBranch =
				getDockContainerRootBranch(dragDropStage);

		if (rootBranch == null) {
			logger.debug("Ignoring unknown root branch {}", dragDropStage);
		} else {
			final DockContainerRootBranchState rootBranchState =
					buildRootBranchState(rootBranch);

			bentoStateBuilder.addDragDropStageState(
					new DragDropStageStateBuilder(
							dragDropStage.isAutoCloseWhenEmpty()
					)
							.setTitle(dragDropStage.getTitle())
							.setX(dragDropStage.getX())
							.setY(dragDropStage.getY())
							.setWidth(dragDropStage.getWidth())
							.setHeight(dragDropStage.getHeight())
							.setModality(dragDropStage.getModality())
							.setOpacity(dragDropStage.getOpacity())
							.setIconified(dragDropStage.isIconified())
							.setFullScreen(dragDropStage.isFullScreen())
							.setMaximized(dragDropStage.isMaximized())
							.setAlwaysOnTop(dragDropStage.isAlwaysOnTop())
							.setResizable(dragDropStage.isResizable())
							.setShowing(dragDropStage.isShowing())
							.setFocused(dragDropStage.isFocused())
							.setDockContainerRootBranchState(
									rootBranchState
							)
							.build()
			);
		}
	}

	/**
	 * {@return the {@link DockContainerRootBranch} for the specified
	 * {@link DragDropStage}, {@code null} when the stage has no scene or its scene
	 * root is not a {@link DockContainerRootBranch}.}
	 *
	 * <p>The null-scene case is not hypothetical. A {@link Stage} carries no scene
	 * until one is assigned, and this module's own restorer can hand back a
	 * scene-less {@link DragDropStage} when the persisted state held no root
	 * branch.</p>
	 *
	 * @param stage the {@link DragDropStage} whose {@link DockContainerRootBranch}
	 * is to be found.
	 */
	private @Nullable DockContainerRootBranch getDockContainerRootBranch(
			final DragDropStage stage
	) {
		final Scene scene = stage.getScene();

		if (scene == null) {
			logger.debug(
					"Ignoring drag/drop stage with no scene: {}",
					stage
			);
			return null;
		}

		final Parent parent = scene.getRoot();

		if (parent instanceof final DockContainerRootBranch rootBranch) {
			return rootBranch;
		}

		logger.debug("Ignoring unknown parent {}", parent);
		return null;
	}

	/**
	 * Builds a {@link DockContainerRootBranchState} for a
	 * {@link DockContainerRootBranch}.
	 *
	 * @param branch the {@link DockContainerRootBranch} whose
	 * {@link DockContainerRootBranchState} is to be built.
	 *
	 * @return the {@link DockContainerRootBranchState}.
	 */
	private DockContainerRootBranchState buildRootBranchState(
			final DockContainerRootBranch branch
	) {
		final DockContainerRootBranchStateBuilder builder =
				new DockContainerRootBranchStateBuilder(
						branch.getIdentifier()
				);

		builder.setPruneWhenEmpty(branch.doPruneWhenEmpty());

		builder.setOrientation(branch.orientationProperty().get());

		// Divider positions (supports multiple)
		final double[] positions = branch.getDividerPositions();

		for (int i = 0; i < positions.length; i++) {

			builder.addDividerPosition(i, positions[i]);
		}

		for (final DockContainer dockContainer : branch.getChildContainers()) {
			builder.addDockContainerState(buildDockContainerState(dockContainer));
		}

		return builder.build();
	}

	/**
	 * Builds a {@link DockContainerState} for a {@link DockContainer}. If the
	 * {@link DockContainer} is not a branch or leaf, which should not happen,
	 * builds an empty leaf to keep the state valid.
	 *
	 * @param dockContainer the {@link DockContainer} whose
	 * {@link DockContainerState} is to be built.
	 *
	 * @return the {@link DockContainerState}.
	 */
	private DockContainerState buildDockContainerState(
			final DockContainer dockContainer
	) {

		return switch (dockContainer) {
			case final DockContainerBranch branch -> buildDockContainerBranchState(branch);
			case final DockContainerLeaf leaf -> buildDockContainerLeafState(leaf);
		};
	}

	/**
	 * Builds a {@link DockContainerBranchState} for a {@link DockContainerBranch}.
	 *
	 * @param branch the {@link DockContainerBranch} whose
	 * {@link DockContainerBranchState} is to be built.
	 *
	 * @return the {@link DockContainerBranchState}.
	 */
	private DockContainerBranchState buildDockContainerBranchState(
			final DockContainerBranch branch
	) {
		final String id = getNonBlankValue(
				branch.getIdentifier(),
				"branch-" + System.identityHashCode(branch)
		);

		final DockContainerBranchStateBuilder builder =
				new DockContainerBranchStateBuilder(
						id
				);

		builder.setPruneWhenEmpty(branch.doPruneWhenEmpty());

		builder.setOrientation(branch.orientationProperty().get());

		// Divider positions (supports multiple)
		final double[] positions = branch.getDividerPositions();

		for (int i = 0; i < positions.length; i++) {

			builder.addDividerPosition(i, positions[i]);
		}

		// Only the child containers, deliberately. A branch owns no dockables of
		// its own: DockContainerBranch.getDockables() is a recursive, flattened
		// view of its descendants, so capturing it here recorded every dockable
		// again on each ancestor branch, once per level of nesting. The leaves
		// that actually hold them capture them in buildLeafState.
		for (final DockContainer dockContainer : branch.getChildContainers()) {
			builder.addDockContainerState(buildDockContainerState(dockContainer));
		}

		return builder.build();
	}

	/**
	 * Builds a {@link DockContainerLeafState} for a {@link DockContainerLeaf}.
	 *
	 * @param leaf the {@link DockContainerLeaf} whose
	 * {@link DockContainerLeafState} is to be built.
	 *
	 * @return the {@link DockContainerLeafState}.
	 */
	private DockContainerLeafState buildDockContainerLeafState(
			final DockContainerLeaf leaf
	) {

		final String id = leaf.getIdentifier();

		final DockContainerLeafStateBuilder leafStateBuilder =
				new DockContainerLeafStateBuilder(id);

		leafStateBuilder.setPruneWhenEmpty(leaf.doPruneWhenEmpty());

		leafStateBuilder.setSide(leaf.getSide());

		// SplitPane.isResizableWithParent, not leaf.isResizable(). The latter is
		// Region.isResizable(), which is hard-coded to true for every Region, so
		// it captured true no matter what the user had configured. The restorer
		// applies this through SplitPane.setResizableWithParent, so that is the
		// property that has to be read back here.
		leafStateBuilder.setResizableWithParent(
				SplitPane.isResizableWithParent(leaf)
		);

		leafStateBuilder.setCanSplit(leaf.isCanSplit());

		// Only ask for the uncollapsed size when the leaf has a side. A leaf's side
		// is legitimately nullable - setSide(@Nullable Side) is public and
		// documented as "null to not display any headers" - but core's
		// getUncollapsedSize() switches on the side and throws for the null case.
		// That throw happens here, outside the per-dockable guard below, so a single
		// headerless leaf anywhere in the tree aborted the save for every Bento. The
		// field is optional in DockContainerLeafState, so omitting it is a supported
		// outcome rather than a loss; and a leaf with no headers cannot be collapsed
		// by core in the first place, which is what the size is for.
		//
		// Guarding on isCollapsed() instead would be wrong: an uncollapsed leaf's
		// size is exactly the value we want recorded for a later collapse.
		if (leaf.getSide() != null) {
			leafStateBuilder.setUncollapsedSizePx(leaf.getUncollapsedSize());
		}

		leafStateBuilder.setCollapsed(leaf.isCollapsed());

		final Dockable selected = leaf.getSelectedDockable();

		if (selected != null) {

			leafStateBuilder.setSelectedDockableStateIdentifier(
					selected.getIdentifier()
			);
		}

		// Dockables.
		// Deliberately not catching Exception here. A dockable that cannot be
		// captured has to abort the save, not be dropped from it. Capture runs
		// to completion before anything is written, so a failure here means the
		// persisted layout is left exactly as it was - whereas swallowing the
		// failure reported a successful save and then truncated the last good
		// file with a layout missing a pane.
		for (final Dockable dockable : leaf.getDockables()) {
			leafStateBuilder.addChildDockableState(buildDockableState(dockable));
		}

		return leafStateBuilder.build();
	}

	/**
	 * Builds a {@link DockableState} for a {@link Dockable}.
	 *
	 * @param dockable the {@link Dockable} whose {@link DockableState} is to
	 * be built.
	 *
	 * @return the {@link DockableState}.
	 */
	private DockableState buildDockableState(final Dockable dockable) {
		return new DockableStateBuilder(dockable.getIdentifier())
				.build();
	}

	/**
	 * Returns the non-blank value of a {@link String}.
	 *
	 * @param value the {@link String} whose non-blankness is to be returned.
	 * @param fallback the value to return when the {@link String} is blank.
	 *
	 * @return the non-blank value of a {@link String}.
	 */
	private static String getNonBlankValue(
			final String value,
			final String fallback
	) {
		return !value.isBlank() ? value : fallback;
	}
}
