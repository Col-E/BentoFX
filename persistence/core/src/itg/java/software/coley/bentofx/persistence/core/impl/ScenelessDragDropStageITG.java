package software.coley.bentofx.persistence.core.impl;

import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import software.coley.bentofx.Bento;
import software.coley.bentofx.building.DockBuilding;
import software.coley.bentofx.control.DragDropStage;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.persistence.core.api.BentoLayout;
import software.coley.bentofx.persistence.core.api.DockingLayout;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.BentoState.BentoStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState.DockContainerRootBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DragDropStageState.DragDropStageStateBuilder;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Coverage for scene-less {@link DragDropStage} handling on both sides of
 * persistence.
 *
 * <p>Two defects met here. The captor dereferenced {@code stage.getScene()}
 * without checking it, and a {@link Stage} has no scene until one is assigned.
 * Worse, the restorer could <em>produce</em> exactly that: it only called
 * {@code setScene} inside an {@code ifPresent} on the persisted root branch
 * state, so state with no root branch yielded a live, scene-less stage handed back
 * to the caller. The next save walked it and threw, and because that throw happens
 * outside the per-dockable guard it aborted the entire save - every Bento, not just
 * the offending stage.</p>
 *
 * <p>The two are tested separately because either could be fixed without the
 * other, and each is a real hazard alone: a scene-less stage can also reach core's
 * own {@code WINDOW_HIDDEN}/{@code WINDOW_SHOWN} filters on {@code DragDropStage},
 * which dereference the scene too, as soon as anything shows it.</p>
 *
 * @author Phil Bryant
 */
@ExtendWith(ApplicationExtension.class)
class ScenelessDragDropStageITG {

	private static final String BENTO_ID = "bento-scene-less";
	private static final String ROOT_BRANCH_ID = "root-scene-less";
	private static final String LEAF_ID = "leaf-scene-less";
	private static final String DOCKABLE_ID = "dockable-scene-less";
	private static final String STAGE_TITLE = "Scene-less Stage";

