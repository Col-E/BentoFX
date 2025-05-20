package software.coley.bentofx.path;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.RootContentLayout;

import java.util.List;

/**
 * @param rootLayout
 * 		Root layout.
 * @param subLayouts
 * 		Hierarchy of content layouts, with the first being the immediate child layout of the root and the last being the holder of the content.
 * @param content
 * 		Content holding the dockable.
 * @param dockable
 * 		Target dockable.
 */
public record DockablePath(@Nonnull RootContentLayout rootLayout,
                           @Nonnull List<ContentLayout> subLayouts,
                           @Nonnull Content content,
                           @Nonnull Dockable dockable) {
	public DockablePath(@Nonnull ContentPath contentPath, @Nonnull Dockable dockable) {
		this(contentPath.rootLayout(), contentPath.subLayouts(), contentPath.content(), dockable);
	}
}
