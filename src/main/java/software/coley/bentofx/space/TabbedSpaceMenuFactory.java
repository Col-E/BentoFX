package software.coley.bentofx.space;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.control.ContextMenu;

/**
 * Factory to create a {@link ContextMenu} for some given {@link DockSpace}.
 *
 * @author Matt Coley
 */
public interface TabbedSpaceMenuFactory {
	/**
	 * @param space
	 * 		Space to create a context menu for.
	 *
	 * @return Context menu for the space.
	 */
	@Nullable
	ContextMenu build(@Nonnull TabbedDockSpace space);
}
