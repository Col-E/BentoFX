package software.coley.bentofx.building;

import javafx.stage.Stage;
import org.jspecify.annotations.Nullable;
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
	DragDropStage newStage(@Nullable Stage sourceStage);
}
