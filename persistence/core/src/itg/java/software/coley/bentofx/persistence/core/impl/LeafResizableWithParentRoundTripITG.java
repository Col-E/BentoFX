package software.coley.bentofx.persistence.core.impl;

import javafx.scene.Scene;
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
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.persistence.core.api.DockingLayout;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState;
import software.coley.bentofx.persistence.core.api.state.DockableState;
import software.coley.bentofx.persistence.core.api.state.DockableState.DockableStateBuilder;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Side.LEFT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip coverage for a leaf's resizable-with-parent flag.
 *
 * <p>The captor used to read {@code leaf.isResizable()}, which resolves to
 * {@code Region.isResizable()} and is hard-coded to {@code true} for every
 * {@code Region}. A leaf the user had pinned as not-resizable-with-parent was
 * therefore saved as {@code true} and came back resizable, and no restore-side
 * test could see it: the restorer applied the state it was handed perfectly well,
 * the state itself was simply wrong before it ever got there.</p>
 *
 * <p>That is why these tests capture from a live tree instead of building state by
 * hand. Hand-built state exercises the restorer against a fixture that encodes
 * whatever the test author believed, so it cannot detect a capture-side defect.
 * The first test captures and asserts on the captured state; the second carries it
 * through a full capture-then-restore cycle.</p>
 *
 * @author Phil Bryant
 */
@ExtendWith(ApplicationExtension.class)
class LeafResizableWithParentRoundTripITG {

	private static final String BENTO_ID = "bento-resizable-round-trip";
	private static final String ROOT_BRANCH_ID = "root-resizable-round-trip";
	private static final String PINNED_LEAF_ID = "leaf-pinned-resizable-round-trip";
	private static final String FLEXIBLE_LEAF_ID = "leaf-flexible-resizable-round-trip";
	private static final String PINNED_DOCKABLE_ID = "dockable-pinned-resizable";
	private static final String FLEXIBLE_DOCKABLE_ID = "dockable-flexible-resizable";

	// No @Start hook: each capture builds and shows its own Stage so the tests
	// stay independent of one another and of the shared TestFX stage. That
	// matters here because the captor walks every open window.

	/**
	 * A leaf pinned with {@code SplitPane.setResizableWithParent(leaf, false)}
	 * must be captured as {@code false}. Before the fix this captured
	 * {@code true}, because every {@code Region} reports itself resizable.
	 */
	@Test
	void captureRecordsResizableWithParentFalseForPinnedLeaf(
			final FxRobot robot
	) {
		final DockContainerLeafState pinnedLeafState =
				captureLeafState(robot, PINNED_LEAF_ID);

		assertThat(pinnedLeafState.isResizableWithParent())
				.describedAs("pinnedLeafState.isResizableWithParent()")
				.contains(false);
	}

	/**
	 * Its sibling, left at the default, must still be captured as {@code true}.
	 * Without this the fix could have been "always capture false" and the other
	 * test would not have noticed.
	 */
	@Test
	void captureRecordsResizableWithParentTrueForFlexibleLeaf(
			final FxRobot robot
	) {
		final DockContainerLeafState flexibleLeafState =
				captureLeafState(robot, FLEXIBLE_LEAF_ID);

		assertThat(flexibleLeafState.isResizableWithParent())
				.describedAs("flexibleLeafState.isResizableWithParent()")
				.contains(true);
	}

	/**
	 * The end-to-end property the user actually cares about: capture a pinned
	 * leaf, restore it, and the restored leaf is still pinned. This is the test
	 * that would have caught B6 on its own, since it never states an expected
	 * value by hand - it compares the restored tree against the original.
	 */
	@Test
	void capturedThenRestoredLeafKeepsResizableWithParent(final FxRobot robot) {
		final AtomicReference<Boolean> restoredPinned = new AtomicReference<>();
		final AtomicReference<Boolean> restoredFlexible = new AtomicReference<>();

		robot.interact(() -> {
			final List<BentoState> bentoStates = captureBentoStates();

			final DockingLayout restoredLayout = new DefaultDockingLayoutRebuilder(
					new DefaultBentoProvider(new Bento(BENTO_ID + "-restored")),
					LeafResizableWithParentRoundTripITG::findDockableState,
					null,
					null
			).rebuild(bentoStates);

			final DockContainerRootBranch restoredRoot = restoredLayout
					.getBentoLayouts()
					.getFirst()
					.getRootBranches()
					.getFirst();

			restoredPinned.set(
					SplitPane.isResizableWithParent(
							requireNonNull(
									findLeaf(restoredRoot, PINNED_LEAF_ID),
									PINNED_LEAF_ID
							)
					)
			);
			restoredFlexible.set(
					SplitPane.isResizableWithParent(
							requireNonNull(
									findLeaf(restoredRoot, FLEXIBLE_LEAF_ID),
									FLEXIBLE_LEAF_ID
							)
					)
			);
		});

		assertThat(restoredPinned.get())
				.describedAs("restored pinned leaf isResizableWithParent")
				.isFalse();
		assertThat(restoredFlexible.get())
				.describedAs("restored flexible leaf isResizableWithParent")
				.isTrue();
	}

