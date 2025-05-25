package software.coley.bentofx.content;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Node;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.RegionBacked;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.RootContentLayout;
import software.coley.bentofx.path.ContentPath;
import software.coley.bentofx.path.LayoutPath;
import software.coley.bentofx.util.BentoUtils;

import java.util.List;

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

	/**
	 * @return Unmodifiable list of current dockables in this content.
	 */
	@Nonnull
	List<Dockable> getDockables();

	/**
	 * Adds the given dockable.
	 * <p/>
	 * Note: This ignores any restrictions such as cross-contamination of {@link Dockable#getDragGroup()}.
	 *
	 * @param dockable
	 * 		Dockable to add.
	 *
	 * @return {@code true} when added successfully.
	 * {@code false} when not added <i>(Due to already being present, or not supported by this content implementation)</i>.
	 */
	boolean addDockable(@Nonnull Dockable dockable);

	/**
	 * Removes the given dockable.
	 * <p/>
	 * Note: This ignores any restrictions such as {@link Dockable#closableProperty()}.
	 *
	 * @param dockable
	 * 		Dockable to remove.
	 *
	 * @return {@code true} when removed successfully.
	 * {@code false} when not removed <i>(Due to not being present, or not supported by this content implementation)</i>.
	 *
	 * @see #closeDockable(Dockable)
	 */
	boolean removeDockable(@Nonnull Dockable dockable);

	/**
	 * Closes the given dockable as if triggered by the user.
	 *
	 * @param dockable
	 * 		Dockable to close.
	 *
	 * @return {@code true} when closed successfully.
	 * {@code false} when not closed <i>(Due to not being present or closable, or not supported by this content implementation)</i>.
	 *
	 * @see #removeDockable(Dockable)
	 */
	boolean closeDockable(@Nonnull Dockable dockable);

	/**
	 * @return {@code true} when this content contains zero {@link Dockable} items.
	 */
	default boolean isEmpty() {
		return getDockables().isEmpty();
	}

	/**
	 * @param dockable
	 * 		Dockable to select.
	 *
	 * @return {@code true} when selected. {@code false} when not selected <i>(Due to not being present)</i>.
	 */
	boolean selectDockable(@Nullable Dockable dockable);
}
