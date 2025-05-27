package software.coley.bentofx.space;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Node;
import software.coley.bentofx.Bento;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockLayout;

import java.util.Collections;
import java.util.List;

/**
 * Empty content that displays some placeholder {@link Node} based on {@link Bento#newEmptySpace(DockLayout)}.
 *
 * @author Matt Coley
 */
non-sealed public interface EmptyDockSpace extends DockSpace {
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
