package software.coley.bentofx;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Scene;
import software.coley.bentofx.util.DragDropStage;

public interface StageFactory {
	@Nonnull
	DragDropStage newStage(@Nullable Scene sourceScene);
}
