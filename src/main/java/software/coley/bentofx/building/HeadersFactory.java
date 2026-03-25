package software.coley.bentofx.building;

import org.jspecify.annotations.NonNull;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import software.coley.bentofx.control.HeaderPane;
import software.coley.bentofx.control.Headers;
import software.coley.bentofx.layout.container.DockContainerLeaf;

/**
 * Factory for building {@link Headers} in a parent {@link HeaderPane}.
 *
 * @author Matt Coley
 */
public interface HeadersFactory {
	/**
	 * @param container
	 * 		Associated container.
	 * @param orientation
	 * 		Orientation of the headers.
	 * @param side
	 * 		Side this headers bar will be located at.
	 *
	 * @return Newly created headers.
	 */
	@NonNull
	Headers newHeaders(@NonNull DockContainerLeaf container, @NonNull Orientation orientation, @NonNull Side side);
}
