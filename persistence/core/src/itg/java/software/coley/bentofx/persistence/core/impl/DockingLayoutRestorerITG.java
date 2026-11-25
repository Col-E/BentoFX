package software.coley.bentofx.persistence.core.impl;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.image.WritableImage;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import software.coley.bentofx.Bento;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.persistence.core.api.BentoLayout;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.DockingLayout;
import software.coley.bentofx.persistence.core.api.DockingLayout.DockingLayoutBuilder;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.provider.StageIconImageProvider;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState.DockContainerLeafStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState.DockContainerRootBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockableState;
import software.coley.bentofx.persistence.core.api.state.DockableState.DockableStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DragDropStageState.DragDropStageStateBuilder;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;
import software.coley.bentofx.persistence.testfixtures.codec.InMemoryLayoutCodec;
import software.coley.bentofx.persistence.testfixtures.codec.ThreadRecordingLayoutCodec;
import software.coley.bentofx.persistence.testfixtures.storage.InMemoryLayoutStorage;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Side.LEFT;
import static javafx.stage.Modality.NONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(ApplicationExtension.class)
class DockingLayoutRestorerITG {

	private static final String CODEC_GET_DECODETHREAD_DESCRIPTION = "codec.getDecodeThread()";

	@Test
	void restoreLayoutReturnsDefaultWhenStorageDoesNotExist() {
		InMemoryLayoutStorage storage = new InMemoryLayoutStorage();
		InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		DockingLayout defaultLayout = new DockingLayoutBuilder().build();
		BentoProvider bentoProvider = new DefaultBentoProvider(new Bento());

		DockingLayoutRestorer layoutRestorer = new DockingLayoutRestorer(
				codec,
				storage,
				bentoProvider,
				id -> Optional.empty(),
				null,
				null
		);

		DockingLayout restoredLayout = layoutRestorer.restoreLayout(() ->
				defaultLayout
		);

		assertThat(restoredLayout)
				.describedAs("restoredLayout")
				.isSameAs(defaultLayout);
		assertThat(storage.exists())
				.describedAs("storage.exists()")
				.isFalse();
		assertThat(codec.getEncodeCalls())
				.describedAs("codec.getEncodeCalls()")
				.isEmpty();
	}

