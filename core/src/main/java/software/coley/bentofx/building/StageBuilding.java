package software.coley.bentofx.building;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;
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
		return newStageForDockable(sourceScene, root, leaf, dockable, width, height);
	}

	/**
	 * Create a new stage for the given dockable.,
	 *
	 * @param sourceScene
	 * 		Original scene to copy state from.
	 * @param root
	 * 		Newly created root branch to place into the resulting stage.
	 * @param leaf
	 * 		Newly created leaf container to place the dockable into.
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
	public DragDropStage newStageForDockable(@Nullable Scene sourceScene,
	                                         @Nonnull DockContainerRootBranch root,
	                                         @Nonnull DockContainerLeaf leaf,
	                                         @Nonnull Dockable dockable,
	                                         double width, double height) {
		// Sanity check, leaf shouldn't have an existing parent.
		if (leaf.getParentContainer() != root && leaf.getParentContainer() != null)
			leaf.removeFromParent();

		// Add the leaf to the given root, and the dockable to the leaf.
		root.addContainer(leaf);
		leaf.addDockable(dockable);

		// Create new stage/scene for the dockable to spawn in.
		Region region = root.asRegion();
		Stage sourceStage = sourceScene == null ? null : (Stage) sourceScene.getWindow();
		DragDropStage stage = stageFactory.newStage(sourceStage);
		Scene scene = sceneFactory.newScene(sourceScene, region, width, height);
		stage.setScene(scene);

		// Copy properties from the source scene/stage.
		if (sourceScene != null)
			initializeFromSource(sourceScene, scene, sourceStage, stage, true);

		return stage;
	}

	/**
	 * Copy attributes from the source scene/stage housing a dockable
	 * to the new scene/stage the dockable will be moved to.
	 *
	 * @param sourceScene
	 * 		Source scene the dockable belonged to.
	 * @param newScene
	 * 		New scene the dockable is being moved to.
	 * @param sourceStage
	 * 		Source stage a dockable belonged to.
	 * @param newStage
	 * 		New stage the dockable is being moved to.
	 * @param sourceIsOwner
	 *        {@code true} to invoke {@link Stage#initOwner(Window)}, where the owner is the source stage.
	 */
	protected void initializeFromSource(@Nonnull Scene sourceScene,
	                                    @Nonnull Scene newScene,
	                                    @Nullable Stage sourceStage,
	                                    @Nonnull DragDropStage newStage,
	                                    boolean sourceIsOwner) {
		// Copy stylesheets.
		newScene.setUserAgentStylesheet(sourceScene.getUserAgentStylesheet());
		newScene.getStylesheets().addAll(sourceScene.getStylesheets());

		// Copy icon.
		if (sourceStage != null)
			newStage.getIcons().addAll(sourceStage.getIcons());

		// Just to prevent 1x1 tiny spawns.
		newStage.setMinWidth(150);
		newStage.setMinHeight(100);

		// Make the source stage the owner of the new stage.
		// - Will prevent minimizing.
		if (sourceIsOwner)
			newStage.initOwner(sourceStage);
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
