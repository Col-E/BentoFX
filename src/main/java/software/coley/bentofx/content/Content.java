package software.coley.bentofx.content;

import jakarta.annotation.Nullable;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.RegionBacked;
import software.coley.bentofx.layout.ContentLayout;
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
	 * @return Immediate parent layout containing this content.
	 */
	@Nullable
	default ContentLayout getParentLayout() {
		return BentoUtils.getParent(getBackingRegion(), ContentLayout.class);
	}
}
