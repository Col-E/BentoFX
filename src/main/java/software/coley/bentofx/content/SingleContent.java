package software.coley.bentofx.content;

import jakarta.annotation.Nonnull;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Side;
import software.coley.bentofx.Dockable;

/**
 * Single {@link Dockable} display content.
 *
 * @author Matt Coley
 */
non-sealed public interface SingleContent extends Content {
	/**
	 * @return Side where the dockable's header is shown.
	 */
	@Nonnull
	ObjectProperty<Side> headerSideProperty();

	/**
	 * @return Displayed dockable.
	 */
	@Nonnull
	Dockable getDockable();
}