	/**
	 * Captures the live tree and returns the state of the requested leaf.
	 */
	private static DockContainerLeafState captureLeafState(
			final FxRobot robot,
			final String leafIdentifier
	) {
		final AtomicReference<List<BentoState>> bentoStatesReference =
				new AtomicReference<>();

		robot.interact(() ->
				bentoStatesReference.set(captureBentoStates())
		);

		final DockContainerRootBranchStateHolder holder =
				new DockContainerRootBranchStateHolder(
						bentoStatesReference.get()
				);

		return holder.leafState(leafIdentifier);
	}

	/**
	 * Builds root -> (pinned leaf, flexible leaf) on its own {@link Bento},
	 * attaches it to a shown stage, and captures. A fresh {@code Bento} and a
	 * fresh {@code Stage} per call keep this independent of the shared
	 * {@code @Start} stage and of any other test's containers, which matters
	 * because the captor walks every open window.
	 *
	 * <p>Must run on the JavaFX application thread.</p>
	 */
	private static List<BentoState> captureBentoStates() {
		final Bento bento = new Bento(BENTO_ID);
		final DockBuilding dockBuilding = bento.dockBuilding();

		final DockContainerRootBranch rootBranch =
				dockBuilding.root(ROOT_BRANCH_ID);
		rootBranch.setOrientation(HORIZONTAL);

		final DockContainerLeaf pinnedLeaf = leafWithDockable(
				dockBuilding,
				PINNED_LEAF_ID,
				PINNED_DOCKABLE_ID
		);
		final DockContainerLeaf flexibleLeaf = leafWithDockable(
				dockBuilding,
				FLEXIBLE_LEAF_ID,
				FLEXIBLE_DOCKABLE_ID
		);

		rootBranch.addContainer(pinnedLeaf);
		rootBranch.addContainer(flexibleLeaf);

		// The property under test. The sibling is deliberately left alone so it
		// keeps the JavaFX default of true.
		SplitPane.setResizableWithParent(pinnedLeaf, false);

		final Stage captureStage = new Stage();
		captureStage.setScene(new Scene(rootBranch, 800, 600));
		captureStage.show();

		try {
			final DefaultBentoProvider bentoProvider =
					new DefaultBentoProvider(bento);

			return new DefaultBentoStateCapturer(bentoProvider)
					.capture();
		} finally {
			captureStage.close();
		}
	}

	private static DockContainerLeaf leafWithDockable(
			final DockBuilding dockBuilding,
			final String leafIdentifier,
			final String dockableIdentifier
	) {
		final DockContainerLeaf leaf = dockBuilding.leaf(leafIdentifier);
		leaf.setSide(LEFT);

		final Dockable dockable = dockBuilding.dockable(dockableIdentifier);
		leaf.addDockable(dockable);
		leaf.selectDockable(dockable);

		return leaf;
	}

	private static Optional<DockableState> findDockableState(
			final String identifier
	) {
		if (PINNED_DOCKABLE_ID.equals(identifier)
				|| FLEXIBLE_DOCKABLE_ID.equals(identifier)) {
			return Optional.of(
					new DockableStateBuilder(identifier)
							.setTitle(identifier)
							.build()
			);
		}

		return Optional.empty();
	}

	private static @Nullable DockContainerLeaf findLeaf(
			final DockContainerBranch branch,
			final String leafIdentifier
	) {
		for (final DockContainer child : branch.getChildContainers()) {
			if (child instanceof final DockContainerLeaf leaf
					&& leaf.getIdentifier().equals(leafIdentifier)) {
				return leaf;
			}

			if (child instanceof final DockContainerBranch childBranch) {
				final DockContainerLeaf found =
						findLeaf(childBranch, leafIdentifier);
				if (found != null) {
					return found;
				}
			}
		}

		return null;
	}

	/**
	 * Narrows captured states down to the Bento and root branch this test owns,
	 * so a container left behind by another test cannot be mistaken for ours.
	 */
	private record DockContainerRootBranchStateHolder(
			List<BentoState> bentoStates
	) {

		private DockContainerLeafState leafState(final String leafIdentifier) {
			final BentoState bentoState = bentoStates.stream()
					.filter(state -> BENTO_ID.equals(state.getIdentifier()))
					.findFirst()
					.orElseThrow(() -> new AssertionError(
							"No captured BentoState for " + BENTO_ID
					));

			return bentoState.getRootBranchStates().stream()
					.filter(state -> ROOT_BRANCH_ID.equals(state.getIdentifier()))
					.flatMap(state -> state.getChildDockContainerStates().stream())
					.filter(DockContainerLeafState.class::isInstance)
					.map(DockContainerLeafState.class::cast)
					.filter(state -> leafIdentifier.equals(state.getIdentifier()))
					.findFirst()
					.orElseThrow(() -> new AssertionError(
							"No captured leaf state for " + leafIdentifier
					));
		}
	}
}
