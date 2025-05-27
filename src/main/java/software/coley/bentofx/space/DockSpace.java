package software.coley.bentofx.space;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Node;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.RegionBacked;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockLayout;
import software.coley.bentofx.layout.RootDockLayout;
import software.coley.bentofx.path.SpacePath;
import software.coley.bentofx.path.LayoutPath;
import software.coley.bentofx.util.BentoUtils;

import java.util.List;

/**
 * Outlines the lowest level of any docking-enabled layout.
 *
 * @author Matt Coley
 * @see Bento#newLayoutBuilder()
 * @see EmptyDockSpace
 * @see SingleDockSpace
 * @see TabbedDockSpace
 */
public sealed interface DockSpace extends RegionBacked, Identifiable permits EmptyDockSpace, SingleDockSpace, TabbedDockSpace {
	/**
	 * Lookup the parent layout of this space.
	 * <ul>
	 *     <li>Will be {@code null} if there is no {@link Node#getParent()}.</li>
	 * </ul>
	 *
	 * @return Immediate parent layout containing this space.
	 */
	@Nullable
	default DockLayout getParentLayout() {
		return BentoUtils.getParent(getBackingRegion(), DockLayout.class);
	}

	/**
	 * Lookup the root layout of this space.
	 * <ul>
	 *     <li>Will be {@code null} if there is no chained {@link Node#getParent()} that is a {@link RootDockLayout}.</li>
	 * </ul>
	 *
	 * @return Root layout containing this space.
	 */
	@Nullable
	default RootDockLayout getRootLayout() {
		return BentoUtils.getOrParent(getBackingRegion(), RootDockLayout.class);
	}

	/**
	 * @return Path to this space from the root.
	 */
	@Nullable
	default SpacePath getPath() {
		DockLayout layout = getParentLayout();
		if (layout == null)
			return null;

		LayoutPath layoutPath = layout.getPath();
		if (layoutPath == null)
			return null;

		return new SpacePath(layoutPath, this);
	}

	/**
	 * @return Unmodifiable list of current dockables in this space.
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
	 * {@code false} when not added <i>(Due to already being present, or not supported by this space implementation)</i>.
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
	 * {@code false} when not removed <i>(Due to not being present, or not supported by this space implementation)</i>.
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
	 * {@code false} when not closed <i>(Due to not being present or closable, or not supported by this space implementation)</i>.
	 *
	 * @see #removeDockable(Dockable)
	 */
	boolean closeDockable(@Nonnull Dockable dockable);

	/**
	 * @return {@code true} when this space contains zero {@link Dockable} items.
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
