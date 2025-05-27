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
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.impl.layout.ImplLeafDockLayout;
import software.coley.bentofx.impl.layout.ImplSplitDockLayout;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.path.LayoutPath;
import software.coley.bentofx.path.PathBuilder;
import software.coley.bentofx.path.SpacePath;
import software.coley.bentofx.space.DockSpace;
import software.coley.bentofx.util.BentoUtils;
import software.coley.bentofx.util.DragDropStage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Outlines the middle level of any docking-enabled layout.
 *
 * @author Matt Coley
 * @see Bento#newLayoutBuilder()
 * @see LeafDockLayout
 * @see SplitDockLayout
 */
public sealed interface DockLayout extends Identifiable, RegionBacked, BentoBacked permits LeafDockLayout, SplitDockLayout {
	/**
	 * Lookup the parent layout of this layout.
	 * <ul>
	 *     <li>Will be {@code null} if the parent is the {@link RootDockLayout}.</li>
	 *     <li>Will be {@code null} if there is no {@link Node#getParent()}.</li>
	 * </ul>
	 *
	 * @return Parent layout containing this layout.
	 */
	@Nullable
	default DockLayout getParentLayout() {
		return BentoUtils.getParent(getBackingRegion(), DockLayout.class);
	}

	/**
	 * Lookup the root layout of this layout.
	 * <ul>
	 *     <li>Will be {@code null} if there is no chained {@link Node#getParent()} that is a {@link RootDockLayout}.</li>
	 * </ul>
	 *
	 * @return Root layout containing this layout.
	 */
	@Nullable
	default RootDockLayout getRootLayout() {
		return BentoUtils.getOrParent(getBackingRegion(), RootDockLayout.class);
	}

	/**
	 * @return Path to this layout from the root.
	 */
	@Nullable
	default LayoutPath getPath() {
		// Root layout must exist.
		RootDockLayout root = getRootLayout();
		if (root == null)
			return null;

		// Build path.
		if (getParentLayout() != null) {
			DockLayout layout = this;
			List<DockLayout> layoutHierarchy = new ArrayList<>(5);
			while (layout != null) {
				layoutHierarchy.addFirst(layout);
				layout = layout.getParentLayout();
			}
			return new LayoutPath(root, layoutHierarchy);
		}
		return new LayoutPath(root, Collections.singletonList(this));
	}

	/**
	 * Attempts to find a given {@link DockLayout} from any child of any depth belonging to this layout.
	 * If a {@code null} is returned, then the given layout does not exist in any child belonging to this layout.
	 *
	 * @param layout
	 * 		Some layout to find.
	 *
	 * @return The path to the {@link DockLayout} if found, otherwise {@code null}.
	 */
	@Nullable
	default LayoutPath findLayout(@Nonnull PathBuilder builder, @Nonnull DockLayout layout) {
		return findLayout(builder, layout.getIdentifier());
	}

	/**
	 * Attempts to find a given {@link DockLayout} from any child of any depth belonging to this layout.
	 * The identifier will be matched against {@link DockLayout#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link DockLayout} to find.
	 *
	 * @return The path to the {@link DockLayout} if found, otherwise {@code null}.
	 */
	@Nullable
	LayoutPath findLayout(@Nonnull PathBuilder builder, @Nonnull String identifier);

	/**
	 * Attempts to find a given {@link DockSpace} from any child {@link DockLayout} of any depth belonging to this layout.
	 * If a {@code null} is returned, then the given space does not exist in any child {@link DockLayout}
	 * belonging to this layout.
	 *
	 * @param space
	 * 		Some space to find.
	 *
	 * @return The path to the {@link Dockable} if found, otherwise {@code null}.
	 */
	@Nullable
	default SpacePath findSpace(@Nonnull PathBuilder builder, @Nonnull DockSpace space) {
		return findSpace(builder, space.getIdentifier());
	}

	/**
	 * Attempts to find a given {@link DockSpace} from any child {@link DockLayout} of any depth belonging to this layout.
	 * The identifier will be matched against {@link DockSpace#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link DockSpace} to find.
	 *
	 * @return The path to the {@link DockSpace} if found, otherwise {@code null}.
	 */
	@Nullable
	SpacePath findSpace(@Nonnull PathBuilder builder, @Nonnull String identifier);

	/**
	 * Attempts to find a given {@link Dockable} from any child {@link DockSpace} of any depth belonging to this layout.
	 * If a {@code null} is returned, then the given dockable does not exist in any child {@link DockLayout}
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
	 * Attempts to find a given {@link Dockable} from any child {@link DockSpace} of any depth belonging to this layout.
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
	 * Attempts to remove the given dockable from any child {@link DockSpace} of any depth belonging to this layout.
	 * Be aware, this method will bypass {@link Dockable#closableProperty()}.
	 *
	 * @param dockable
	 * 		Dockable to remove.
	 *
	 * @return {@code true} if removed. {@code false} if not removed.
	 */
	boolean removeDockable(@Nonnull Dockable dockable);

	/**
	 * Attempts to close the given dockable from any child {@link DockSpace} of any depth belonging to this layout.
	 *
	 * @param dockable
	 * 		Dockable to close.
	 *
	 * @return {@code true} if close. {@code false} if not close.
	 */
	boolean closeDockable(@Nonnull Dockable dockable);

	/**
	 * @param space
	 * 		Space to split this with.
	 * @param side
	 * 		Side to put the other space on.
	 *
	 * @return A split layout containing the current layout and the given space on the given side.
	 */
	@Nonnull
	default SplitDockLayout asSplitWith(@Nonnull DockSpace space, @Nullable Side side) {
		return asSplitWith(new ImplLeafDockLayout(getBento(), space), side);
	}

	/**
	 * @param layout
	 * 		Layout to split this with.
	 * @param side
	 * 		Side to put the other space on.
	 *
	 * @return A split layout containing the current layout and the given layout on the given side.
	 */
	@Nonnull
	default SplitDockLayout asSplitWith(@Nonnull DockLayout layout, @Nullable Side side) {
		final Bento bento = getBento();
		return switch (side) {
			case TOP -> new ImplSplitDockLayout(bento, Orientation.VERTICAL, layout, this);
			case BOTTOM -> new ImplSplitDockLayout(bento, Orientation.VERTICAL, this, layout);
			case LEFT -> new ImplSplitDockLayout(bento, Orientation.HORIZONTAL, layout, this);
			case RIGHT -> new ImplSplitDockLayout(bento, Orientation.HORIZONTAL, this, layout);
			case null -> new ImplSplitDockLayout(bento, Orientation.HORIZONTAL, this, layout);
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
	boolean replaceChildLayout(@Nonnull DockLayout child, @Nonnull DockLayout replacement);

	/**
	 * @param child
	 * 		Child layout to remove.
	 *
	 * @return {@code true} when the child was found and removed.
	 */
	boolean removeChildLayout(@Nonnull DockLayout child);

	/**
	 * Attempts to remove this layout in the parent <i>(or root)</i> layout.
	 *
	 * @return {@code true} when this layout successfully removed itself from its parent.
	 */
	default boolean removeFromParent() {
		DockLayout parent = getParentLayout();
		if (parent != null) {
			return parent.removeChildLayout(this);
		} else {
			RootDockLayout root = getRootLayout();
			if (root != null) {
				Region region = root.getBackingRegion();

				// Check and see if the root layout is the top-level scene graph node.
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
	List<DockLayout> getChildLayouts();
}
