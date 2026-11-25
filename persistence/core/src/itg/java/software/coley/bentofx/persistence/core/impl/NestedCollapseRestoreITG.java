package software.coley.bentofx.persistence.core.impl;

import javafx.scene.Scene;
import javafx.scene.control.Label;
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
import software.coley.bentofx.persistence.core.api.state.DockContainerBranchState.DockContainerBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState.DockContainerLeafStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState.DockContainerRootBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockableState;
import software.coley.bentofx.persistence.core.api.state.DockableState.DockableStateBuilder;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Side.LEFT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage for collapsed state being restored at every depth of the
 * container tree, not only directly beneath the root branch.
 * <p>{@code restoreRootBranchContainer} always collapsed its own direct child
 * leaves, but {@code restoreBranch} - the method every branch below the root is
 * built by - did not. A layout shaped root-to-branch-to-leaf, which is what a
 * user produces as soon as they split a pane, therefore captured collapsed state
 * correctly, wrote it, read it back, and then silently dropped it.</p>
 *
 * <p>These tests deliberately satisfy every precondition
 * {@code DockContainerBranch.setContainerCollapsed} imposes, because failing any
 * one of them makes it return {@code false} without acting - which would let the
 * test pass whether the restorer even attempted the collapse. The branch holds
 * two leaves so there is a divider to move, the collapsing leaf is first, its
 * side is set, that side agrees with the branch orientation, and the leaves
 * carry dockables so a header pane exists for the collapsed size to be measured
 * from. The containers are also attached to a shown {@code Stage}: collapsing
 * reads laid-out divider geometry, and core defers the work when there is no
 * {@code Scene}.</p>
 *
 * @author Phil Bryant
 */
@ExtendWith(ApplicationExtension.class)
class NestedCollapseRestoreITG {

	private static final String BENTO_ID = "bento-nested-collapse";
	private static final String ROOT_BRANCH_ID = "root-nested-collapse";
	private static final String NESTED_BRANCH_ID = "branch-nested-collapse";
	private static final String COLLAPSED_LEAF_ID = "leaf-collapsed-nested";
	private static final String OPEN_LEAF_ID = "leaf-open-nested";
	private static final String COLLAPSED_DOCKABLE_ID = "dockable-collapsed-nested";
	private static final String OPEN_DOCKABLE_ID = "dockable-open-nested";

	/**
	 * A leaf nested one level below the root must come back collapsed. This is
	 * the case that regressed: before the fix the restored leaf was always
	 * expanded.
	 */
	@Test
	void restoreCollapsesLeafNestedBelowRootBranch(final FxRobot robot) {
		final DockContainerLeaf collapsedLeaf = restoreAndFindLeaf(
				robot,
				COLLAPSED_LEAF_ID
		);

		assertThat(collapsedLeaf.isCollapsed())
				.describedAs("nested collapsedLeaf.isCollapsed()")
				.isTrue();
	}

	/**
	 * The sibling leaf, whose state says it is not collapsed, must come back
	 * expanded. Without this the previous test would also pass against a
	 * hypothetical fix that collapsed every nested leaf indiscriminately.
	 */
	@Test
	void restoreLeavesUncollapsedSiblingExpanded(final FxRobot robot) {
		final DockContainerLeaf openLeaf = restoreAndFindLeaf(
				robot,
				OPEN_LEAF_ID
		);

		assertThat(openLeaf.isCollapsed())
				.describedAs("nested openLeaf.isCollapsed()")
				.isFalse();
	}

