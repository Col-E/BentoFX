package software.coley.bentofx;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.control.ContextMenu;

/**
 * Factory to create a {@link ContextMenu} for some given {@link Dockable}.
 *
 * @author Matt Coley
 */
public interface MenuFactory {
	/**
	 * @param dockable
	 * 		Dockable to create a context menu for.
	 *
	 * @return Context menu for the dockable.
	 */
	@Nullable
	ContextMenu build(@Nonnull Dockable dockable);
}
