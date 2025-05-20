package software.coley.bentofx.path;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.RootContentLayout;

import java.util.List;

/**
 * @param rootLayout
 * 		Root layout.
 * @param subLayouts
 * 		Hierarchy of content layouts, with the first being the immediate child layout of the root and the last being the holder of the content.
 * @param content
 * 		Target path.
 */
public record ContentPath(@Nonnull RootContentLayout rootLayout,
                          @Nonnull List<ContentLayout> subLayouts,
                          @Nonnull Content content) {
	public ContentPath(@Nonnull LayoutPath layoutPath, @Nonnull Content content) {
		this(layoutPath.rootLayout(), layoutPath.layouts(), content);
	}
}