	/**
	 * Restores the layout, attaches it to a shown stage so the collapse has laid
	 * out geometry to work with, and returns the nested leaf with the requested
	 * identifier.
	 */
	private static DockContainerLeaf restoreAndFindLeaf(
			final FxRobot robot,
			final String leafIdentifier
	) {
		final AtomicReference<@Nullable DockContainerLeaf> leafReference =
				new AtomicReference<>();
		final AtomicReference<Stage> stageReference = new AtomicReference<>();

		robot.interact(() -> {
			final Map<String, DockableState> dockableStates = Map.of(
					COLLAPSED_DOCKABLE_ID,
					createDockableState(COLLAPSED_DOCKABLE_ID),
					OPEN_DOCKABLE_ID,
					createDockableState(OPEN_DOCKABLE_ID)
			);

			final DockingLayout dockingLayout =
					new DefaultDockingLayoutRebuilder(
							new DefaultBentoProvider(new Bento(BENTO_ID)),
							id -> Optional.ofNullable(dockableStates.get(id)),
							null,
							null
					).rebuild(List.of(createNestedBentoState()));

			final DockContainerRootBranch rootBranch = dockingLayout
					.getBentoLayouts()
					.getFirst()
					.getRootBranches()
					.getFirst();

			// Collapsing consults divider geometry, and core queues the work when
			// the container has no Scene. Show the tree so the collapse applies.
			final Stage stage = new Stage();
			stage.setScene(new Scene(rootBranch, 800, 600));
			stage.show();
			stageReference.set(stage);

			leafReference.set(findNestedLeaf(rootBranch, leafIdentifier));
		});

		// The restorer defers divider and collapse work onto the JavaFX queue.
		// A no-op interact() round trip lets that queued work finish before the
		// assertion reads the leaf.
		robot.interact(() -> { /* fence */ });

		try {
			return requireNonNull(
					leafReference.get(),
					"nested leaf " + leafIdentifier
			);
		} finally {
			robot.interact(() -> stageReference.get().close());
		}
	}

	/**
	 * Builds root -> branch -> (collapsed leaf, open leaf). The nested branch is
	 * horizontal and both leaves sit on the left so the collapse is compatible
	 * with the split orientation.
	 */
	private static BentoState createNestedBentoState() {
		final DockContainerLeafStateBuilder collapsedLeafBuilder =
				new DockContainerLeafStateBuilder(COLLAPSED_LEAF_ID);
		collapsedLeafBuilder.setSide(LEFT);
		collapsedLeafBuilder.setCollapsed(true);
		collapsedLeafBuilder.addChildDockableState(
				createDockableState(COLLAPSED_DOCKABLE_ID)
		);

		final DockContainerLeafStateBuilder openLeafBuilder =
				new DockContainerLeafStateBuilder(OPEN_LEAF_ID);
		openLeafBuilder.setSide(LEFT);
		openLeafBuilder.setCollapsed(false);
		openLeafBuilder.addChildDockableState(
				createDockableState(OPEN_DOCKABLE_ID)
		);

		final DockContainerBranchStateBuilder nestedBranchBuilder =
				new DockContainerBranchStateBuilder(NESTED_BRANCH_ID);
		nestedBranchBuilder.setOrientation(HORIZONTAL);
		nestedBranchBuilder.addDockContainerState(collapsedLeafBuilder.build());
		nestedBranchBuilder.addDockContainerState(openLeafBuilder.build());

		final DockContainerRootBranchStateBuilder rootBranchBuilder =
				new DockContainerRootBranchStateBuilder(ROOT_BRANCH_ID);
		rootBranchBuilder.setOrientation(HORIZONTAL);
		rootBranchBuilder.addDockContainerState(nestedBranchBuilder.build());

		return new BentoStateBuilder(BENTO_ID)
				.addRootBranchState(rootBranchBuilder.build())
				.build();
	}

	private static DockableState createDockableState(final String identifier) {
		return new DockableStateBuilder(identifier)
				.setTitle(identifier)
				.setDockableNode(new Label(identifier))
				.build();
	}

	/**
	 * Depth-first search for a leaf, so the test does not assume how deeply the
	 * restorer nested it.
	 */
	private static @Nullable DockContainerLeaf findNestedLeaf(
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
						findNestedLeaf(childBranch, leafIdentifier);
				if (found != null) {
					return found;
				}
			}
		}

		return null;
	}
}
