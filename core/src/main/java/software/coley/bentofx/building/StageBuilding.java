package software.coley.bentofx.building;

import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.robot.Robot;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.Bento;
import software.coley.bentofx.control.DragDropStage;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerLeafMenuFactory;
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
	private boolean applyMousePosition = false;
	private boolean applySourceAsOwner = true;

	public StageBuilding(Bento bento) {
		this.bento = bento;
	}

	/**
	 * Create a new stage for the given dockable.
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
	public DragDropStage newStageForDockable(Scene sourceScene, DockContainerLeaf source, Dockable dockable) {
		Region sourceRegion = source.asRegion();
		double width = sourceRegion.getWidth();
		double height = sourceRegion.getHeight();
		final DockContainerLeafMenuFactory leafMenuFactory = source.getMenuFactory();
		final Side side = source.getSide();
		return newStageForDockable(sourceScene, dockable, width, height, leafMenuFactory, side);
	}

	/**
	 * Create a new stage for the given dockable.
	 *
	 * @param sourceScene
	 * 		Original scene to copy state from.
	 * @param dockable
	 * 		Dockable to place into the newly created stage.
	 * @param width
	 * 		Preferred stage width.
	 * @param height
	 * 		Preferred stage height.
	 * @param leafMenuFactory
	 * 		DockContainerLeafMenuFactory for creating leaf menus
	 * @param side
	 * 		Side of this container to place {@code Header} displays on.
	 *        {@code null} to not display any headers.
	 *
	 * @return Newly created stage.
	 */
	public DragDropStage newStageForDockable(@Nullable Scene sourceScene,
	                                         Dockable dockable,
	                                         double width,
	                                         double height,
	                                         @Nullable DockContainerLeafMenuFactory leafMenuFactory,
	                                         @Nullable Side side) {
		DockBuilding builder = bento.dockBuilding();
		DockContainerRootBranch root = builder.root();
		DockContainerLeaf leaf = builder.leaf();
		leaf.setMenuFactory(leafMenuFactory);
		leaf.setSide(side);
		return newStageForDockable(sourceScene, root, leaf, dockable, width, height);
	}

	/**
	 * Create a new stage for the given dockable.
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
	public DragDropStage newStageForDockable(@Nullable Scene sourceScene,
	                                         DockContainerRootBranch root,
	                                         DockContainerLeaf leaf,
	                                         Dockable dockable,
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
			initializeFromSource(sourceScene, scene, sourceStage, stage, applySourceAsOwner);

		// Position the stage at the mouse position, if enabled.
		if (applyMousePosition) {
			final Robot robot = new Robot();
			final Point2D mousePosition = robot.getMousePosition();

			// Clamp the position to the screen bounds.
			// We don't want a new stage that spawns on the bottom or right edge of the screen to be inaccessible.
			double x = mousePosition.getX();
			double y = mousePosition.getY();
			ObservableList<Screen> screens = Screen.getScreensForRectangle(x, y, x + 1, y + 1);
			if (!screens.isEmpty()) {
				final Bounds dockableContainerBounds = leaf.getBoundsInLocal();
				final Rectangle2D screenBounds = screens.getFirst().getVisualBounds();
				final double maxX = screenBounds.getMaxX() - dockableContainerBounds.getWidth();
				final double maxY = screenBounds.getMaxY() - dockableContainerBounds.getHeight();
				x = Math.min(x, maxX);
				y = Math.min(y, maxY);
			}

			stage.setX(x);
			stage.setY(y);
		}

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
	protected void initializeFromSource(Scene sourceScene,
	                                    Scene newScene,
	                                    @Nullable Stage sourceStage,
	                                    DragDropStage newStage,
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

	/**
	 * @param applySourceAsOwner
	 *        {@code true} to make newly created stages have their owner set to the source stage the dockable is being dragged out of.
	 *        {@code false} to not set an owner, allowing the new stage to be handled independently of the source stage.
	 *
	 * @see Stage#initOwner(Window)
	 */
	public void setApplySourceAsOwner(boolean applySourceAsOwner) {
		this.applySourceAsOwner = applySourceAsOwner;
	}

	/**
	 *
	 * @param applyMousePosition
	 *        {@code true} to position newly created stages at the current mouse position.
	 *        {@code false} to not apply any special positioning, allowing the stage to be positioned automatically <i>(Generally centered)</i>.
	 */
	public void setApplyMousePosition(boolean applyMousePosition) {
		this.applyMousePosition = applyMousePosition;
	}
}
