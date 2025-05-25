package software.coley.bentofx.content;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.ContentLayout;

import java.util.Collections;
import java.util.List;

/**
 * Empty content that displays some placeholder {@link Node} based on {@link Bento#newEmptyContent(ContentLayout)}.
 *
 * @author Matt Coley
 */
non-sealed public interface EmptyContent extends Content {
	@Nonnull
	@Override
	default List<Dockable> getDockables() {
		return Collections.emptyList();
	}

	@Override
	default boolean addDockable(@Nonnull Dockable dockable) {
		// Not supported with an empty display
		return false;
	}

	@Override
	default boolean removeDockable(@Nonnull Dockable dockable) {
		// Not supported with an empty display
		return false;
	}

	@Override
	default boolean closeDockable(@Nonnull Dockable dockable) {
		// Not supported with an empty display
		return false;
	}

	@Override
	default boolean selectDockable(@Nullable Dockable dockable) {
		// Not supported with an empty display
		return false;
	}
}
