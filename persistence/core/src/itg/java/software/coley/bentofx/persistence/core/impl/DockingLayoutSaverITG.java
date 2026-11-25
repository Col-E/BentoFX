package software.coley.bentofx.persistence.core.impl;

import javafx.scene.Scene;
import javafx.stage.Stage;
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
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.IdentifiableState;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;
import software.coley.bentofx.persistence.testfixtures.codec.InMemoryLayoutCodec;
import software.coley.bentofx.persistence.testfixtures.codec.ThreadRecordingLayoutCodec;
import software.coley.bentofx.persistence.testfixtures.storage.InMemoryLayoutStorage;
import software.coley.bentofx.persistence.testfixtures.storage.ThreadRecordingLayoutStorage;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ApplicationExtension.class)
class DockingLayoutSaverITG {

	private static final String DETACHED_STAGE_TITLE = "Detached";

	private static final String CODEC_GET_ENCODE_THREAD_DESCRIPTION = "codec.getEncodeThread()";
	private static final String STORAGE_GET_OPEN_OUTPUT_STREAM_THREAD_DESCRIPTION = "storage.getOpenOutputStreamThread()";
	private static final String STORAGE_TO_BYTE_ARRAY_DESCRIPTION = "storage.toByteArray()";

	@Test
	void saveLayoutEncodesNonStageRootsAndDragDropStagesWithoutDuplicates(FxRobot robot) throws BentoStateException {
		Bento bento = new Bento();
		DockBuilding dockBuilding = bento.dockBuilding();

		// Main
		final String mainRootBranchId = "root-main";
		final String mainLeafId = "leaf-main";
		final String mainDockableId = "dock-main";
		final String mainDockableTitle = "Main Dock";

		// DragDropStage
		final String dragDropRootBranchId = "root-drag";

		DockContainerRootBranch mainRoot = dockBuilding.root(mainRootBranchId);
		DockContainerLeaf mainLeaf = dockBuilding.leaf(mainLeafId);
		Dockable mainDockable = dockBuilding.dockable(mainDockableId);
		mainDockable.setTitle(mainDockableTitle);
		mainLeaf.addDockable(mainDockable);
		mainRoot.addContainer(mainLeaf);

		DockContainerRootBranch dragRoot = dockBuilding.root(dragDropRootBranchId);
		DockContainerLeaf dragLeaf = dockBuilding.leaf("leaf-drag");
		Dockable dragDockable = dockBuilding.dockable("dock-drag");
		dragDockable.setTitle("Drag Dock");
		dragLeaf.addDockable(dragDockable);
		dragRoot.addContainer(dragLeaf);

		AtomicReference<Stage> mainStageRef = new AtomicReference<>();
		AtomicReference<DragDropStage> stageRef = new AtomicReference<>();
		robot.interact(() -> {
			Stage mainStage = new Stage();
			mainStage.setScene(new Scene(mainRoot));
			mainStage.show();
			mainStageRef.set(mainStage);

			DragDropStage stage = new DragDropStage(true);
			stage.setTitle(DETACHED_STAGE_TITLE);
			stage.setX(200);
			stage.setY(150);
			stage.setWidth(600);
			stage.setHeight(400);
			stage.setScene(new Scene(dragRoot));
			stage.show();
			stageRef.set(stage);
		});

		DefaultBentoProvider bentoProvider = new DefaultBentoProvider();
		bentoProvider.addBento(bento);
		InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		InMemoryLayoutStorage storage = new InMemoryLayoutStorage();

		try (DockingLayoutSaver saver = new DockingLayoutSaver(
				codec, storage, bentoProvider
		)) {

			saver.saveLayout();
		}

		final List<BentoState> bentoStates = codec.getEncodedStates();

		assertThat(storage.exists())
				.describedAs("storage.exists()")
				.isTrue();
		assertThat(storage.toByteArray())
				.describedAs(STORAGE_TO_BYTE_ARRAY_DESCRIPTION)
				.isNotEmpty();
		assertThat(bentoStates)
				.describedAs("bentoStates")
				.hasSize(1);

		BentoState saved = bentoStates.getFirst();
		assertThat(saved.getRootBranchStates())
				.describedAs("saved.getRootBranchStates()")
				.extracting(IdentifiableState::getIdentifier)
				.containsExactly(mainRootBranchId);
		assertThat(saved.getDragDropStageStates())
				.describedAs("saved.getDragDropStageStates()")
				.hasSize(1);
		assertThat(saved.getDragDropStageStates().getFirst().getTitle())
				.describedAs("saved.getDragDropStageStates().getFirst().getTitle()")
				.contains(DETACHED_STAGE_TITLE);
		assertThat(saved.getDragDropStageStates().getFirst()
				.getDockContainerRootBranchState()
				.map(IdentifiableState::getIdentifier))
				.describedAs("saved.getDragDropStageStates().getFirst() .getDockContainerRootBran...")
				.contains(dragDropRootBranchId);

		robot.interact(() -> {
			stageRef.get().hide();
			mainStageRef.get().hide();
		});
	}

