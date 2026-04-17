package software.coley.bentofx.dockable;

import javafx.scene.control.ContextMenu;
import org.jspecify.annotations.Nullable;

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
	ContextMenu build(Dockable dockable);
}