	// This test follows the "Single Act Rule" and only exercises the unit
	// under test (DockingLayoutRestorer) once. As such, we are suppressing the
	// warning for the number of assertions ("Single Assert Rule").
	@SuppressWarnings("java:S5961")
	@Test
	void restoreLayoutBuildsRootBranchesAndDragDropStages(FxRobot robot) throws Exception {

		// Dockable state
		final String expectedDockableId = "Dockable ID";
		final String expectedDockableTitle = "Dockable Title";
		final String expectedDockableTooltipText = "This is the Dockable's tooltip text";
		final boolean expectedDockableIsClosable = true;

		// Leaf state
		final String expectedLeafId = "Leaf ID";
		final String expectedSecondLeafId = "Second Leaf ID";
		final Side expectedLeafSide = LEFT;
		final boolean expectedLeafCanSplit = true;
		final boolean expectedLeafResizableWithParent = true;
		final double expectedLeafUncollapsedSizePx = 240.0;
		final boolean expectedLeafIsCollapsed = false;
		final boolean expectedLeafPruneWhenEmpty = false;

		// Bento state
		final String expectedBentoId = "Bento ID";
		final String expectedBentoRootId = "Bento Root ID";

		// DragDropStage root branch state
		final String expectedDragRootBuilderId = "DragDropStage Root Branch ID";
		final Orientation expectedDragRootOrientation = VERTICAL;
		final boolean expectedDragRootPruneWhenEmpty = true;

		// DragDropStage state
		final String expectedDragDropStageTitle = "DragDropStage ID";
		final double expectedX = 120.0;
		final double expectedY = 75.0;
		final double expectedWidth = 640.0;
		final double expectedHeight = 480.0;
		final double expectedOpacity = 0.8;
		final boolean expectedResizable = true;
		final boolean expectedAlwaysOnTop = false;
		final Modality expectedModality = NONE;

		// Primary root branch state
		final Orientation expectedRootOrientation = HORIZONTAL;
		final boolean expectedRootPruneWhenEmpty = true;
		final double expectedRootDividerPosition = 0.35;

		DockableState dockableState = new DockableStateBuilder(expectedDockableId)
				.setTitle(expectedDockableTitle)
				.setTooltipText(expectedDockableTooltipText)
				.setDockableNode(new Label(expectedDockableId))
				.setClosable(expectedDockableIsClosable)
				.build();

		DockContainerLeafStateBuilder leafBuilder =
				new DockContainerLeafStateBuilder(expectedLeafId);
		leafBuilder.setSide(expectedLeafSide);
		leafBuilder.setCanSplit(expectedLeafCanSplit);
		leafBuilder.setResizableWithParent(expectedLeafResizableWithParent);
		leafBuilder.setSelectedDockableStateIdentifier(expectedDockableId);
		leafBuilder.setUncollapsedSizePx(expectedLeafUncollapsedSizePx);
		leafBuilder.setCollapsed(expectedLeafIsCollapsed);
		leafBuilder.setPruneWhenEmpty(expectedLeafPruneWhenEmpty);
		leafBuilder.addChildDockableState(dockableState);

		DockContainerRootBranchStateBuilder rootBuilder =
				new DockContainerRootBranchStateBuilder(expectedBentoRootId);
		rootBuilder.setOrientation(expectedRootOrientation);
		rootBuilder.setPruneWhenEmpty(expectedRootPruneWhenEmpty);
		rootBuilder.addDividerPosition(0, expectedRootDividerPosition);
		rootBuilder.addDockContainerState(leafBuilder.build());
		rootBuilder.addDockContainerState(
				new DockContainerLeafStateBuilder(expectedSecondLeafId).build()
		);

		DockContainerRootBranchStateBuilder dragRootBuilder =
				new DockContainerRootBranchStateBuilder(expectedDragRootBuilderId);
		dragRootBuilder.setOrientation(expectedDragRootOrientation);
		dragRootBuilder.setPruneWhenEmpty(expectedDragRootPruneWhenEmpty);

		BentoState state = new BentoState.BentoStateBuilder(expectedBentoId)
				.addRootBranchState(rootBuilder.build())
				.addDragDropStageState(new DragDropStageStateBuilder(true)
						.setTitle(expectedDragDropStageTitle)
						.setX(expectedX)
						.setY(expectedY)
						.setWidth(expectedWidth)
						.setHeight(expectedHeight)
						.setOpacity(expectedOpacity)
						.setResizable(expectedResizable)
						.setAlwaysOnTop(expectedAlwaysOnTop)
						.setModality(expectedModality)
						.setDockContainerRootBranchState(dragRootBuilder.build())
						.build())
				.build();

		InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		InMemoryLayoutStorage storage = new InMemoryLayoutStorage();
		try (var out = storage.openOutputStream()) {
			codec.writeEncoded(List.of(state), out);
		}

		StageIconImageProvider stageIconImageProvider =
				() -> List.of(
						new WritableImage(1, 1)
				);

		DockingLayoutRestorer restorer = new DockingLayoutRestorer(
				codec,
				storage,
				new DefaultBentoProvider(new Bento(expectedBentoId)),
				actualId ->
						actualId.equals(expectedDockableId)
								? Optional.of(dockableState)
								: Optional.empty(),
				stageIconImageProvider,
				null
		);

		AtomicReference<DockingLayout> restoredReferenceToDockingLayout =
				new AtomicReference<>();

		robot.interact(() ->
				restoredReferenceToDockingLayout.set(
						restorer.restoreLayout(
								() ->
										new DockingLayoutBuilder().build()
						)
				)
		);

		DockingLayout dockingLayout = restoredReferenceToDockingLayout.get();
		assertThat(dockingLayout.getBentoLayouts())
				.describedAs("dockingLayout.getBentoLayouts()")
				.hasSize(1);

		BentoLayout bentoLayout = dockingLayout.getBentoLayouts().getFirst();
		assertThat(bentoLayout.getIdentifier())
				.describedAs("bentoLayout.getIdentifier()")
				.isEqualTo(expectedBentoId);
		assertThat(bentoLayout.getRootBranches())
				.describedAs("bentoLayout.getRootBranches()")
				.hasSize(1);
		assertThat(bentoLayout.getDragDropStages())
				.describedAs("bentoLayout.getDragDropStages()")
				.hasSize(1);

		DockContainerRootBranch root = bentoLayout.getRootBranches().getFirst();
		assertThat(root.getIdentifier())
				.describedAs("root.getIdentifier()")
				.isEqualTo(expectedBentoRootId);
		assertThat(root.getOrientation())
				.describedAs("root.getOrientation()")
				.isEqualTo(expectedRootOrientation);
		assertThat(root.doPruneWhenEmpty())
				.describedAs("root.doPruneWhenEmpty()")
				.isEqualTo(expectedRootPruneWhenEmpty);
		assertThat(root.getDividerPositions())
				.describedAs("root.getDividerPositions()")
				.containsExactly(expectedRootDividerPosition);

		List<DockContainer> rootContainers = root.getChildContainers();
		assertThat(rootContainers)
				.describedAs("rootContainers")
				.hasSize(2);

		DockContainer dockContainer = rootContainers.getFirst();
		assertThat(dockContainer)
				.describedAs("dockContainer")
				.isInstanceOf(DockContainerLeaf.class);
		assertThat(dockContainer.getIdentifier())
				.describedAs("dockContainer.getIdentifier()")
				.isEqualTo(expectedLeafId);

		final DockContainerLeaf leaf = (DockContainerLeaf) dockContainer;
		assertThat(leaf.getSide())
				.describedAs("leaf.getSide()")
				.isEqualTo(expectedLeafSide);
		assertThat(leaf.isCanSplit())
				.describedAs("leaf.isCanSplit()")
				.isEqualTo(expectedLeafCanSplit);
		assertThat(SplitPane.isResizableWithParent(leaf))
				.describedAs("SplitPane.isResizableWithParent(leaf)")
				.isEqualTo(expectedLeafResizableWithParent);
		assertThat(leaf.isCollapsed())
				.describedAs("leaf.isCollapsed()")
				.isEqualTo(expectedLeafIsCollapsed);
		assertThat(leaf.doPruneWhenEmpty())
				.describedAs("leaf.doPruneWhenEmpty()")
				.isEqualTo(expectedLeafPruneWhenEmpty);

		List<Dockable> dockables = dockContainer.getDockables();
		assertThat(dockables)
				.describedAs("dockables")
				.hasSize(1);
		assertThat(dockables.getFirst().getIdentifier())
				.describedAs("dockables.getFirst().getIdentifier()")
				.isEqualTo(expectedDockableId);
		assertThat(dockables.getFirst().getTitle())
				.describedAs("dockables.getFirst().getTitle()")
				.isEqualTo(expectedDockableTitle);
		assertThat(dockables.getFirst().getTooltip())
				.describedAs("dockables.getFirst().getTooltip()")
				.isNotNull();
		assertThat(
				requireNonNull(dockables.getFirst().getTooltip()).getText()
		)
				.describedAs("dockables.getFirst().getTooltip().getText()")
				.isEqualTo(expectedDockableTooltipText);
		assertThat(dockables.getFirst().isClosable())
				.describedAs("dockables.getFirst().isClosable()")
				.isEqualTo(expectedDockableIsClosable);

		Stage dragStage = bentoLayout.getDragDropStages().getFirst();
		assertThat(dragStage.getTitle())
				.describedAs("dragStage.getTitle()")
				.isEqualTo(expectedDragDropStageTitle);
		assertThat(dragStage.getX())
				.describedAs("dragStage.getX()")
				.isEqualTo(expectedX);
		assertThat(dragStage.getY())
				.describedAs("dragStage.getY()")
				.isEqualTo(expectedY);
		assertThat(dragStage.getWidth())
				.describedAs("dragStage.getWidth()")
				.isEqualTo(expectedWidth);
		assertThat(dragStage.getHeight())
				.describedAs("dragStage.getHeight()")
				.isEqualTo(expectedHeight);
		assertThat(dragStage.getOpacity())
				.describedAs("dragStage.getOpacity()")
				.isEqualTo(expectedOpacity);
		assertThat(dragStage.isResizable())
				.describedAs("dragStage.isResizable()")
				.isEqualTo(expectedResizable);
		assertThat(dragStage.isAlwaysOnTop())
				.describedAs("dragStage.isAlwaysOnTop()")
				.isEqualTo(expectedAlwaysOnTop);
		assertThat(dragStage.getModality())
				.describedAs("dragStage.getModality()")
				.isEqualTo(expectedModality);
		assertThat(dragStage.getIcons())
				.describedAs("dragStage.getIcons()")
				.hasSize(1);
		assertThat(dragStage.getScene().getRoot())
				.describedAs("dragStage.getScene().getRoot()")
				.isInstanceOf(DockContainerRootBranch.class);

		final DockContainerRootBranch rootBranch =
				(DockContainerRootBranch) dragStage.getScene().getRoot();
		assertThat(rootBranch.getIdentifier())
				.describedAs("rootBranch.getIdentifier()")
				.isEqualTo(expectedDragRootBuilderId);
		assertThat(rootBranch.getOrientation())
				.describedAs("rootBranch.getOrientation()")
				.isEqualTo(expectedDragRootOrientation);
		assertThat(rootBranch.doPruneWhenEmpty())
				.describedAs("rootBranch.doPruneWhenEmpty()")
				.isEqualTo(expectedDragRootPruneWhenEmpty);

		// Cleanup
		robot.interact(dragStage::hide);
	}

