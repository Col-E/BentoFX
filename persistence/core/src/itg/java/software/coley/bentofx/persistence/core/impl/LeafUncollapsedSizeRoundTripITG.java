package software.coley.bentofx.persistence.core.impl;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import software.coley.bentofx.Bento;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.persistence.core.api.DockingLayout;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.BentoState.BentoStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState.DockContainerLeafStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState.DockContainerRootBranchStateBuilder;
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
import static org.assertj.core.api.Assertions.within;

/**
 * Round-trip coverage for the size a collapsed leaf returns to when expanded.
 *
 * <p>The captor recorded {@code uncollapsedSizePx} and the restorer ignored it
 * entirely, so a restored-collapsed pane jumped to a default width the first time
 * the user expanded it rather than returning to the width they left it at. Closing
 * the gap needed a public setter on {@code DockContainerLeaf}, since the property
 * was only reachable through a {@code protected} method.</p>
 *
 * <p>Ordering is the subtle part and the reason these tests exist. While a leaf is
 * uncollapsed its uncollapsed-size properties are <em>bound</em> to its live width
 * and height, so a value written before the collapse is discarded. Collapsing
 * unbinds them. The restorer therefore has to apply the size after
 * {@code setContainerCollapsed}, and a test that only checked the final value
 * without forcing that ordering could pass against a broken implementation.</p>
 *
 * @author Phil Bryant
 */
@ExtendWith(ApplicationExtension.class)
class LeafUncollapsedSizeRoundTripITG {

	private static final String BENTO_ID = "bento-uncollapsed-size";
	private static final String ROOT_BRANCH_ID = "root-uncollapsed-size";
	private static final String COLLAPSED_LEAF_ID = "leaf-collapsed-uncollapsed-size";
	private static final String OPEN_LEAF_ID = "leaf-open-uncollapsed-size";
	private static final String COLLAPSED_DOCKABLE_ID = "dockable-collapsed-uncollapsed-size";
	private static final String OPEN_DOCKABLE_ID = "dockable-open-uncollapsed-size";

	/**
	 * A distinctive width, well away from any default or from half of the scene
	 * width, so a passing assertion cannot be a coincidence.
	 */
	private static final double PERSISTED_UNCOLLAPSED_SIZE = 237.0;

	/**
	 * The persisted value must round-trip exactly, so this only absorbs
	 * floating-point noise.
	 */
	private static final double EXACT_SIZE_TOLERANCE = 0.5;

	/**
	 * Tolerance for the width the leaf actually renders at after expanding, which
	 * is not required to equal the stored size to the pixel. Core positions
	 * dividers by their center point, so a divider contributes half its width as
	 * implicit padding, and the resulting position is pixel-snapped - expanding to
	 * a stored 237px settles at 236px with the default divider. Four pixels covers
	 * that geometry without being loose enough to accept a default-sized pane,
	 * which in this fixture would be several hundred pixels wide.
	 */
	private static final double RENDERED_SIZE_TOLERANCE = 4.0;

	/**
	 * The property that regressed: a leaf restored as collapsed must remember the
	 * width it should return to. Before the fix the persisted value was dropped
	 * and the leaf reported whatever the layout happened to give it.
	 */
	@Test
	void restoredCollapsedLeafKeepsPersistedUncollapsedSize(final FxRobot robot) {
		final DockContainerLeaf collapsedLeaf =
				restoreAndFindLeaf(robot, COLLAPSED_LEAF_ID);

		assertThat(collapsedLeaf.isCollapsed())
				.describedAs("collapsedLeaf.isCollapsed()")
				.isTrue();
		assertThat(collapsedLeaf.getUncollapsedSize())
				.describedAs("collapsedLeaf.getUncollapsedSize()")
				.isCloseTo(PERSISTED_UNCOLLAPSED_SIZE, within(EXACT_SIZE_TOLERANCE));
	}

	/**
	 * An uncollapsed leaf must not have the persisted value forced onto it. Its
	 * size belongs to the live layout, and its tracking properties are bound, so
	 * the restorer is expected to leave it alone. Without this the previous test
	 * would also pass against an implementation that wrote the size
	 * unconditionally and happened to get away with it.
	 */
	@Test
	void restoredUncollapsedLeafTracksItsLiveSize(final FxRobot robot) {
		final DockContainerLeaf openLeaf =
				restoreAndFindLeaf(robot, OPEN_LEAF_ID);

		assertThat(openLeaf.isCollapsed())
				.describedAs("openLeaf.isCollapsed()")
				.isFalse();
		assertThat(openLeaf.getUncollapsedSize())
				.describedAs("openLeaf.getUncollapsedSize()")
				.isNotCloseTo(
						PERSISTED_UNCOLLAPSED_SIZE,
						within(EXACT_SIZE_TOLERANCE)
				);
	}

