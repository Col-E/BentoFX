package software.coley.bentofx.persistence.core.impl;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import software.coley.bentofx.Bento;
import software.coley.bentofx.building.DockBuilding;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.persistence.core.api.DockingLayout;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.DockContainerBranchState;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState;
import software.coley.bentofx.persistence.core.api.state.DockContainerState;
import software.coley.bentofx.persistence.core.api.state.DockableState;
import software.coley.bentofx.persistence.core.api.state.DockableState.DockableStateBuilder;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Side.LEFT;
import static javafx.geometry.Side.TOP;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * General capture/restore round trip test using reflection to automatically
 * capture all fields of any state type.
 *
 * <p>Other round-trip tests in this package were written against a known
 * defect, so each proves one named property survives. This test asserts the
 * whole state instead: capture a deliberately non-default tree, restore it,
 * capture again, and compare the two states field by field.</p>
 *
 * <p>Two properties are deliberately out of scope. A {@code DragDropStage}'s
 * geometry is owned by the window manager, not the layout, so it does not
 * round-trip exactly; and {@code isShowing} cannot be tested because this module
 * never shows a restored stage and an unshown stage is not captured</p>
 *
 * @author Phil Bryant
 */
@ExtendWith(ApplicationExtension.class)
class LayoutRoundTripITG {

	private static final String BENTO_ID = "bento-round-trip";
	private static final String ROOT_BRANCH_ID = "root-round-trip";
	private static final String NESTED_BRANCH_ID = "branch-round-trip";
	private static final String COLLAPSED_LEAF_ID = "leaf-collapsed-round-trip";
	private static final String OPEN_LEAF_ID = "leaf-open-round-trip";
	private static final String SIBLING_LEAF_ID = "leaf-sibling-round-trip";
	private static final String COLLAPSED_DOCKABLE_ID = "dockable-collapsed-round-trip";
	private static final String FIRST_OPEN_DOCKABLE_ID = "dockable-open-first-round-trip";
	private static final String SECOND_OPEN_DOCKABLE_ID = "dockable-open-second-round-trip";
	private static final String SIBLING_DOCKABLE_ID = "dockable-sibling-round-trip";

	private static final double UNCOLLAPSED_SIZE_PX = 321;
	private static final double SCENE_WIDTH = 800;
	private static final double SCENE_HEIGHT = 600;

	/** Divider positions are doubles that have been through a real layout pass. */
	private static final double DIVIDER_TOLERANCE = 1e-6;

	@Test
	void capturedLayoutSurvivesRestoreFieldForField(final FxRobot robot) {
		final List<BentoState> captured = captureNonDefaultLayout(robot);

		assertCapturedLayoutIsNonTrivial(captured);

		final List<BentoState> recaptured = restoreThenCapture(robot, captured);

		assertThat(recaptured)
				.describedAs("state captured after a restore of the same layout")
				.usingRecursiveComparison()
				.withEqualsForType(
						(first, second) ->
								Math.abs(first - second) <= DIVIDER_TOLERANCE,
						Double.class
				)
				.isEqualTo(captured);
	}

	/**
	 * Guards against the round trip passing because both sides are empty. A
	 * restored root branch is not attached to a {@code Scene} until the caller
	 * attaches it, and an unattached branch is invisible to a capture, so an
	 * attach that silently failed would compare nothing against nothing.
	 */
	private static void assertCapturedLayoutIsNonTrivial(
			final List<BentoState> captured
	) {
		assertThat(captured)
				.describedAs("captured states")
				.hasSize(1);

		final BentoState bentoState = captured.getFirst();
		assertThat(bentoState.getRootBranchStates())
				.describedAs("captured root branch states")
				.hasSize(1);

		final DockContainerLeafState collapsedLeafState =
				findLeafState(bentoState, COLLAPSED_LEAF_ID);

		assertThat(collapsedLeafState.isCollapsed())
				.describedAs("the collapsed leaf really was captured as collapsed")
				.contains(true);
		assertThat(collapsedLeafState.getUncollapsedSizePx())
				.describedAs("the collapsed leaf's uncollapsed size")
				.contains(UNCOLLAPSED_SIZE_PX);
		assertThat(findLeafState(bentoState, OPEN_LEAF_ID).isResizableWithParent())
				.describedAs("the open leaf's resizable-with-parent flag")
				.contains(false);
	}