	@Test
	void restoreLayoutDecodesAwayFromFxThreadAndRestoresOnFxThread(FxRobot robot) throws Exception {
		final String bentoId = "bento-thread-test";
		final String rootId = "root-thread-test";
		final String leafId = "leaf-thread-test";
		final String dockableId = "dock-thread-test";

		final DockableState dockableState =
				new DockableStateBuilder(dockableId)
						.setDockableNode(new Label(dockableId))
						.build();

		final DockContainerLeafStateBuilder leafBuilder =
				new DockContainerLeafStateBuilder(leafId);
		leafBuilder.addChildDockableState(dockableState);

		final DockContainerRootBranchStateBuilder rootBuilder =
				new DockContainerRootBranchStateBuilder(rootId);
		rootBuilder.addDockContainerState(leafBuilder.build());

		final BentoState state = new BentoState.BentoStateBuilder(bentoId)
				.addRootBranchState(rootBuilder.build())
				.build();

		final ThreadRecordingLayoutCodec codec =
				new ThreadRecordingLayoutCodec();
		final InMemoryLayoutStorage storage = new InMemoryLayoutStorage();
		try (var out = storage.openOutputStream()) {
			codec.writeEncoded(List.of(state), out);
		}

		final AtomicBoolean providerRanOnFxThread = new AtomicBoolean();
		final AtomicReference<Thread> providerThread = new AtomicReference<>();

		final DockingLayoutRestorer restorer = new DockingLayoutRestorer(
				codec,
				storage,
				new DefaultBentoProvider(new Bento(bentoId)),
				actualId -> {
					providerRanOnFxThread.set(Platform.isFxApplicationThread());
					providerThread.set(Thread.currentThread());
					return actualId.equals(dockableId)
							? Optional.of(dockableState)
							: Optional.empty();
				},
				null,
				null
		);

		final AtomicReference<DockingLayout> restoredLayout =
				new AtomicReference<>();

		robot.interact(() ->
				restoredLayout.set(
						restorer.restoreLayout(
								() -> new DockingLayoutBuilder().build()
						)
				)
		);

		assertThat(codec.getDecodeThread())
				.describedAs(CODEC_GET_DECODETHREAD_DESCRIPTION)
				.isNotNull();
		assertThat(providerThread.get())
				.describedAs("providerThread.get()")
				.isNotNull();
		assertThat(codec.getDecodeThread())
				.describedAs(CODEC_GET_DECODETHREAD_DESCRIPTION)
				.isNotEqualTo(providerThread.get());
		assertThat(providerRanOnFxThread)
				.describedAs("providerRanOnFxThread")
				.isTrue();
		assertThat(restoredLayout.get().getBentoLayouts())
				.describedAs("restoredLayout.get().getBentoLayouts()")
				.hasSize(1);
	}

