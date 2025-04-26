package software.coley.bentofx;

import jakarta.annotation.Nonnull;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import software.coley.bentofx.content.EmptyContent;
import software.coley.bentofx.layout.ContentLayout;

/**
 * Factory to create a {@link Node} for new {@link EmptyContent} instances residing in some given {@link ContentLayout}.
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
		public Node build(@Nonnull ContentLayout parentLayout) {
			return new Region();
		}
	};

	/**
	 * @param parentLayout
	 * 		Parent layout to contain the new {@link EmptyContent}.
	 *
	 * @return Display graphic for the new empty content.
	 */
	@Nonnull
	Node build(@Nonnull ContentLayout parentLayout);
}
