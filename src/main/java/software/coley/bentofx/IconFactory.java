package software.coley.bentofx;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;

/**
 * Factory to create a {@link Node} graphic for some given {@link Dockable}.
 *
 * @author Matt Coley
 */
public interface IconFactory {
	/**
	 * @param dockable
	 * 		Dockable to create a graphic for.
	 *
	 * @return Graphic for the dockable.
	 */
	@Nullable
	Node build(@Nonnull Dockable dockable);
}
