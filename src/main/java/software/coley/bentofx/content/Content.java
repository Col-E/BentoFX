package software.coley.bentofx.content;

import jakarta.annotation.Nullable;
import javafx.scene.Node;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.RegionBacked;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.RootContentLayout;
import software.coley.bentofx.path.ContentPath;
import software.coley.bentofx.path.LayoutPath;
import software.coley.bentofx.util.BentoUtils;

/**
 * Outlines the lowest level of any docking-enabled layout.
 *
 * @author Matt Coley
 * @see Bento#newContentBuilder()
 * @see EmptyContent
 * @see SingleContent
 * @see TabbedContent
 */
public sealed interface Content extends RegionBacked, Identifiable permits EmptyContent, SingleContent, TabbedContent {
	/**
	 * Lookup the parent layout of this content.
	 * <ul>
	 *     <li>Will be {@code null} if there is no {@link Node#getParent()}.</li>
	 * </ul>
	 *
	 * @return Immediate parent layout containing this content.
	 */
	@Nullable
	default ContentLayout getParentLayout() {
		return BentoUtils.getParent(getBackingRegion(), ContentLayout.class);
	}

	/**
	 * Lookup the root layout of this content.
	 * <ul>
	 *     <li>Will be {@code null} if there is no chained {@link Node#getParent()} that is a {@link RootContentLayout}.</li>
	 * </ul>
	 *
	 * @return Root layout containing this content.
	 */
	@Nullable
	default RootContentLayout getRootLayout() {
		return BentoUtils.getOrParent(getBackingRegion(), RootContentLayout.class);
	}

	/**
	 * @return Path to this content from the root.
	 */
	@Nullable
	default ContentPath getPath() {
		ContentLayout layout = getParentLayout();
		if (layout == null)
			return null;

		LayoutPath layoutPath = layout.getPath();
		if (layoutPath == null)
			return null;

		return new ContentPath(layoutPath, this);
	}
}
