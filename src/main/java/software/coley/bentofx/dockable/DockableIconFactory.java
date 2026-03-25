package software.coley.bentofx.dockable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import javafx.scene.Node;

/**
 * Factory to create a {@link Node} graphic for some given {@link Dockable}.
 * Implementations should create <b>NEW</b> instances for <b>EACH</b> call.
 *
 * @author Matt Coley
 */
public interface DockableIconFactory {
	/**
	 * @param dockable
	 * 		Dockable to create a graphic for.
	 *
	 * @return Graphic for the dockable.
	 */
	@Nullable
	Node build(@NonNull Dockable dockable);
}