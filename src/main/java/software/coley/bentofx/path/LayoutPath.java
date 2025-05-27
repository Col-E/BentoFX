package software.coley.bentofx.path;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.layout.DockLayout;
import software.coley.bentofx.layout.RootDockLayout;

import java.util.List;

/**
 * @param rootLayout
 * 		Root layout.
 * @param layouts
 * 		Hierarchy of layouts, with the first being the immediate child layout of the root.
 */
public record LayoutPath(@Nonnull RootDockLayout rootLayout,
                         @Nonnull List<DockLayout> layouts) {
	/**
	 * The last layout in the path. If you used a search for a {@link DockLayout} via its identifier
	 * this will be the intended layout in the path with the requested identifier.
	 *
	 * @return The last layout in the path.
	 */
	@Nonnull
	public DockLayout tailLayout() {
		return layouts.getLast();
	}
}
