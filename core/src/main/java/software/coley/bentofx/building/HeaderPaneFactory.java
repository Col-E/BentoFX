package software.coley.bentofx.building;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.control.HeaderPane;
import software.coley.bentofx.layout.container.DockContainerLeaf;

/**
 * Factory for building a {@link HeaderPane} for a {@link DockContainerLeaf}.
 *
 * @author Matt Coley
 */
public interface HeaderPaneFactory {
	/**
	 * @param container
	 * 		Parent container.
	 *
	 * @return New header pane.
	 */
	@Nonnull
	HeaderPane newHeaderPane(@Nonnull DockContainerLeaf container);
}
