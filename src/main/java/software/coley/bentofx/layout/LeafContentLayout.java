package software.coley.bentofx.layout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.content.EmptyContent;

/**
 * Outlines a middle level layout that holds a single {@link Content}.
 *
 * @author Matt Coley
 */
non-sealed public interface LeafContentLayout extends ContentLayout {
	/**
	 * @return Displayed content.
	 */
	@Nonnull
	Content getContent();

	/**
	 * Replaces this layout's content.
	 *
	 * @param content
	 * 		New content to display. A {@code null} will be mapped to {@link EmptyContent}.
	 */
	void setContent(@Nullable Content content);
}
