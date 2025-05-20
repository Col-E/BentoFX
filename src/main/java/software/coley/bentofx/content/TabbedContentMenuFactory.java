package software.coley.bentofx.content;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.control.ContextMenu;
import software.coley.bentofx.content.Content;

/**
 * Factory to create a {@link ContextMenu} for some given {@link Content}.
 *
 * @author Matt Coley
 */
public interface TabbedContentMenuFactory {
	/**
	 * @param content
	 * 		Content to create a context menu for.
	 *
	 * @return Context menu for the content.
	 */
	@Nullable
	ContextMenu build(@Nonnull TabbedContent content);
}
