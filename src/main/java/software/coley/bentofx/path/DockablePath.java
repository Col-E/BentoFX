package software.coley.bentofx.path;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.space.DockSpace;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockLayout;
import software.coley.bentofx.layout.RootDockLayout;

import java.util.List;

/**
 * @param rootLayout
 * 		Root layout.
 * @param layouts
 * 		Hierarchy of layouts, with the first being the immediate child layout of the root and the last being the holder of the space.
 * @param space
 * 		Space holding the dockable.
 * @param dockable
 * 		Target dockable.
 */
public record DockablePath(@Nonnull RootDockLayout rootLayout,
                           @Nonnull List<DockLayout> layouts,
                           @Nonnull DockSpace space,
                           @Nonnull Dockable dockable) {
	public DockablePath(@Nonnull SpacePath spacePath, @Nonnull Dockable dockable) {
		this(spacePath.rootLayout(), spacePath.layouts(), spacePath.space(), dockable);
	}
}
