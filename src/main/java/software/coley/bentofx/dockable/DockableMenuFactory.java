package software.coley.bentofx.dockable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import javafx.scene.control.ContextMenu;

/**
 * Factory to create a {@link ContextMenu} for some given {@link Dockable}.
 *
 * @author Matt Coley
 */
public interface DockableMenuFactory {
	/**
	 * @param dockable
	 * 		Dockable to create a context menu for.
	 *
	 * @return Context menu for the dockable.
	 */
	@Nullable
	ContextMenu build(@NonNull Dockable dockable);
}