	/**
	 * Builds and shows the tree described on {@link #buildTree}, then captures it.
	 */
	private static List<BentoState> captureNonDefaultLayout(final FxRobot robot) {
		final AtomicReference<List<BentoState>> capturedReference =
				new AtomicReference<>();
		final AtomicReference<Stage> stageReference = new AtomicReference<>();
		final AtomicReference<Tree> treeReference = new AtomicReference<>();

		robot.interact(() -> {
			final Tree tree = buildTree();
			treeReference.set(tree);

			final Stage stage = new Stage();
			stage.setScene(new Scene(tree.rootBranch(), SCENE_WIDTH, SCENE_HEIGHT));
			stage.show();
			stageReference.set(stage);
		});

		// Collapsing consults live divider geometry, so it has to happen after the
		// tree is laid out rather than while it is being assembled.
		robot.interact(() -> {
			final Tree tree = treeReference.get();
			tree.nestedBranch().setContainerCollapsed(tree.collapsedLeaf(), true);
			tree.collapsedLeaf().setUncollapsedSize(UNCOLLAPSED_SIZE_PX);
		});

		fence(robot);

		try {
			robot.interact(() ->
					capturedReference.set(
							new DefaultBentoStateCapturer(
									new DefaultBentoProvider(
											treeReference.get().bento()
									)
							).capture()
					)
			);

			return capturedReference.get();
		} finally {
			robot.interact(() -> stageReference.get().close());
		}
	}

	/**
	 * Restores the supplied state, attaches and shows the result, then captures it
	 * again.
	 */
	private static List<BentoState> restoreThenCapture(
			final FxRobot robot,
			final List<BentoState> captured
	) {
		final AtomicReference<List<BentoState>> recapturedReference =
				new AtomicReference<>();
		final AtomicReference<Stage> stageReference = new AtomicReference<>();

		final Bento restoredBento = new Bento(BENTO_ID);

		robot.interact(() -> {
			final DockingLayout dockingLayout =
					new DefaultDockingLayoutRebuilder(
							new DefaultBentoProvider(restoredBento),
							LayoutRoundTripITG::findDockableState,
							null,
							null
					).rebuild(captured);

			final DockContainerRootBranch rootBranch = dockingLayout
					.getBentoLayouts()
					.getFirst()
					.getRootBranches()
					.getFirst();

			// The restorer hands root branches back unattached, and an unattached
			// branch never registers with its Bento, so without this the capture
			// below would find nothing at all.
			final Stage stage = new Stage();
			stage.setScene(new Scene(rootBranch, SCENE_WIDTH, SCENE_HEIGHT));
			stage.show();
			stageReference.set(stage);
		});

		// Divider positions and collapse geometry are applied on the JavaFX queue,
		// after the initial layout pass.
		fence(robot);

		try {
			robot.interact(() ->
					recapturedReference.set(
							new DefaultBentoStateCapturer(
									new DefaultBentoProvider(restoredBento)
							).capture()
					)
			);

			return recapturedReference.get();
		} finally {
			robot.interact(() -> stageReference.get().close());
		}
	}

