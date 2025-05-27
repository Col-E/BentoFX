package software.coley.bentofx.layout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.bentofx.space.DockSpace;
import software.coley.bentofx.space.EmptyDockSpace;

/**
 * Outlines a middle level layout that holds a single {@link DockSpace}.
 *
 * @author Matt Coley
 */
non-sealed public interface LeafDockLayout extends DockLayout {
	/**
	 * @return Displayed docking space.
	 */
	@Nonnull
	DockSpace getSpace();

	/**
	 * Replaces this layout's docking space.
	 *
	 * @param space
	 * 		New docking space to display. A {@code null} will be mapped to {@link EmptyDockSpace}.
	 */
	void setSpace(@Nullable DockSpace space);
}