	/**
	 * The captor half. A scene-less {@link DragDropStage} in the window list must
	 * not stop the save. Before the fix this threw {@code NullPointerException} out
	 * of {@code captureBentoStates}, taking every Bento's save with it.
	 *
	 * <p>The stage has to be <em>shown</em> to matter: {@code Window.getWindows()}
	 * lists only showing windows, so an unshown stage never reaches the captor and
	 * a test using one passes whether the guard exists or not. Showing a scene-less
	 * stage is exactly what a caller does with a restored layout, so this is the
	 * realistic arrangement as well as the reachable one.</p>
	 */
	@Test
	void captureIgnoresScenelessDragDropStage(final FxRobot robot) {
		final AtomicReference<@Nullable List<BentoState>> bentoStateReference =
				new AtomicReference<>();
		final AtomicReference<@Nullable Bento> bentoReference =
				new AtomicReference<>();
		final AtomicReference<@Nullable DragDropStage> scenelessStage =
				new AtomicReference<>();
		final AtomicReference<@Nullable Stage> hostStage = new AtomicReference<>();

		try {
			robot.interact(() -> {
				final Bento bento = new Bento(BENTO_ID);
				final DockBuilding dockBuilding = bento.dockBuilding();

				// A normal, capturable root branch on its own stage, so the test
				// can show that the good content still gets bentoStateReference.
				final DockContainerRootBranch rootBranch =
						dockBuilding.root(ROOT_BRANCH_ID);
				final DockContainerLeaf leaf = dockBuilding.leaf(LEAF_ID);
				final Dockable dockable = dockBuilding.dockable(DOCKABLE_ID);
				leaf.addDockable(dockable);
				rootBranch.addContainer(leaf);
				bentoReference.set(bento);

				final Stage host = new Stage();
				host.setScene(new Scene(rootBranch, 400, 300));
				host.show();
				hostStage.set(host);

				// The hazard: a showing DragDropStage whose getScene() is null, so
				// that it appears in Window.getWindows() and the captor walks it.
				//
				// Shown WITH a scene and detached in a later FX runnable, rather
				// than shown scene-less. Both arrangements put the same stage in
				// front of the captor - showing, getScene() null - but this one
				// never asks the platform to map a window that has no view
				// attached. Mapping a view-less window is toolkit-dependent, and a
				// stall inside it wedges the run: FxRobot.interact waits on the FX
				// thread with no timeout, so the test never fails, it hangs.
				final DragDropStage stage = new DragDropStage(true);
				stage.setScene(new Scene(new Region(), 100, 100));
				stage.show();
				scenelessStage.set(stage);
			});

			// Every scene attach or detach on a showing stage gets its own
			// robot.interact, so that FxRobot pumps a pulse in between and each
			// scene renders once before the next change. Quantum sizes its render
			// latch by the number of dirty scenes, so a scene made dirty by show()
			// and then detached before it renders leaves a count that nothing will
			// ever decrement. The FX thread then parks in
			// PaintCollector.waitForRenderingToComplete on the next synchronous
			// render - in this test or, because the damage outlives it, in any
			// later one. Under Monocle (-Pheadless=true) that hung the build
			// rather than failing it; the same code passes on a real display,
			// which is why CI's xvfb run never saw it.
			robot.interact(() -> {

				// Use Objects.requireNotNull instead of AssertJ because Nullaway
				// doesn't recognize AssertJ assertions.
				final DragDropStage stage = Objects.requireNonNull(
						scenelessStage.get(),
						"stage is null"
				);

				stage.setScene(null);

				// Without these two the test passes whether the guard exists or
				// not: a stage the captor never walks cannot exercise it. They
				// assert the arrangement, not the behaviour under test.
				assertThat(Window.getWindows())
						.describedAs("windows the captor will walk")
						.contains(stage);
				assertThat(stage.getScene())
						.describedAs("scene of the stage the captor will walk")
						.isNull();

				final Bento bento = bentoReference.get();

				assertThat(bento)
						.describedAs("bento")
						.isNotNull();

				bentoStateReference.set(
						new DefaultBentoStateCapturer(
								new DefaultBentoProvider(bento)
						).capture()
				);
			});

			// Use Objects.requireNotNull instead of AssertJ because Nullaway
			// doesn't recognize AssertJ assertions.
			final List<BentoState> bentoStateList =
					Objects.requireNonNull(
							bentoStateReference.get(),
							"bentoStateList is null"
					);

			assertThat(bentoStateList)
					.describedAs("bentoStateList")
					.isNotNull();

			// Use Objects.requireNotNull instead of AssertJ because Nullaway
			// doesn't recognize AssertJ assertions.
			final BentoState bentoState = Objects.requireNonNull(
					bentoStateList.getFirst(),
					"bentoState is null"
			);

			assertThat(bentoState.getDragDropStageStates())
					.describedAs("drag/drop stage states from a scene-less stage")
					.isEmpty();
			assertThat(bentoState.getRootBranchStates())
					.describedAs("root branch states bentoStateReference alongside it")
					.hasSize(1);
		} finally {
			robot.interact(() -> {
				// Attach a scene before closing, so core's WINDOW_HIDDEN filter
				// has something to dereference. Without this the stage survives
				// the test and throws during a later teardown, failing an
				// unrelated test.
				final DragDropStage stage = scenelessStage.get();
				if (stage != null && stage.getScene() == null) {
					stage.setScene(new Scene(new Region(), 100, 100));
				}
			});

			// Separate runnable, for the same reason as the detach above: the
			// scene just attached has to render before the window goes away.
			robot.interact(() -> {
				final DragDropStage stage = scenelessStage.get();
				if (stage != null) {
					stage.close();
				}

				final Stage host = hostStage.get();
				if (host != null) {
					host.close();
				}
			});
		}
	}