	@Test
	void saveLayoutDoesNotWriteWhenNoBentosExist() throws BentoStateException {
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final InMemoryLayoutStorage storage = new InMemoryLayoutStorage();
		final BentoProvider emptyBentoProvider = new DefaultBentoProvider();

		try (DockingLayoutSaver saver = new DockingLayoutSaver(
				codec, storage, emptyBentoProvider

		)) {

			saver.saveLayout();
		}

		assertThat(codec.getEncodedStates())
				.describedAs("codec.getEncodedStates()")
				.isEmpty();
		assertThat(storage.toByteArray())
				.describedAs(STORAGE_TO_BYTE_ARRAY_DESCRIPTION)
				.isEmpty();
	}

	/**
	 * Tests a {@code Bento} exists, but none of its root branches
	 * have a {@code Scene}, so the capture finds nothing.
	 */
	@Test
	void saveLayoutKeepsThePersistedLayoutWhenNothingIsAttached()
			throws BentoStateException {
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final InMemoryLayoutStorage storage = new InMemoryLayoutStorage();

		final byte[] previouslySavedLayout =
				"a-good-layout".getBytes(StandardCharsets.UTF_8);
		storage.write(previouslySavedLayout);

		// A root branch that is never given a Scene - which is exactly what this
		// module's own restorer hands back. Until the application attaches it,
		// bento.getRootContainers() does not report it.
		final Bento bento = new Bento();
		bento.dockBuilding().root("root-never-attached");

		final DefaultBentoProvider bentoProvider = new DefaultBentoProvider();
		bentoProvider.addBento(bento);

		try (DockingLayoutSaver saver = new DockingLayoutSaver(
				codec, storage, bentoProvider
		)) {

			saver.saveLayout();
		}

		assertThat(codec.getEncodedStates())
				.describedAs("codec.getEncodedStates()")
				.isEmpty();
		assertThat(storage.toByteArray())
				.describedAs(STORAGE_TO_BYTE_ARRAY_DESCRIPTION)
				.isEqualTo(previouslySavedLayout);
	}

	@Test
	void saveLayoutEncodesAndWritesAwayFromFxThreadWhenCalledOnFxThread(FxRobot robot) {
		final Bento bento = new Bento();
		final DockBuilding dockBuilding = bento.dockBuilding();
		final DockContainerRootBranch root = dockBuilding.root("root");
		root.addContainer(dockBuilding.leaf("leaf"));

		final ThreadRecordingLayoutCodec codec = new ThreadRecordingLayoutCodec();
		final BentoProvider bentoProvider = new DefaultBentoProvider(bento);

		// Not a try-with-resources: the DockingLayoutSaver below is handed this
		// storage and closes it, which is the ownership LayoutStorage documents.
		// Closing it here as well would state the opposite.
		//noinspection resource
		final ThreadRecordingLayoutStorage storage = new ThreadRecordingLayoutStorage();

		final AtomicReference<Thread> fxThread = new AtomicReference<>();
		final AtomicReference<Stage> stageRef = new AtomicReference<>();

		robot.interact(() -> {
			// The root branch needs a Scene before the save. This test is about
			// which thread encodes and writes, so it needs write to happen at
			// all, and a capture only sees root branches their Bento knows about -
			// registration that happens from a scene listener in core. Without
			// this the save now correctly finds nothing and skips write.
			final Stage stage = new Stage();
			stage.setScene(new Scene(root));
			stage.show();
			stageRef.set(stage);

			fxThread.set(Thread.currentThread());
			try (DockingLayoutSaver saver = new DockingLayoutSaver(
					codec, storage, bentoProvider
			)) {
				saver.saveLayout();
			} catch (final BentoStateException e) {
				throw new AssertionError(e);
			}
		});

		robot.interact(() -> stageRef.get().hide());

		assertThat(fxThread.get())
				.describedAs("fxThread.get()")
				.isNotNull();
		assertThat(codec.getEncodeThread())
				.describedAs(CODEC_GET_ENCODE_THREAD_DESCRIPTION)
				.isNotNull();
		assertThat(storage.getOpenOutputStreamThread())
				.describedAs(STORAGE_GET_OPEN_OUTPUT_STREAM_THREAD_DESCRIPTION)
				.isNotNull();
		assertThat(codec.getEncodeThread())
				.describedAs(CODEC_GET_ENCODE_THREAD_DESCRIPTION)
				.isNotEqualTo(fxThread.get());
		assertThat(storage.getOpenOutputStreamThread())
				.describedAs(STORAGE_GET_OPEN_OUTPUT_STREAM_THREAD_DESCRIPTION)
				.isNotEqualTo(fxThread.get());
	}
}