	@Test
	void restoreLayoutFallsBackToDefaultWhenTheStoredLayoutCannotBeDecoded() {
		final InMemoryLayoutStorage storage = new InMemoryLayoutStorage();
		storage.write("not a token this codec ever produced".getBytes(StandardCharsets.UTF_8));
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final DockingLayout defaultLayout = new DockingLayoutBuilder().build();

		final DockingLayoutRestorer restorer = new DockingLayoutRestorer(
				codec,
				storage,
				new DefaultBentoProvider(new Bento()),
				id -> Optional.empty(),
				null,
				null
		);

		final DockingLayout restoredLayout = restorer.restoreLayout(() -> defaultLayout);

		assertThat(restoredLayout)
				.describedAs("restoredLayout")
				.isSameAs(defaultLayout);
	}

	@Test
	void restoreLayoutWrapsADefaultLayoutSupplierFailureAsIllegalStateException() {
		final InMemoryLayoutStorage storage = new InMemoryLayoutStorage();
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();

		final DockingLayoutRestorer restorer = new DockingLayoutRestorer(
				codec,
				storage,
				new DefaultBentoProvider(new Bento()),
				id -> Optional.empty(),
				null,
				null
		);

		assertThatThrownBy(() ->
				restorer.restoreLayout(() -> {
					throw new RuntimeException("default layout supplier boom");
				})
		)
				.describedAs("restoreLayout with a failing default layout supplier")
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Could not create default docking layout")
				.cause()
				.isInstanceOf(BentoStateException.class)
				.cause()
				.hasMessageContaining("default layout supplier boom");
	}

