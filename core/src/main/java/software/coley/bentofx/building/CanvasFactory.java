package software.coley.bentofx.building;

import org.jspecify.annotations.NonNull;
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
	@NonNull
	PixelCanvas newCanvas(@NonNull DockContainerLeaf container);
}
