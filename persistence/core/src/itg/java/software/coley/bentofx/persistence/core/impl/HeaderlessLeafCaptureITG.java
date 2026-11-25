package software.coley.bentofx.persistence.core.impl;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import software.coley.bentofx.Bento;
import software.coley.bentofx.building.DockBuilding;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState;
import software.coley.bentofx.persistence.core.api.state.DockContainerState;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static javafx.geometry.Side.LEFT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage for capturing a leaf whose side is {@code null}.
 *
 * <p>A leaf's side is legitimately nullable: {@code setSide(@Nullable Side)} is
 * public and documented as "{@code null} to not display any headers". But core's
 * {@code getUncollapsedSize()} switches on the side and throws
 * {@code IllegalStateException} for the null case, and the captor called it
 * unconditionally. That throw originates outside the per-dockable guard in
 * {@code buildLeafState}, so it propagated out of {@code captureBentoStates} and
 * aborted the save for <em>every</em> Bento, not merely the offending leaf.</p>
 *
 * <p>The exception message - "Container with null side should not be collapsed" -
 * made this harder to recognize than it should have been: the container need not be
 * collapsed for it to fire, and in the case below it is not.</p>
 *
 * @author Phil Bryant
 */
@ExtendWith(ApplicationExtension.class)
class HeaderlessLeafCaptureITG {

	private static final String BENTO_ID = "bento-headerless";
	private static final String ROOT_BRANCH_ID = "root-headerless";
	private static final String HEADERLESS_LEAF_ID = "leaf-headerless";
	private static final String SIDED_LEAF_ID = "leaf-sided";
	private static final String HEADERLESS_DOCKABLE_ID = "dockable-headerless";
	private static final String SIDED_DOCKABLE_ID = "dockable-sided";

	/**
	 * The defect. A single headerless leaf must not stop the save. Before the fix
	 * this threw {@code IllegalStateException} out of {@code captureBentoStates}.
	 */
	@Test
	void captureSucceedsWithHeaderlessLeaf(final FxRobot robot) {
		final Captured captured = capture(robot);

		assertThat(captured.states())
				.describedAs("captured bento states")
				.hasSize(1);
		assertThat(captured.states().getFirst().getRootBranchStates())
				.describedAs("captured root branch states")
				.hasSize(1);
	}

	/**
	 * The headerless leaf is still captured - it is skipped only for the one
	 * property core cannot supply, not dropped from the layout.
	 */
	@Test
	void headerlessLeafIsStillCapturedWithoutItsUncollapsedSize(
			final FxRobot robot
	) {
		final DockContainerLeafState leafState =
				leafState(capture(robot), HEADERLESS_LEAF_ID);

		assertThat(leafState.getSide())
				.describedAs("headerless leafState.getSide()")
				.isEmpty();
		assertThat(leafState.getUncollapsedSizePx())
				.describedAs("headerless leafState.getUncollapsedSizePx()")
				.isEmpty();
		assertThat(leafState.getChildDockableStates())
				.describedAs("headerless leafState.getChildDockableStates()")
				.hasSize(1);
	}

	/**
	 * Its sided sibling must still record a size. Without this the fix could have
	 * been "never capture the uncollapsed size" and the other tests would not have
	 * noticed - which would silently undo B7.
	 */
	@Test
	void sidedLeafStillCapturesItsUncollapsedSize(final FxRobot robot) {
		final DockContainerLeafState leafState =
				leafState(capture(robot), SIDED_LEAF_ID);

		assertThat(leafState.getSide())
				.describedAs("sided leafState.getSide()")
				.contains(LEFT);
		assertThat(leafState.getUncollapsedSizePx())
				.describedAs("sided leafState.getUncollapsedSizePx()")
				.isPresent();
	}

	/**
	 * Builds root -> (headerless leaf, sided leaf) on a shown stage and captures.
	 * Both leaves carry a dockable so the tree is realistic; the stage is shown
	 * because capture reads live geometry.
	 */
	private static Captured capture(final FxRobot robot) {
		final AtomicReference<List<BentoState>> captured = new AtomicReference<>();
		final AtomicReference<Stage> stageRef = new AtomicReference<>();

		robot.interact(() -> {
			final Bento bento = new Bento(BENTO_ID);
			final DockBuilding dockBuilding = bento.dockBuilding();

			final DockContainerRootBranch rootBranch =
					dockBuilding.root(ROOT_BRANCH_ID);

			// The hazard: side explicitly null, which core documents as "no
			// headers" and which getUncollapsedSize() cannot answer for.
			final DockContainerLeaf headerless =
					dockBuilding.leaf(HEADERLESS_LEAF_ID);
			headerless.setSide(null);
			addDockable(dockBuilding, headerless, HEADERLESS_DOCKABLE_ID);

			final DockContainerLeaf sided = dockBuilding.leaf(SIDED_LEAF_ID);
			sided.setSide(LEFT);
			addDockable(dockBuilding, sided, SIDED_DOCKABLE_ID);

			rootBranch.addContainer(headerless);
			rootBranch.addContainer(sided);

			final Stage stage = new Stage();
			stage.setScene(new Scene(rootBranch, 800, 600));
			stage.show();
			stageRef.set(stage);

			captured.set(
					new DefaultBentoStateCapturer(new DefaultBentoProvider(bento))
							.capture()
			);
		});

		robot.interact(() -> stageRef.get().close());

		return new Captured(captured.get());
	}

	private static void addDockable(
			final DockBuilding dockBuilding,
			final DockContainerLeaf leaf,
			final String dockableIdentifier
	) {
		final Dockable dockable = dockBuilding.dockable(dockableIdentifier);
		leaf.addDockable(dockable);
		leaf.selectDockable(dockable);
	}

	private static DockContainerLeafState leafState(
			final Captured captured,
			final String leafIdentifier
	) {
		final DockContainerRootBranchState rootState =
				captured.states().getFirst().getRootBranchStates().getFirst();

		return rootState.getChildDockContainerStates().stream()
				.filter(DockContainerLeafState.class::isInstance)
				.map(DockContainerLeafState.class::cast)
				.filter(state -> leafIdentifier.equals(state.getIdentifier()))
				.findFirst()
				.orElseThrow(() -> new AssertionError(
						"No captured leaf state for " + leafIdentifier
								+ "; captured: "
								+ rootState.getChildDockContainerStates().stream()
								.map(DockContainerState::getIdentifier)
								.toList()
				));
	}

	private record Captured(List<BentoState> states) {
	}
}