	@Test
	void restoreLayoutFailsLoudlyWhenTheFxThreadNeverRunsTheRestore()
			throws InterruptedException {
		final String bentoId = "bento-timeout-test";
		final BentoState state = new BentoState.BentoStateBuilder(bentoId).build();

		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final InMemoryLayoutStorage storage = new InMemoryLayoutStorage();
		try (var out = storage.openOutputStream()) {
			codec.writeEncoded(List.of(state), out);
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}

		final DockingLayoutRestorer restorer = new DockingLayoutRestorer(
				codec,
				storage,
				new DefaultBentoProvider(new Bento(bentoId)),
				id -> Optional.empty(),
				null,
				null
		);

		final CountDownLatch fxThreadOccupied = new CountDownLatch(1);
		final CountDownLatch releaseFxThread = new CountDownLatch(1);

		Platform.runLater(() -> {
			fxThreadOccupied.countDown();
			try {
				releaseFxThread.await();
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});

		try {
			assertThat(fxThreadOccupied.await(10, TimeUnit.SECONDS))
					.describedAs("JavaFX thread became occupied")
					.isTrue();

			assertThatThrownBy(() ->
					restorer.restoreLayout(() -> new DockingLayoutBuilder().build())
			)
					.describedAs("restoreLayout with an FX thread that never runs the restore")
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("Timed out restoring the docking layout")
					.cause()
					.isInstanceOf(software.coley.bentofx.persistence.core.api.BentoStateTimeoutException.class);
		} finally {
			releaseFxThread.countDown();
		}
	}
}
