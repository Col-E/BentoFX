package software.coley.bentofx.building;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.stage.Stage;
import software.coley.bentofx.control.DragDropStage;

/**
 * Factory for building new {@link DragDropStage}.
 *
 * @author Matt Coley
 */
public interface StageFactory {
	/**
	 * @param sourceStage
	 * 		Original stage to copy state from.
	 *
	 * @return Newly created stage.
	 */
	@Nonnull
	DragDropStage newStage(@Nullable Stage sourceStage);
}