	/**
	 * Builds {@code root(VERTICAL) -> [branch(HORIZONTAL) -> [collapsed leaf, open
	 * leaf], sibling leaf]}.
	 *
	 * <p>Every property is deliberately set away from its default, because a round
	 * trip that compares defaults to defaults proves nothing. The nested branch is
	 * horizontal and both its leaves sit on the left, which is what core requires
	 * for a collapse to be applicable.</p>
	 *
	 * <p>Must run on the JavaFX application thread.</p>
	 */
	private static Tree buildTree() {
		final Bento bento = new Bento(BENTO_ID);
		final DockBuilding dockBuilding = bento.dockBuilding();

		final DockContainerRootBranch rootBranch =
				dockBuilding.root(ROOT_BRANCH_ID);
		rootBranch.setOrientation(VERTICAL);
		rootBranch.setPruneWhenEmpty(false);

		final DockContainerBranch nestedBranch =
				dockBuilding.branch(NESTED_BRANCH_ID);
		nestedBranch.orientationProperty().set(HORIZONTAL);
		nestedBranch.setPruneWhenEmpty(true);

		final DockContainerLeaf collapsedLeaf =
				dockBuilding.leaf(COLLAPSED_LEAF_ID);
		collapsedLeaf.setSide(LEFT);
		collapsedLeaf.setCanSplit(false);
		collapsedLeaf.setPruneWhenEmpty(false);
		addDockable(dockBuilding, collapsedLeaf, COLLAPSED_DOCKABLE_ID);

		final DockContainerLeaf openLeaf = dockBuilding.leaf(OPEN_LEAF_ID);
		openLeaf.setSide(LEFT);
		addDockable(dockBuilding, openLeaf, FIRST_OPEN_DOCKABLE_ID);
		final Dockable selected =
				addDockable(dockBuilding, openLeaf, SECOND_OPEN_DOCKABLE_ID);
		// Selecting the second one, so the captured selection is not just
		// whichever dockable happened to be added first.
		openLeaf.selectDockable(selected);
		SplitPane.setResizableWithParent(openLeaf, false);

		nestedBranch.addContainer(collapsedLeaf);
		nestedBranch.addContainer(openLeaf);

		final DockContainerLeaf siblingLeaf =
				dockBuilding.leaf(SIBLING_LEAF_ID);
		siblingLeaf.setSide(TOP);
		addDockable(dockBuilding, siblingLeaf, SIBLING_DOCKABLE_ID);

		// Two children on the root as well, so the root carries a divider of its
		// own rather than leaving that half of the state empty.
		rootBranch.addContainer(nestedBranch);
		rootBranch.addContainer(siblingLeaf);

		return new Tree(bento, rootBranch, nestedBranch, collapsedLeaf);
	}

	private static Dockable addDockable(
			final DockBuilding dockBuilding,
			final DockContainerLeaf leaf,
			final String dockableIdentifier
	) {
		final Dockable dockable = dockBuilding.dockable(dockableIdentifier);
		leaf.addDockable(dockable);
		return dockable;
	}

	/**
	 * The restorer resolves dockables through a provider, so the test has to supply
	 * one for each identifier the captured state names.
	 */
	private static Optional<DockableState> findDockableState(
			final String dockableIdentifier
	) {
		return Optional.of(
				new DockableStateBuilder(dockableIdentifier)
						.setDockableNode(new Label(dockableIdentifier))
						.build()
		);
	}

	private static DockContainerLeafState findLeafState(
			final BentoState bentoState,
			final String leafIdentifier
	) {
		return requireNonNull(
				findLeafState(
						bentoState.getRootBranchStates().getFirst()
								.getChildDockContainerStates(),
						leafIdentifier
				),
				"captured leaf state " + leafIdentifier
		);
	}

	private static @Nullable DockContainerLeafState findLeafState(
			final List<DockContainerState> containerStates,
			final String leafIdentifier
	) {
		for (final DockContainerState containerState : containerStates) {
			if (containerState instanceof final DockContainerLeafState leafState) {
				if (leafIdentifier.equals(leafState.getIdentifier())) {
					return leafState;
				}
			} else if (containerState instanceof final DockContainerBranchState branchState) {
				final DockContainerLeafState nested = findLeafState(
						branchState.getChildDockContainerStates(),
						leafIdentifier
				);
				if (nested != null) {
					return nested;
				}
			}
		}
		return null;
	}

	/**
	 * A no-op round trip through the JavaFX thread, letting work the restorer and
	 * core queued for after the layout pass finish before anything reads the tree.
	 */
	private static void fence(final FxRobot robot) {
		robot.interact(() -> { /* fence */ });
		robot.interact(() -> { /* fence */ });
	}

	/** The parts of the built tree the test needs to reach afterwards. */
	private record Tree(
			Bento bento,
			DockContainerRootBranch rootBranch,
			DockContainerBranch nestedBranch,
			DockContainerLeaf collapsedLeaf
	) {
	}
}