	/**
	 * The restorer half, and the root cause. Persisted stage state with no root
	 * branch must still yield a stage that owns a scene, because the caller adds
	 * every returned stage to the layout and typically shows it.
	 */
	@Test
	void restoreGivesSceneToStageWithoutPersistedRootBranch(final FxRobot robot) {
		final AtomicReference<@Nullable DockingLayout> restoredLayoutReference = new AtomicReference<>();

		robot.interact(() ->
				restoredLayoutReference.set(restoreLayout(stateWithStageMissingRootBranch()))
		);

		// Use Objects.requireNotNull instead of AssertJ because Nullaway
		// doesn't recognize AssertJ assertions.
		final DockingLayout restoredLayout = Objects.requireNonNull(
				restoredLayoutReference.get(),
				"restoredLayout is null"
		);

		final BentoLayout bentoLayout =
				restoredLayout.getBentoLayouts().getFirst();

		assertThat(bentoLayout.getDragDropStages())
				.describedAs("restoredLayoutReference drag/drop stages")
				.hasSize(1);

		final DragDropStage stage = bentoLayout.getDragDropStages().getFirst();

		assertThat(stage.getScene())
				.describedAs("scene of a stage restoredLayoutReference without a root branch")
				.isNotNull();
		assertThat(stage.getScene().getRoot())
				.describedAs("scene root of that stage")
				.isNotNull();
	}

	/**
	 * The two halves joined. Restoring stage state with no root branch and then
	 * capturing must not throw - this is the end-to-end shape of the original
	 * defect, where the module's own output fed back in and killed the next save.
	 */
	@Test
	void restoredStageWithoutRootBranchCanBeCapturedAgain(final FxRobot robot) {
		final AtomicReference<@Nullable Throwable> thrown = new AtomicReference<>();

		robot.interact(() -> {
			final Bento bento = new Bento(BENTO_ID + "-round-trip");

			try {
				restoreLayout(
						stateWithStageMissingRootBranch(),
						bento
				);

				new DefaultBentoStateCapturer(new DefaultBentoProvider(bento))
						.capture();
			} catch (final RuntimeException e) {
				thrown.set(e);
			}
		});

		assertThat(thrown.get())
				.describedAs("exception from capturing after restoring a "
						+ "root-branch-less stage")
				.isNull();
	}

	/**
	 * A scene-less stage must not break showing either. Core's
	 * {@code DragDropStage} filters dereference the scene on show and hide, so a
	 * stage this module hands back has to be safe to show - which is what callers
	 * do with every stage in the returned layout.
	 */
	@Test
	void restoredStageWithoutRootBranchIsSafeToShow(final FxRobot robot) {
		final AtomicReference<@Nullable DragDropStage> stageRef = new AtomicReference<>();

		robot.interact(() -> {
			final DockingLayout layout =
					restoreLayout(stateWithStageMissingRootBranch());
			stageRef.set(
					layout.getBentoLayouts()
							.getFirst()
							.getDragDropStages()
							.getFirst()
			);
		});

		// Use Objects.requireNotNull instead of AssertJ because Nullaway
		// doesn't recognize AssertJ assertions.
		final DragDropStage stage = Objects.requireNonNull(
				stageRef.get(),
				"stage is null"
		);

		assertThatCode(() -> {
			robot.interact(stage::show);
			robot.interact(stage::hide);
		})
				.describedAs("showing and hiding a stage restored without a root branch")
				.doesNotThrowAnyException();
	}

	/**
	 * Persisted state holding one drag/drop stage whose root branch state is
	 * absent - the case that produced a scene-less stage.
	 */
	private static BentoState stateWithStageMissingRootBranch() {
		return new BentoStateBuilder(BENTO_ID)
				.addRootBranchState(
						new DockContainerRootBranchStateBuilder(ROOT_BRANCH_ID)
								.build()
				)
				.addDragDropStageState(
						new DragDropStageStateBuilder(true)
								.setTitle(STAGE_TITLE)
								// setDockContainerRootBranchState deliberately not
								// called: this is the state under test.
								.build()
				)
				.build();
	}

	private static DockingLayout restoreLayout(final BentoState bentoState) {
		return restoreLayout(bentoState, new Bento(BENTO_ID));
	}

	/**
	 * Must run on the JavaFX application thread.
	 */
	private static DockingLayout restoreLayout(
			final BentoState bentoState,
			final Bento bento
	) {
		return new DefaultDockingLayoutRebuilder(
				new DefaultBentoProvider(bento),
				id -> Optional.empty(),
				null,
				null
		).rebuild(List.of(bentoState));
	}
}
