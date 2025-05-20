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
}