	/**
	 * Guards the ordering directly. Expanding the restored leaf must take it to
	 * the persisted width, which is only true if the size survived the collapse.
	 * An implementation that applied the size before collapsing would lose it and
	 * fail here even though the leaf was correctly collapsed.
	 */
	@Test
	void expandingRestoredCollapsedLeafReturnsToPersistedSize(
			final FxRobot robot
	) {
		final DockContainerLeaf collapsedLeaf =
				restoreAndFindLeaf(robot, COLLAPSED_LEAF_ID);

		final AtomicReference<Double> widthAfterExpanding =
				new AtomicReference<>();

		robot.interact(() -> {
			final DockContainerBranch parent = requireNonNull(
					collapsedLeaf.getParentContainer(),
					"collapsedLeaf.getParentContainer()"
			);
			parent.setContainerCollapsed(collapsedLeaf, false);
		});

		robot.interact(() ->
				widthAfterExpanding.set(collapsedLeaf.getWidth())
		);

		assertThat(collapsedLeaf.isCollapsed())
				.describedAs("collapsedLeaf.isCollapsed() after expanding")
				.isFalse();
		assertThat(widthAfterExpanding.get())
				.describedAs("collapsedLeaf.getWidth() after expanding")
				.isCloseTo(
						PERSISTED_UNCOLLAPSED_SIZE,
						within(RENDERED_SIZE_TOLERANCE)
				);
	}

	/**
	 * Restores the layout onto a shown stage and returns the requested leaf. The
	 * stage is left open: these tests read live geometry, and closing it would
	 * detach the tree the assertions inspect.
	 */
	private static DockContainerLeaf restoreAndFindLeaf(
			final FxRobot robot,
			final String leafIdentifier
	) {
		final AtomicReference<@Nullable DockContainerLeaf> leafReference =
				new AtomicReference<>();

		robot.interact(() -> {
			final DockingLayout dockingLayout = new DefaultDockingLayoutRebuilder(
					new DefaultBentoProvider(new Bento(BENTO_ID)),
					LeafUncollapsedSizeRoundTripITG::findDockableState,
					null,
					null
			).rebuild(List.of(persistedBentoState()));

			final DockContainerRootBranch rootBranch = dockingLayout
					.getBentoLayouts()
					.getFirst()
					.getRootBranches()
					.getFirst();

			final Stage stage = new Stage();
			stage.setScene(new Scene(rootBranch, 800, 600));
			stage.show();

			leafReference.set(findLeaf(rootBranch, leafIdentifier));
		});

		// Collapsing and divider positioning are deferred onto the JavaFX queue.
		// Two fences: the first lets the restorer's queued work run, the second
		// lets any layout pass it triggered settle.
		robot.interact(() -> { /* fence */ });
		robot.interact(() -> { /* fence */ });

		return requireNonNull(
				leafReference.get(),
				"leaf " + leafIdentifier
		);
	}

	/**
	 * Builds root -> (collapsed leaf, open leaf) directly under the root branch,
	 * both carrying the persisted uncollapsed size. Two leaves so a divider exists
	 * for the collapse to move, the collapsing one first, sides set and compatible
	 * with the horizontal orientation, and dockables present so a header pane
	 * exists for the collapsed size to be measured from - all preconditions
	 * {@code setContainerCollapsed} imposes.
	 */
	private static BentoState persistedBentoState() {
		final DockContainerLeafStateBuilder collapsedLeafBuilder =
				new DockContainerLeafStateBuilder(COLLAPSED_LEAF_ID);
		collapsedLeafBuilder.setSide(LEFT);
		collapsedLeafBuilder.setCollapsed(true);
		collapsedLeafBuilder.setUncollapsedSizePx(PERSISTED_UNCOLLAPSED_SIZE);
		collapsedLeafBuilder.addChildDockableState(
				dockableState(COLLAPSED_DOCKABLE_ID)
		);

		final DockContainerLeafStateBuilder openLeafBuilder =
				new DockContainerLeafStateBuilder(OPEN_LEAF_ID);
		openLeafBuilder.setSide(LEFT);
		openLeafBuilder.setCollapsed(false);
		// Deliberately also carries the persisted size. An uncollapsed leaf must
		// ignore it rather than adopt it.
		openLeafBuilder.setUncollapsedSizePx(PERSISTED_UNCOLLAPSED_SIZE);
		openLeafBuilder.addChildDockableState(
				dockableState(OPEN_DOCKABLE_ID)
		);

		final DockContainerRootBranchStateBuilder rootBranchBuilder =
				new DockContainerRootBranchStateBuilder(ROOT_BRANCH_ID);
		rootBranchBuilder.setOrientation(HORIZONTAL);
		rootBranchBuilder.addDockContainerState(collapsedLeafBuilder.build());
		rootBranchBuilder.addDockContainerState(openLeafBuilder.build());

		return new BentoStateBuilder(BENTO_ID)
				.addRootBranchState(rootBranchBuilder.build())
				.build();
	}

	private static DockableState dockableState(final String identifier) {
		return new DockableStateBuilder(identifier)
				.setTitle(identifier)
				.build();
	}

	private static Optional<DockableState> findDockableState(
			final String identifier
	) {
		if (COLLAPSED_DOCKABLE_ID.equals(identifier)
				|| OPEN_DOCKABLE_ID.equals(identifier)) {
			return Optional.of(dockableState(identifier));
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
}
