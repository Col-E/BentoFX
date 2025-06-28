package software.coley.bentofx.building;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import software.coley.bentofx.Bento;
import software.coley.bentofx.control.DragDropStage;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;

/**
 * Builders for {@link DragDropStage}.
 */
public class StageBuilding {
	private static final StageFactory DEFAULT_STAGE_FACTORY = (sourceStage) -> new DragDropStage(true);
	private static final SceneFactory DEFAULT_SCENE_FACTORY = (sourceScene, content, width, height) -> new Scene(content, width, height);
	private final Bento bento;
	private StageFactory stageFactory = DEFAULT_STAGE_FACTORY;
	private SceneFactory sceneFactory = DEFAULT_SCENE_FACTORY;

	public StageBuilding(@Nonnull Bento bento) {
		this.bento = bento;
	}

	/**
	 * Create a new stage for the given dockable.,
	 *
	 * @param sourceScene
	 * 		Original scene to copy state from.
	 * @param source
	 * 		Container holding the dockable.
	 * @param dockable
	 * 		Dockable to place into the newly created stage.
	 *
	 * @return Newly created stage.
	 */
	@Nonnull
	public DragDropStage newStageForDockable(@Nonnull Scene sourceScene, @Nonnull DockContainer source, @Nonnull Dockable dockable) {
		Region sourceRegion = source.asRegion();
		double width = sourceRegion.getWidth();
		double height = sourceRegion.getHeight();
		return newStageForDockable(sourceScene, dockable, width, height);
	}

	/**
	 * Create a new stage for the given dockable.,
	 *
	 * @param sourceScene
	 * 		Original scene to copy state from.
	 * @param dockable
	 * 		Dockable to place into the newly created stage.
	 * @param width
	 * 		Preferred stage width.
	 * @param height
	 * 		Preferred stage height.
	 *
	 * @return Newly created stage.
	 */
	@Nonnull
	public DragDropStage newStageForDockable(@Nullable Scene sourceScene, @Nonnull Dockable dockable, double width, double height) {
		DockBuilding builder = bento.dockBuilding();
		DockContainerRootBranch root = builder.root();
		DockContainerLeaf leaf = builder.leaf();
		root.addContainer(leaf);
		leaf.addDockable(dockable);

		Region region = root.asRegion();

		Stage sourceStage = sourceScene == null ? null : (Stage) sourceScene.getWindow();
		DragDropStage stage = stageFactory.newStage(sourceStage);
		Scene scene = sceneFactory.newScene(sourceScene, region, width, height);
		stage.setScene(scene);

		if (sourceScene != null) {
			scene.setUserAgentStylesheet(sourceScene.getUserAgentStylesheet());
			scene.getStylesheets().addAll(sourceScene.getStylesheets());

			stage.initOwner(sourceStage);
			stage.setMinWidth(150);
			stage.setMinHeight(100);
			if (sourceStage != null) {
				stage.getIcons().addAll(sourceStage.getIcons());
			}
		}

		return stage;
	}

	/**
	 * @param factory
	 * 		New factory for creating stages.
	 */
	public void setStageFactory(@Nullable StageFactory factory) {
		if (factory == null)
			factory = DEFAULT_STAGE_FACTORY;
		stageFactory = factory;
	}

	/**
	 * @param factory
	 * 		New factory for creating scenes.
	 */
	public void setSceneFactory(@Nullable SceneFactory factory) {
		if (factory == null)
			factory = DEFAULT_SCENE_FACTORY;
		sceneFactory = factory;
	}
}
