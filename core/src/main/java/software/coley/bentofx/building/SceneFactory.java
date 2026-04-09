package software.coley.bentofx.building;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import software.coley.bentofx.control.DragDropStage;

/**
 * Factory for building scenes when a new {@link DragDropStage} is being prepared.
 *
 * @author Matt Coley
 */
public interface SceneFactory {
	/**
	 * @param sourceScene
	 * 		Original scene to copy state from.
	 * @param content
	 * 		Content to place in the scene.
	 * @param width
	 * 		Content width.
	 * @param height
	 * 		Content height.
	 *
	 * @return Newly created scene.
	 */
	@Nonnull
	Scene newScene(@Nullable Scene sourceScene, @Nonnull Region content, double width, double height);
}
