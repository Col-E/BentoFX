package software.coley.bentofx.layout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import software.coley.bentofx.Bento;
import software.coley.bentofx.BentoBacked;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.RegionBacked;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.impl.layout.ImplLeafContentLayout;
import software.coley.bentofx.impl.layout.ImplSplitContentLayout;
import software.coley.bentofx.path.ContentPath;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.path.LayoutPath;
import software.coley.bentofx.path.PathBuilder;
import software.coley.bentofx.util.BentoUtils;
import software.coley.bentofx.util.DragDropStage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Outlines the middle level of any docking-enabled layout.
 *
 * @author Matt Coley
 * @see Bento#newContentBuilder()
 * @see LeafContentLayout
 * @see SplitContentLayout
 */
public sealed interface ContentLayout extends Identifiable, RegionBacked, BentoBacked permits LeafContentLayout, SplitContentLayout {
	/**
	 * Lookup the parent layout of this layout.
	 * <ul>
	 *     <li>Will be {@code null} if the parent is the {@link RootContentLayout}.</li>
	 *     <li>Will be {@code null} if there is no {@link Node#getParent()}.</li>
	 * </ul>
	 *
	 * @return Parent layout containing this layout.
	 */
	@Nullable
	default ContentLayout getParentLayout() {
		return BentoUtils.getParent(getBackingRegion(), ContentLayout.class);
	}

	/**
	 * Lookup the root layout of this layout.
	 * <ul>
	 *     <li>Will be {@code null} if there is no chained {@link Node#getParent()} that is a {@link RootContentLayout}.</li>
	 * </ul>
	 *
	 * @return Root layout containing this layout.
	 */
	@Nullable
	default RootContentLayout getRootLayout() {
		return BentoUtils.getOrParent(getBackingRegion(), RootContentLayout.class);
	}

	/**
	 * @return Path to this layout from the root.
	 */
	@Nullable
	default LayoutPath getPath() {
		// Root layout must exist.
		RootContentLayout root = getRootLayout();
		if (root == null)
			return null;

		// Build path.
		if (getParentLayout() != null) {
			ContentLayout layout = this;
			List<ContentLayout> layoutHierarchy = new ArrayList<>(5);
			while (layout != null) {
				layoutHierarchy.addFirst(layout);
				layout = layout.getParentLayout();
			}
			return new LayoutPath(root, layoutHierarchy);
		}
		return new LayoutPath(root, Collections.singletonList(this));
	}

	/**
	 * Attempts to find a given {@link ContentLayout} from any child of any depth belonging to this layout.
	 * If a {@code null} is returned, then the given layout does not exist in any child belonging to this layout.
	 *
	 * @param layout
	 * 		Some layout to find.
	 *
	 * @return The path to the {@link ContentLayout} if found, otherwise {@code null}.
	 */
	@Nullable
	default LayoutPath findLayout(@Nonnull PathBuilder builder, @Nonnull ContentLayout layout) {
		return findLayout(builder, layout.getIdentifier());
	}

	/**
	 * Attempts to find a given {@link ContentLayout} from any child of any depth belonging to this layout.
	 * The identifier will be matched against {@link ContentLayout#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link ContentLayout} to find.
	 *
	 * @return The path to the {@link ContentLayout} if found, otherwise {@code null}.
	 */
	@Nullable
	LayoutPath findLayout(@Nonnull PathBuilder builder, @Nonnull String identifier);

	/**
	 * Attempts to find a given {@link Content} from any child {@link ContentLayout} of any depth belonging to this layout.
	 * If a {@code null} is returned, then the given content does not exist in any child {@link ContentLayout}
	 * belonging to this layout.
	 *
	 * @param content
	 * 		Some content to find.
	 *
	 * @return The path to the {@link Dockable} if found, otherwise {@code null}.
	 */
	@Nullable
	default ContentPath findContent(@Nonnull PathBuilder builder, @Nonnull Content content) {
		return findContent(builder, content.getIdentifier());
	}

	/**
	 * Attempts to find a given {@link Content} from any child {@link ContentLayout} of any depth belonging to this layout.
	 * The identifier will be matched against {@link Content#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link Content} to find.
	 *
	 * @return The path to the {@link Content} if found, otherwise {@code null}.
	 */
	@Nullable
	ContentPath findContent(@Nonnull PathBuilder builder, @Nonnull String identifier);

