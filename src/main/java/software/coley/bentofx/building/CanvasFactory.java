package software.coley.bentofx.building;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.control.canvas.PixelCanvas;
import software.coley.bentofx.layout.container.DockContainerLeaf;

/**
 * Factory for building a {@link PixelCanvas} for a {@link DockContainerLeaf}.
 *
 * @author Matt Coley
 */
public interface CanvasFactory {
	/**
	 * @param container
	 * 		Parent container.
	 *
	 * @return New canvas.
	 */
	@Nonnull
	PixelCanvas newCanvas(@Nonnull DockContainerLeaf container);
}
