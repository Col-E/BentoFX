package software.coley.bentofx.persistence.core.impl;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.bentofx.Bento;
import software.coley.bentofx.building.DockBuilding;
import software.coley.bentofx.control.DragDropStage;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.persistence.core.api.BentoLayout;
import software.coley.bentofx.persistence.core.api.BentoLayout.BentoLayoutBuilder;
import software.coley.bentofx.persistence.core.api.DockingLayout;
import software.coley.bentofx.persistence.core.api.DockingLayout.DockingLayoutBuilder;
import software.coley.bentofx.persistence.core.api.DockingLayoutRebuilder;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.provider.DockContainerLeafMenuFactoryProvider;
import software.coley.bentofx.persistence.core.api.provider.DockableStateProvider;
import software.coley.bentofx.persistence.core.api.provider.StageIconImageProvider;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.DockContainerBranchState;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState;
import software.coley.bentofx.persistence.core.api.state.DockContainerState;
import software.coley.bentofx.persistence.core.api.state.DockableState;
import software.coley.bentofx.persistence.core.api.state.DragDropStageState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static software.coley.bentofx.persistence.core.impl.StageUtils.getXInScreenBounds;
import static software.coley.bentofx.persistence.core.impl.StageUtils.getYInScreenBounds;

/**
 * Restores decoded Bento layout state into live BentoFX runtime objects.
 *
 * <p>Default implementation of {@link DockingLayoutRebuilder}. Public so the
 * interface's static factory method can construct it, but kept in a
 * non-exported package: callers obtain it through
 * {@link DockingLayoutRebuilder#create}.</p>
 *
 * @author Phil Bryant
 */
public final class DefaultDockingLayoutRebuilder implements DockingLayoutRebuilder {

	private static final Logger logger =
			LoggerFactory.getLogger(DefaultDockingLayoutRebuilder.class);

	private final BentoProvider bentoProvider;
	private final DockableStateProvider dockableStateProvider;
	private final @Nullable StageIconImageProvider stageIconImageProvider;
	private final @Nullable DockContainerLeafMenuFactoryProvider dockContainerLeafMenuFactoryProvider;