	/**
	 * Attempts to find a given {@link Dockable} from any child {@link Content} of any depth belonging to this layout.
	 * If a {@code null} is returned, then the given dockable does not exist in any child {@link ContentLayout}
	 * belonging to this layout.
	 *
	 * @param dockable
	 * 		Some dockable to find.
	 *
	 * @return The path to the {@link Dockable} if found, otherwise {@code null}.
	 */
	@Nullable
	default DockablePath findDockable(@Nonnull PathBuilder builder, @Nonnull Dockable dockable) {
		return findDockable(builder, dockable.getIdentifier());
	}

	/**
	 * Attempts to find a given {@link Dockable} from any child {@link Content} of any depth belonging to this layout.
	 * The identifier will be matched against {@link Dockable#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link Dockable} to find.
	 *
	 * @return The path to the {@link Dockable} if found, otherwise {@code null}.
	 */
	@Nullable
	DockablePath findDockable(@Nonnull PathBuilder builder, @Nonnull String identifier);

	/**
	 * Attempts to remove a given dockable from any child {@link Content} of any depth belonging to this layout.
	 * Be aware, this method will bypass {@link Dockable#closableProperty()}.
	 *
	 * @param dockable
	 * 		Dockable to remove.
	 *
	 * @return {@code true} if removed. {@code false} if not removed.
	 */
	boolean removeDockable(@Nonnull Dockable dockable);

	/**
	 * @param content
	 * 		Content to split this with.
	 * @param side
	 * 		Side to put the other content on.
	 *
	 * @return A split content layout containing the current layout and the given content on the given side.
	 */
	@Nonnull
	default SplitContentLayout asSplitWith(@Nonnull Content content, @Nullable Side side) {
		return asSplitWith(new ImplLeafContentLayout(getBento(), content), side);
	}

	/**
	 * @param layout
	 * 		Layout to split this with.
	 * @param side
	 * 		Side to put the other content on.
	 *
	 * @return A split content layout containing the current layout and the given layout on the given side.
	 */
	@Nonnull
	default SplitContentLayout asSplitWith(@Nonnull ContentLayout layout, @Nullable Side side) {
		final Bento bento = getBento();
		return switch (side) {
			case TOP -> new ImplSplitContentLayout(bento, Orientation.VERTICAL, layout, this);
			case BOTTOM -> new ImplSplitContentLayout(bento, Orientation.VERTICAL, this, layout);
			case LEFT -> new ImplSplitContentLayout(bento, Orientation.HORIZONTAL, layout, this);
			case RIGHT -> new ImplSplitContentLayout(bento, Orientation.HORIZONTAL, this, layout);
			case null -> new ImplSplitContentLayout(bento, Orientation.HORIZONTAL, this, layout);
		};
	}

	/**
	 * @param child
	 * 		Child layout to replace.
	 * @param replacement
	 * 		Replacement layout.
	 *
	 * @return {@code true} when the child was found and replaced with the given replacement.
	 */
	boolean replaceChildLayout(@Nonnull ContentLayout child, @Nonnull ContentLayout replacement);

	/**
	 * @param child
	 * 		Child layout to remove.
	 *
	 * @return {@code true} when the child was found and removed.
	 */
	boolean removeChildLayout(@Nonnull ContentLayout child);

	/**
	 * Attempts to remove this layout in the parent <i>(or root)</i> layout.
	 *
	 * @return {@code true} when this layout successfully removed itself from its parent.
	 */
	default boolean removeFromParent() {
		ContentLayout parent = getParentLayout();
		if (parent != null) {
			return parent.removeChildLayout(this);
		} else {
			RootContentLayout root = getRootLayout();
			if (root != null) {
				Region region = root.getBackingRegion();

				// Check and see if the root content is the top-level scene graph node.
				// If it is, check and see if we want to auto-close the window when it is empty.
				if (region.getParent() == null) {
					Scene scene = region.getScene();
					if (scene != null && scene.getWindow() instanceof DragDropStage dds && dds.isAutoCloseWhenEmpty()) {
						// Our stage will auto-unregister any relevant roots.
						dds.close();
						return true;
					}
				}

				// Otherwise, just clear the layout and show an empty template.
				root.clearLayout();
				return true;
			}
		}
		return false;
	}

	/**
	 * @return List of child layouts contained within this layout.
	 */
	@Nonnull
	List<ContentLayout> getChildLayouts();
}
