package software.coley.bentofx.layout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import software.coley.bentofx.Bento;
import software.coley.bentofx.BentoBacked;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.RegionBacked;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.impl.layout.ImplLeafContentLayout;
import software.coley.bentofx.impl.layout.ImplSplitContentLayout;
import software.coley.bentofx.path.ContentPath;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.path.LayoutPath;
import software.coley.bentofx.path.PathBuilder;
import software.coley.bentofx.util.BentoUtils;
import software.coley.bentofx.util.DragDropStage;

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
	@Nullable
	default ContentLayout getParentLayout() {
		return BentoUtils.getParent(getBackingRegion(), ContentLayout.class);
	}

	@Nullable
	default RootContentLayout getRootLayout() {
		return BentoUtils.getOrParent(getBackingRegion(), RootContentLayout.class);
	}

	@Nullable
	default LayoutPath findLayout(@Nonnull PathBuilder builder, @Nonnull ContentLayout layout) {
		return findLayout(builder, layout.getIdentifier());
	}

	@Nullable
	LayoutPath findLayout(@Nonnull PathBuilder builder, @Nonnull String id);

	@Nullable
	default ContentPath findContent(@Nonnull PathBuilder builder, @Nonnull Content content) {
		return findContent(builder, content.getIdentifier());
	}

	@Nullable
	ContentPath findContent(@Nonnull PathBuilder builder, @Nonnull String id);

	@Nullable
	default DockablePath findDockable(@Nonnull PathBuilder builder, @Nonnull Dockable dockable) {
		return findDockable(builder, dockable.getIdentifier());
	}

	@Nullable
	DockablePath findDockable(@Nonnull PathBuilder builder, @Nonnull String id);


	boolean removeDockable(@Nonnull Dockable dockable);

	@Nonnull
	default ContentLayout asWithout(@Nonnull Content content) {
		return this;
	}

	@Nonnull
	default ContentLayout asSplitWith(@Nonnull Content content, @Nullable Side side) {
		return asSplitWith(new ImplLeafContentLayout(getBento(), content), side);
	}

	@Nonnull
	default ContentLayout asSplitWith(@Nonnull ContentLayout layout, @Nullable Side side) {
		final Bento bento = getBento();
		return switch (side) {
			case TOP -> new ImplSplitContentLayout(bento, Orientation.VERTICAL, layout, this);
			case BOTTOM -> new ImplSplitContentLayout(bento, Orientation.VERTICAL, this, layout);
			case LEFT -> new ImplSplitContentLayout(bento, Orientation.HORIZONTAL, layout, this);
			case RIGHT -> new ImplSplitContentLayout(bento, Orientation.HORIZONTAL, this, layout);
			case null -> new ImplSplitContentLayout(bento, Orientation.HORIZONTAL, this, layout);
		};
	}

	boolean replaceChildLayout(@Nonnull ContentLayout child, @Nonnull ContentLayout replacement);

	boolean removeChildLayout(@Nonnull ContentLayout child);

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

	@Nonnull
	List<ContentLayout> getChildLayouts();
}
