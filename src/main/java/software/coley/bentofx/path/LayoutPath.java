package software.coley.bentofx.path;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.RootContentLayout;

import java.util.List;

/**
 * @param rootLayout
 * 		Root layout.
 * @param layouts
 * 		Hierarchy of content layouts, with the first being the immediate child layout of the root.
 */
public record LayoutPath(@Nonnull RootContentLayout rootLayout,
                         @Nonnull List<ContentLayout> layouts) {
	/**
	 * The last layout in the path. If you used a search for a {@link ContentLayout} via its identifier
	 * this will be the intended layout in the path with the requested identifier.
	 *
	 * @return The last layout in the path.
	 */
	@Nonnull
	public ContentLayout tailLayout() {
		return layouts.getLast();
	}
}
