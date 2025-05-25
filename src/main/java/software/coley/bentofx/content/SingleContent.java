package software.coley.bentofx.content;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Side;
import software.coley.bentofx.dockable.Dockable;

import java.util.Collections;
import java.util.List;

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

	@Nonnull
	@Override
	default List<Dockable> getDockables() {
		return Collections.singletonList(getDockable());
	}

	@Override
	default boolean addDockable(@Nonnull Dockable dockable) {
		// Not supported as this content is created with a single item, and only supports showing a single item.
		return false;
	}

	@Override
	default boolean removeDockable(@Nonnull Dockable dockable) {
		// Not supported for similar reasons as above.
		return false;
	}

	@Override
	default boolean closeDockable(@Nonnull Dockable dockable) {
		// Not supported for similar reasons as above.
		return false;
	}

	@Override
	default boolean selectDockable(@Nullable Dockable dockable) {
		if (getDockable() == dockable) {
			dockable.nodeProperty().get().requestFocus();
			return true;
		}
		return false;
	}
}
