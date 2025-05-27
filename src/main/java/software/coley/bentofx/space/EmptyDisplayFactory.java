package software.coley.bentofx.space;

import jakarta.annotation.Nonnull;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import software.coley.bentofx.layout.DockLayout;

/**
 * Factory to create a {@link Node} for new {@link EmptyDockSpace} instances residing in some given {@link DockLayout}.
 *
 * @author Matt Coley
 */
public interface EmptyDisplayFactory {
	/**
	 * Factory implementation that creates a blank display.
	 */
	EmptyDisplayFactory BLANK = new EmptyDisplayFactory() {
		@Nonnull
		@Override
		public Node build(@Nonnull DockLayout parentLayout) {
			return new Region();
		}
	};

	/**
	 * @param parentLayout
	 * 		Parent layout to contain the new {@link EmptyDockSpace}.
	 *
	 * @return Display graphic for the new empty space.
	 */
	@Nonnull
	Node build(@Nonnull DockLayout parentLayout);
}
