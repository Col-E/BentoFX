package software.coley.bentofx;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import software.coley.bentofx.util.DragDropStage;

public interface SceneFactory {
	@Nonnull
	Scene newScene(@Nonnull Region content, double width, double height);
}
