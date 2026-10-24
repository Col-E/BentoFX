package software.coley.bentofx.persistence.core.api;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import software.coley.bentofx.Bento;
import software.coley.bentofx.building.DockBuilding;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.provider.DockableStateProvider;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the public {@link BentoStateCapturer} and
 * {@link DockingLayoutRebuilder} interfaces together, specifically the API a
 * custom {@code DockingLayoutPersistenceProvider} typically uses: static
 * factory, then {@code capture()} and {@code rebuild(List)}, with no dependency
 * on the framework's codec or storage.
 *
 * <p>Internal round-trip coverage already lives elsewhere; this test only
 * proves the two public interfaces expose the full capability a
 * {@code DockingLayoutPersistenceProvider} needs, reachable through their
 * public interfaces.</p>
 *
 * @author Phil Bryant
 */
@ExtendWith(ApplicationExtension.class)
class CaptureAndRebuildBentoStateITG {

	private static final String BENTO_ID = "bento-capture-rebuild";
	private static final String ROOT_BRANCH_ID = "root-capture-rebuild";
	private static final String LEAF_ID = "leaf-capture-rebuild";
	private static final String DOCKABLE_ID = "dockable-capture-rebuild";
	private static final String DOCKABLE_TITLE = "Captured and Rebuilt";
	private static final String STAGE_TITLE = "Capture and Rebuild";

	private @Nullable Stage stage;

	@SuppressWarnings("unused") // invoked reflectively
	@Start
	void start(final Stage stage) {
		this.stage = stage;
	}

	/**
	 * A {@code DockingLayoutPersistenceProvider}'s save-restore cycle in
	 * miniature. The capture and the rebuild come from the public factories
	 * supplying the default implementations.
	 */
	@Test
	void capturedStateRebuildsIntoAnEquivalentLayout(final FxRobot robot) {
		final DefaultBentoProvider bentoProvider = new DefaultBentoProvider();
		final AtomicReference<List<BentoState>> captured = new AtomicReference<>();
		final AtomicReference<DockingLayout> rebuilt = new AtomicReference<>();
		final Stage primaryStage = getStage();

		robot.interact(() -> {
			final Bento bento = new Bento(BENTO_ID);
			final DockBuilding dockBuilding = bento.dockBuilding();

			final DockContainerRootBranch rootBranch =
					dockBuilding.root(ROOT_BRANCH_ID);
			rootBranch.setOrientation(HORIZONTAL);

			final DockContainerLeaf leaf = dockBuilding.leaf(LEAF_ID);
			final Dockable dockable = dockBuilding.dockable(DOCKABLE_ID);
			dockable.setTitle(DOCKABLE_TITLE);
			leaf.addDockable(dockable);
			leaf.selectDockable(dockable);

			rootBranch.addContainer(leaf);
			bentoProvider.addBento(bento);

			primaryStage.setTitle(STAGE_TITLE);
			primaryStage.setScene(new Scene(rootBranch));
			primaryStage.show();

			final BentoStateCapturer capture =
					BentoStateCapturer.create(bentoProvider);
			captured.set(capture.capture());

			final DockingLayoutRebuilder rebuilder = DockingLayoutRebuilder.create(
					bentoProvider,
					id -> Optional.of(
							new DockableStateBuilder(id)
									.setTitle(DOCKABLE_TITLE)
									.build()
					),
					null,
					null
			);
			rebuilt.set(rebuilder.rebuild(captured.get()));
		});

		assertThat(captured.get())
				.describedAs("captured state list")
				.singleElement()
				.satisfies(bentoState -> {
					assertThat(bentoState.getIdentifier())
							.describedAs("captured Bento identifier")
							.isEqualTo(BENTO_ID);
					assertThat(bentoState.getRootBranchStates())
							.describedAs("captured root branches")
							.singleElement()
							.satisfies(root -> {
								assertThat(root.getIdentifier())
										.describedAs("captured root identifier")
										.isEqualTo(ROOT_BRANCH_ID);
								assertThat(root.getChildDockContainerStates())
										.singleElement()
										.isInstanceOfSatisfying(
												DockContainerLeafState.class,
												leafState ->
														assertThat(leafState.getChildDockableStates())
																.extracting(DockableState::getIdentifier)
																.containsExactly(DOCKABLE_ID)
										);
							});
				});

		final DockingLayout rebuiltLayout = requireNonNull(rebuilt.get());
		assertThat(rebuiltLayout.getBentoLayouts())
				.describedAs("rebuilt Bento layouts")
				.singleElement()
				.satisfies(bentoLayout -> {
					assertThat(bentoLayout.getIdentifier())
							.describedAs("rebuilt Bento identifier")
							.isEqualTo(BENTO_ID);
					assertThat(bentoLayout.getRootBranches())
							.describedAs("rebuilt root branches")
							.singleElement()
							.satisfies(root -> {
								assertThat(root.getIdentifier())
										.describedAs("rebuilt root identifier")
										.isEqualTo(ROOT_BRANCH_ID);
								assertThat(root.getDockables())
										.describedAs("rebuilt dockables")
										.extracting(Dockable::getIdentifier)
										.containsExactly(DOCKABLE_ID);
							});
				});
	}

	/**
	 * {@link BentoStateCapturer#create(BentoProvider)} rejects a null provider,
	 * as its contract requires. The matching check on
	 * {@code DockingLayoutRebuilder#create} is covered by its symmetric null
	 * parameters below.
	 */
	@Test
	// Testing for null
	@SuppressWarnings("all")
	void captureFactoryRejectsNullProvider() {

		assertThatThrownBy(() -> BentoStateCapturer.create(null))
				.describedAs("NPE thrown for null BentoProvider")
				.isInstanceOf(NullPointerException.class);
	}

	/**
	 * {@link DockingLayoutRebuilder#create} rejects a null required provider on
	 * each of its mandatory arguments. The nullable arguments are tolerated.
	 */
	@Test
	// Testing for null
	@SuppressWarnings("all")
	void rebuilderFactoryRejectsNullRequiredProviders() {
		final DefaultBentoProvider bentoProvider = new DefaultBentoProvider();
		final DockableStateProvider dockableStateProvider =
				id -> Optional.empty();

		assertThatThrownBy(() ->
				DockingLayoutRebuilder.create(
						null,
						dockableStateProvider,
						null,
						null
				)
		)
				.describedAs("NPE for null BentoProvider")
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() ->
				DockingLayoutRebuilder.create(
						bentoProvider,
						null,
						null,
						null
				)
		)
				.describedAs("NPE for null DockableStateProvider")
				.isInstanceOf(NullPointerException.class);
	}

	private Stage getStage() {
		return requireNonNull(
				stage,
				"FX stage was not provided by TestFX"
		);
	}
}