	public DefaultDockingLayoutRebuilder(
			final BentoProvider bentoProvider,
			final DockableStateProvider dockableStateProvider,
			final @Nullable StageIconImageProvider stageIconImageProvider,
			final @Nullable DockContainerLeafMenuFactoryProvider dockContainerLeafMenuFactoryProvider
	) {
		this.bentoProvider = Objects.requireNonNull(bentoProvider);
		this.dockableStateProvider = Objects.requireNonNull(dockableStateProvider);
		this.stageIconImageProvider = stageIconImageProvider;
		this.dockContainerLeafMenuFactoryProvider = dockContainerLeafMenuFactoryProvider;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>This method must run on the JavaFX application thread because it
	 * creates and mutates JavaFX and BentoFX objects.</p>
	 */
	@Override
	public DockingLayout rebuild(
			final List<BentoState> bentoStateList
	) {
		// A DragDropStage's root registers with its Bento as soon as the stage
		// has a scene, before the rest of the layout is built. If anything
		// after that throws - an application callback, most likely - those
		// roots would stay registered, never shown, and be captured by the next
		// save as extra root branches. So every stage given a scene is recorded
		// here and taken back out if the restore fails.
		final List<DragDropStage> stagesWithScenes = new ArrayList<>();

		try {
			return rebuild(bentoStateList, stagesWithScenes);
		} catch (final RuntimeException e) {
			stagesWithScenes.forEach(
					DefaultDockingLayoutRebuilder::unregisterRoot
			);
			throw e;
		}
	}

	/**
	 * Unregisters every drag/drop stage root in a restored layout from its
	 * {@link Bento}, for a layout that will not be applied after all. Must run
	 * on the JavaFX application thread.
	 *
	 * <p>Only drag/drop stages need this. A restored root branch registers only
	 * once it has a scene, and only drag/drop stages are given one here.</p>
	 *
	 * @param dockingLayout the layout being discarded.
	 */
	static void discard(final DockingLayout dockingLayout) {
		for (final BentoLayout bentoLayout : dockingLayout.getBentoLayouts()) {
			bentoLayout.getDragDropStages().forEach(
					DefaultDockingLayoutRebuilder::unregisterRoot
			);
		}
	}

	/**
	 * Unregisters a drag/drop stage's root branch from its {@link Bento}.
	 *
	 * @param dragDropStage the stage whose root branch to unregister.
	 */
	private static void unregisterRoot(final DragDropStage dragDropStage) {
		final Scene scene = dragDropStage.getScene();

		if (scene != null
				&& scene.getRoot() instanceof final DockContainerRootBranch rootBranch) {
			rootBranch.getBento().unregisterRoot(rootBranch);
		}
	}

	/**
	 * Does the work of {@link #rebuild(List)}.
	 *
	 * @param bentoStateList decoded Bento states.
	 * @param stagesWithScenes receives every drag/drop stage as soon as it has
	 * been given a scene, so a failure can unregister its root.
	 * @return restored docking layout.
	 */
	private DockingLayout rebuild(
			final List<BentoState> bentoStateList,
			final List<DragDropStage> stagesWithScenes
	) {
		final DockingLayoutBuilder dockingLayoutBuilder =
				new DockingLayoutBuilder();

		// Restore the BentoLayout, containing root branches and drag/drop
		// stages for each Bento, and add the BentoLayout to the
		// DockingLayout.
		for (final BentoState bentoState : bentoStateList) {

			final String bentoIdentifier = bentoState.getIdentifier();

			final BentoLayoutBuilder bentoLayoutBuilder =
					new BentoLayoutBuilder(bentoIdentifier);

			// All Bentos are not created equal - it's possible the client
			// application extended the Bento class or otherwise customized
			// its functionality, and used the custom Bento when creating
			// the layout that is being restored. Use the BentoProvider to
			// get the Bento for the bento identifier.
			final Bento bento = bentoProvider.getBento(bentoIdentifier)
					.orElseGet(() -> {
								logger.warn(
										"Could not find the Bento with " +
												"identifier {}. Some " +
												"docking features might " +
												"not be available.",
										bentoIdentifier
								);
								return new Bento();
							}
					);

			final DockBuilding dockBuilding = bento.dockBuilding();

			// Restore each DockContainerRootBranch and add it to the
			// BentoLayout.
			for (final DockContainerRootBranchState rootBranchState :
					bentoState.getRootBranchStates()) {
				bentoLayoutBuilder.addRootBranch(
						restoreRootBranchContainer(
								dockBuilding,
								rootBranchState
						)
				);
			}

			// Restore each DragDropStage and add it to the BentoLayout. The
			// showing flag is carried through rather than acted on: this module
			// does not show stages, so the persisted value is only useful once
			// the caller can read it back off the BentoLayout. Absent counts as
			// showing - see BentoLayout.wasShowing.
			for (final DragDropStageState dragDropStageState :
					bentoState.getDragDropStageStates()) {
				bentoLayoutBuilder.addDragDropStage(
						restoreDragDropStage(
								dockBuilding,
								dragDropStageState,
								stagesWithScenes
						),
						dragDropStageState.isShowing().orElse(true)
				);
			}

			dockingLayoutBuilder.addBentoLayout(
					bentoLayoutBuilder.build()
			);
		}

		return dockingLayoutBuilder.build();
	}

	/**
	 * Creates a {@link DragDropStage}, restore its
	 * {@link DockContainerRootBranch}, adds the {@link DockContainerRootBranch}
	 * to the {@link DragDropStage}, and applies the {@link DragDropStageState}
	 * to the {@link DragDropStage}.
	 *
	 * @param dockBuilding the {@link DockBuilding} to use to create
	 * {@link DockContainer}s and {@link Dockable}s in the {@link DragDropStage}.
	 * @param stageState   the {@link DragDropStageState} defining the persisted
	 * layout for the {@link DragDropStage}.
	 * @param stagesWithScenes receives the stage as soon as it has a scene.
	 * @return the restored {@link DragDropStage}.
	 */
	private DragDropStage restoreDragDropStage(
			final DockBuilding dockBuilding,
			final DragDropStageState stageState,
			final List<DragDropStage> stagesWithScenes
	) {

		final DragDropStage dragDropStage = new DragDropStage(
				stageState.isAutoClosedWhenEmpty()
		);

		// Every restored stage gets a scene, even when the persisted state held
		// no root branch. A scene-less Stage is a live hazard rather than a
		// harmless empty one: the captor dereferences the scene on the next
		// save, and core's own WINDOW_HIDDEN/WINDOW_SHOWN filters on
		// DragDropStage do the same as soon as anything shows it - which
		// callers do unconditionally for every stage in the returned layout.
		// Falling back to an empty root keeps the stage inert instead.
		final DockContainerRootBranch rootContainer =
				stageState.getDockContainerRootBranchState()
						.map(dockContainerRootBranchState ->
								restoreRootBranchContainer(
										dockBuilding,
										dockContainerRootBranchState
								)
						)
						.orElseGet(() -> {
							logger.warn(
									"Restored drag/drop stage '{}' has no " +
											"persisted root branch; giving it " +
											"an empty one.",
									stageState.getTitle().orElse("<untitled>")
							);
							return dockBuilding.root();
						});

		dragDropStage.setScene(new Scene(rootContainer));
		stagesWithScenes.add(dragDropStage);

		// Restore the DragDropStage's icons
		if (stageIconImageProvider != null) {
			dragDropStage.getIcons().addAll(
					stageIconImageProvider.getStageIcons()
			);
		}

		// Apply the stage state to the DragDropStage
		stageState.getTitle().ifPresent(dragDropStage::setTitle);
		stageState.getWidth().ifPresent(dragDropStage::setWidth);
		stageState.getHeight().ifPresent(dragDropStage::setHeight);
		stageState.getOpacity().ifPresent(dragDropStage::setOpacity);
		stageState.isIconified().ifPresent(dragDropStage::setIconified);
		stageState.isFullScreen().ifPresent(dragDropStage::setFullScreen);
		stageState.isMaximized().ifPresent(dragDropStage::setMaximized);
		stageState.isAlwaysOnTop().ifPresent(dragDropStage::setAlwaysOnTop);
		stageState.isResizable().ifPresent(dragDropStage::setResizable);
		stageState.isFocused()
				.filter(Boolean::booleanValue)
				.ifPresent(ignored -> dragDropStage.requestFocus());
		stageState.getModality().ifPresent(dragDropStage::initModality);

		stageState.getX().ifPresent(stageX -> {
					final double xInScreenBounds =
							getXInScreenBounds(dragDropStage, stageX);
					dragDropStage.setX(xInScreenBounds);
				}
		);

		stageState.getY().ifPresent(stageY -> {
					final double yInScreenBounds =
							getYInScreenBounds(dragDropStage, stageY);
					dragDropStage.setY(yInScreenBounds);
				}
		);

		return dragDropStage;
	}

	/**
	 * Creates a {@link DockContainerRootBranch}, restore its
	 * {@link DockContainer}s and {@link Dockable}s, and applies the
	 * {@link DockContainerRootBranchState} to the
	 * {@link DockContainerRootBranch}.
	 *
	 * @param dockBuilding the {@link DockBuilding} to use to create
	 * {@link DockContainer}s and {@link Dockable}s in the
	 * {@link DockContainerRootBranch}.
	 * @param rootBranchState the {@link DockContainerRootBranchState} defining
	 * the persisted layout for the {@link DockContainerRootBranch}.
	 * @return the restored {@link DockContainerRootBranch}.
	 */
	private DockContainerRootBranch restoreRootBranchContainer(
			final DockBuilding dockBuilding,
			final DockContainerRootBranchState rootBranchState
	) {
		final DockContainerRootBranch rootBranch =
				dockBuilding.root(rootBranchState.getIdentifier());

		rootBranchState.doPruneWhenEmpty().ifPresent(
				rootBranch::setPruneWhenEmpty
		);

		rootBranchState.getOrientation().ifPresent(
				rootBranch::setOrientation
		);

		// No child-dockable pass here: a root branch holds dockables only
		// through its descendant leaves, which restoreChildDockContainers
		// walks. The call that used to sit here could never have done anything
		// anyway - it ran before any child container existed, and
		// DockContainerBranch.addDockable returns false when there is no child
		// to accept the dockable.
		restoreChildDockContainers(dockBuilding, rootBranchState, rootBranch);
		applyDividerPositions(
				rootBranchState.getDividerPositions().entrySet(),
				rootBranch
		);
		collapseLeavesOnceLaidOut(
				rootBranch,
				getLeafStates(rootBranchState)
		);

		return rootBranch;
	}

	/**
	 * Restores all {@link DockContainer} children for
	 * {@link DockContainerRootBranch} and adds each {@link DockContainer} to
	 * the {@link DockContainerRootBranch}.
	 *
	 * @param dockBuilding    the {@link DockBuilding} to use to create
	 *                        {@link DockContainer}s and {@link Dockable}s in the {@link DragDropStage}.
	 * @param rootBranchState the {@link DockContainerRootBranchState}
	 *                        containing the {@link DockContainerState} of the
	 *                        {@link DockContainerRootBranch}'s children.
	 * @param rootBranch      the {@link DockContainerRootBranch} whose children are
	 *                        to be restored.
	 */
	private void restoreChildDockContainers(
			final DockBuilding dockBuilding,
			final DockContainerRootBranchState rootBranchState,
			final DockContainerRootBranch rootBranch
	) {
		for (final DockContainerState childState :
				rootBranchState.getChildDockContainerStates()) {

			final DockContainer child =
					restoreDockContainer(dockBuilding, childState);

			if (!rootBranch.addContainer(child)) {
				logger.warn(
						"Could not add container '{}' to root branch '{}'; " +
								"that part of the layout is missing.",
						child.getIdentifier(),
						rootBranch.getIdentifier()
				);
			}
		}
	}

	/**
	 * Restores a {@link DockContainer} from a {@link DockContainerBranchState}
	 * or {@link DockContainerLeafState}.
	 *
	 * <p>Those two are the only possibilities: {@link DockContainerState} is
	 * sealed to them, so the switch below is exhaustive and this method always
	 * returns a container. It used to carry a {@code default} arm that logged an
	 * unrecognized state type and returned {@code null}, which turned a
	 * programming error into a container quietly missing from the restored
	 * layout.</p>
	 *
	 * @param dockBuilding       the {@link DockBuilding} used to create the restored
	 *                           {@link DockContainer}.
	 * @param dockContainerState the {@link DockContainerState} defining the
	 *                           persisted layout for the {@link DockContainer}.
	 * @return the restored {@link DockContainer}.
	 */
	private DockContainer restoreDockContainer(
			final DockBuilding dockBuilding,
			final DockContainerState dockContainerState
	) {

		return switch (dockContainerState) {
			case final DockContainerBranchState branchState -> restoreBranch(dockBuilding, branchState);
			case final DockContainerLeafState leafState -> restoreLeaf(dockBuilding, leafState);
		};
	}

	/**
	 * Restores a {@link DockContainerBranch} from a
	 * {@link DockContainerBranchState}.
	 *
	 * @param dockBuilding the {@link DockBuilding} used to create the restored
	 *                     {@link DockContainerBranch}.
	 * @param branchState  the {@link DockContainerBranchState} defining the
	 *                     persisted layout for the {@link DockContainerBranch}.
	 * @return the restored {@link DockContainerBranch}.
	 */
	private DockContainerBranch restoreBranch(
			final DockBuilding dockBuilding,
			final DockContainerBranchState branchState
	) {
		final DockContainerBranch branch =
				dockBuilding.branch(branchState.getIdentifier());

		branchState.doPruneWhenEmpty().ifPresent(
				branch::setPruneWhenEmpty
		);

		branchState.getOrientation().ifPresent(orientation ->
				branch.orientationProperty().set(orientation)
		);

		// Restore child DockContainers and add them to their parent branch
		for (final DockContainerState dockContainerState :
				branchState.getChildDockContainerStates()) {

			final DockContainer child =
					restoreDockContainer(dockBuilding, dockContainerState);

			if (!branch.addContainer(child)) {
				logger.warn(
						"Could not add container '{}' to branch '{}'; that " +
								"part of the layout is missing.",
						child.getIdentifier(),
						branch.getIdentifier()
				);
			}
		}

		applyDividerPositions(
				branchState.getDividerPositions().entrySet(),
				branch
		);

		// Mirrors restoreRootBranchContainer. Both are needed: getLeaves and
		// getLeafStates only look at direct children, so each branch has to
		// collapse its own leaves. Every branch below the root is built here, so
		// the two calls together cover every leaf at every depth. Without this
		// one, a layout deeper than root-to-leaf silently lost the collapsed
		// state of every leaf it contained.
		collapseLeavesOnceLaidOut(
				branch,
				getLeafStates(branchState)
		);

		return branch;
	}

	/**
	 * Restores a {@link DockContainerLeaf} from a {@link DockContainerLeafState}.
	 *
	 * @param dockBuilding the {@link DockBuilding} used to create the restored
	 *                     {@link DockContainerLeaf}.
	 * @param leafState    the {@link DockContainerLeafState} defining the
	 *                     persisted layout for the {@link DockContainerLeaf}.
	 * @return the restored {@link DockContainerLeaf}.
	 */
	private DockContainerLeaf restoreLeaf(
			final DockBuilding dockBuilding,
			final DockContainerLeafState leafState
	) {
		// Create the leaf and restore its state
		final DockContainerLeaf leaf =
				dockBuilding.leaf(leafState.getIdentifier());
		leafState.doPruneWhenEmpty().ifPresent(leaf::setPruneWhenEmpty);
		leafState.getSide().ifPresent(leaf::setSide);
		leafState.isCanSplit().ifPresent(leaf::setCanSplit);
		leafState.isResizableWithParent().ifPresent(isResizableWithParent ->
				SplitPane.setResizableWithParent(
						leaf,
						isResizableWithParent
				)
		);

		// Set the menu factory
		if (dockContainerLeafMenuFactoryProvider != null) {
			dockContainerLeafMenuFactoryProvider.getDockContainerLeafMenuFactory(
					leaf.getIdentifier()
			).ifPresent(
					leaf::setMenuFactory
			);
		}

		// The identifier of the selected Dockable
		final String selectedId =
				leafState.getSelectedDockableIdentifier().orElse(null);

		// Restore Dockables and, if one is selected, select it
		for (final DockableState dockableState :
				leafState.getChildDockableStates()) {

			final String dockableId = dockableState.getIdentifier();
			final Dockable dockable = restoreDockable(dockBuilding, dockableId);

			if (dockable != null) {
				if (leaf.addDockable(dockable)) {
					// Only worth attempting when the dockable actually landed in
					// the leaf - selectDockable rejects anything the leaf does
					// not contain, so a failed add guarantees a failed select
					// and a second, redundant warning.
					if (dockableId.equals(selectedId)
							&& !leaf.selectDockable(dockable)) {
						logger.warn(
								"Could not select dockable '{}' in leaf '{}'; " +
										"the leaf will open on another tab.",
								dockableId,
								leaf.getIdentifier()
						);
					}
				} else {
					logger.warn(
							"Could not add dockable '{}' to leaf '{}'; it is " +
									"missing from the restored layout.",
							dockableId,
							leaf.getIdentifier()
					);
				}
			} else {
				logger.warn(
						"Dockable with ID '{}' could not be acquired.",
						dockableId
				);
			}
		}

		return leaf;
	}

	/**
	 * Uses the {@link DockableStateProvider} to resolve the
	 * {@link DockableState} and then applies that {@link DockableState} to newly
	 * created {@link Dockable}.
	 *
	 * @param dockBuilding       the {@link DockBuilding} used to create the restored
	 *                           {@link Dockable}.
	 * @param dockableIdentifier the identifier of the {@link Dockable} to be
	 *                           restored.
	 * @return the restored {@link Dockable} if its {@link DockableState} is
	 * provided by the {@link DockableStateProvider}; otherwise, returns
	 * {@code null}.
	 */
	private @Nullable Dockable restoreDockable(
			final DockBuilding dockBuilding,
			final String dockableIdentifier
	) {

		final DockableState dockableState;
		try {
			dockableState = Objects.requireNonNull(
					dockableStateProvider.resolveDockableState(dockableIdentifier),
					() ->
							"Could not resolve DockableState '" + dockableIdentifier + "'; skipping it."
			).orElse(null);
		} catch (final RuntimeException e) {
			logger.warn(
					"Could not resolve DockableState '{}'; skipping it.",
					dockableIdentifier,
					e
			);
			return null;
		}

		Dockable dockable;

		if (dockableState != null) {

			// The dockable is built from the identifier that was asked for, not
			// from the resolved state's own, so a provider answering with a state
			// for some other dockable would be applied to this one under this
			// identifier. Reporting rather than substituting: the persisted layout
			// asked for this identifier, and honoring it keeps the restored tree
			// matching what was saved.
			if (!dockableIdentifier.equals(dockableState.getIdentifier())) {
				logger.warn(
						"The DockableStateProvider answered the request for " +
								"dockable '{}' with the state for '{}'; applying " +
								"it to '{}' as persisted.",
						dockableIdentifier,
						dockableState.getIdentifier(),
						dockableIdentifier
				);
			}

			dockable = dockBuilding.dockable(dockableIdentifier);

			dockableState.getTitle().ifPresent(
					dockable::setTitle
			);

			dockableState.getTooltipText().ifPresent(tooltipText ->
					dockable.setTooltip(new Tooltip(tooltipText))
			);

			dockableState.getDockableIconFactory().ifPresent(
					dockable::setIconFactory
			);

			dockableState.getDockableNode().ifPresent(
					dockable::setNode
			);

			dockableState.getDockableMenuFactory().ifPresent(
					dockable::setContextMenuFactory
			);

			dockableState.getDragGroupMask().ifPresent(
					dockable::setDragGroupMask
			);

			dockableState.isClosable().ifPresent(
					dockable::setClosable
			);

			dockableState.getDockableConsumer().ifPresent(consumer ->
					consumer.accept(dockable)
			);

		} else {

			dockable = null;
		}

		return dockable;
	}

	/**
	 * Applies divider positions to a branch, deferred until after the initial
	 * layout pass.
	 *
	 * @param dividerPositions the divider positions to be applied.
	 * @param branch           the {@link DockContainerBranch} containing the
	 *                         {@code Divider}s.
	 */
	private static void applyDividerPositions(
			Collection<Map.Entry<Integer, Double>> dividerPositions,
			final DockContainerBranch branch
	) {
		for (final Map.Entry<Integer, Double> positionEntry : dividerPositions) {
			// Defer setting the divider position until after the initial layout pass
			Platform.runLater(() ->
					branch.setDividerPosition(
							positionEntry.getKey(),
							positionEntry.getValue()
					)
			);
		}
	}

	/**
	 * Collapses the branch's leaves once the branch has actually been laid out.
	 *
	 * <p>Waiting on layout. Collapsing measures the leaf's headers and those are
	 * zero until a layout pass has run, which pinned the leaf to about the width
	 * of a divider instead of its header. The caller attaches the restored tree
	 * to a {@code Scene} only after this returns, so a {@code Platform.runLater}
	 * can still run first; it did, on roughly one restore in four.</p>
	 *
	 * <p>Only the collapse waits. Divider positions stay as they were, applied
	 * whether or not the tree is ever attached.</p>
	 *
	 * @param branch the branch whose leaves may need collapsing.
	 * @param leafStates the states of the branch's direct leaf children.
	 */
	private static void collapseLeavesOnceLaidOut(
			final DockContainerBranch branch,
			final Collection<DockContainerLeafState> leafStates
	) {
		runOnceLaidOut(branch, () ->
				conditionallyCollapseLeaves(
						getLeaves(branch),
						leafStates,
						branch
				)
		);
	}

	/**
	 * Runs {@code action} as soon as {@code region} has non-empty layout bounds,
	 * immediately when it already has them.
	 *
	 * @param region the region to wait on.
	 * @param action the work needing laid-out geometry.
	 */
	private static void runOnceLaidOut(
			final Region region,
			final Runnable action
	) {
		if (isLaidOut(region.getLayoutBounds())) {
			action.run();
			return;
		}

		region.layoutBoundsProperty().addListener(
				new ChangeListener<>() {
					@Override
					public void changed(
							final ObservableValue<? extends Bounds> observable,
							final Bounds previousBounds,
							final Bounds currentBounds
					) {
						if (isLaidOut(currentBounds)) {
							// One shot: later resizes are the user's, not ours.
							observable.removeListener(this);
							action.run();
						}
					}
				}
		);
	}

	private static boolean isLaidOut(final Bounds bounds) {
		return bounds.getWidth() > 0 && bounds.getHeight() > 0;
	}

	/**
	 * Maps {@link DockContainerLeaf} to their identifiers.
	 *
	 * @return an unmodifiable a {@link Map} whose keys are
	 * {@link DockContainerLeaf#getIdentifier} and whose values are
	 * {@link DockContainerLeaf}
	 */
	private static Map<String, DockContainerLeaf> getLeaves(
			final DockContainerBranch branch
	) {
		// Map the leaves to their identifiers
		final Map<String, DockContainerLeaf> leaves = new HashMap<>();
		for (final DockContainer container : branch.getChildContainers()) {
			if (container instanceof final DockContainerLeaf leaf) {
				leaves.put(leaf.getIdentifier(), leaf);
			}
		}
		return Map.copyOf(leaves);
	}

	/**
	 * Returns an unmodifiable collection of all {@link DockContainerLeafState}.
	 * found in a {@link DockContainerBranchState}
	 *
	 * @param branchState the {@link DockContainerBranchState} whose
	 *                    {@link DockContainerLeafState} are to be found.
	 * @return an unmodifiable collection of all {@link DockContainerLeafState}.
	 */
	private static Collection<DockContainerLeafState> getLeafStates(
			final DockContainerBranchState branchState
	) {
		return branchState.getChildDockContainerStates().stream()
				.filter(DockContainerLeafState.class::isInstance)
				.map(DockContainerLeafState.class::cast)
				.toList();
	}

	/**
	 * Conditionally collapses {@link DockContainerLeaf} instances.
	 * This method should not be called until all {@link DockContainerLeaf} have
	 * been added to the {@link DockContainerBranch} because a
	 * {@link DockContainerLeaf} can only be collapsed if the
	 * {@link DockContainerBranch} containing it contains more than one
	 * {@link DockContainer}.
	 *
	 * @param leaves     the {@link DockContainerLeaf} to be conditionally collapsed.
	 * @param leafStates the {@link DockContainerLeafState}
	 * @param branch     the {@link DockContainerBranch} containing the
	 *                   {@link DockContainerLeaf}.
	 */
	private static void conditionallyCollapseLeaves(
			final Map<String, DockContainerLeaf> leaves,
			final Collection<DockContainerLeafState> leafStates,
			final DockContainerBranch branch
	) {
		for (final DockContainerLeafState leafState : leafStates) {
			final DockContainerLeaf leaf = leaves.get(leafState.getIdentifier());
			if (leaf != null) {
				leafState.isCollapsed().ifPresent(isCollapsed -> {
					branch.setContainerCollapsed(leaf, isCollapsed);

					// Deliberately comparing the resulting state rather than the
					// method's return value. setContainerCollapsed also returns
					// false when the leaf is already in the requested state,
					// which is the normal case for every uncollapsed leaf in
					// every restore - reporting on the return value would bury
					// the real failures in a warning per leaf.
					if (leaf.isCollapsed() != isCollapsed) {
						logger.warn(
								"Could not restore the collapsed state of leaf " +
										"'{}' in branch '{}'; wanted collapsed={}.",
								leafState.getIdentifier(),
								branch.getIdentifier(),
								isCollapsed
						);
					}
				});

				restoreUncollapsedSize(leafState, leaf);
			}
		}
	}

	/**
	 * Restores the size a collapsed {@link DockContainerLeaf} will occupy once the
	 * user expands it again.
	 *
	 * <p>Order matters, which is why this runs after
	 * {@link DockContainerBranch#setContainerCollapsed}. While a leaf is
	 * uncollapsed its uncollapsed-size properties are bound to its live width and
	 * height, so a value written before the collapse would be discarded.
	 * Collapsing unbinds them, and only then can the persisted size be applied.
	 * For a leaf that is not collapsed there is nothing to restore: its size comes
	 * from the layout and the divider positions.</p>
	 *
	 * @param leafState the persisted state holding the uncollapsed size.
	 * @param leaf      the {@link DockContainerLeaf} to apply the size to.
	 */
	private static void restoreUncollapsedSize(
			final DockContainerLeafState leafState,
			final DockContainerLeaf leaf
	) {
		if (!leaf.isCollapsed()) {
			return;
		}

		leafState.getUncollapsedSizePx().ifPresent(uncollapsedSizePx -> {
			if (!leaf.setUncollapsedSize(uncollapsedSizePx)) {
				logger.debug(
						"Could not restore the uncollapsed size for leaf {}.",
						leaf.getIdentifier()
				);
			}
		});
	}
}
