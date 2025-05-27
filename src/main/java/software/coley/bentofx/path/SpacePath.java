package software.coley.bentofx.path;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.layout.DockLayout;
import software.coley.bentofx.layout.RootDockLayout;
import software.coley.bentofx.space.DockSpace;

import java.util.List;

/**
 * @param rootLayout
 * 		Root layout.
 * @param layouts
 * 		Hierarchy of layouts, with the first being the immediate child layout of the root and the last being the holder of the space.
 * @param space
 * 		Target docking space.
 */
public record SpacePath(@Nonnull RootDockLayout rootLayout,
                        @Nonnull List<DockLayout> layouts,
                        @Nonnull DockSpace space) {
	public SpacePath(@Nonnull LayoutPath layoutPath, @Nonnull DockSpace space) {
		this(layoutPath.rootLayout(), layoutPath.layouts(), space);
	}
}
