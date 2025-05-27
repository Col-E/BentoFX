package software.coley.bentofx.layout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.RegionBacked;
import software.coley.bentofx.space.DockSpace;
import software.coley.bentofx.space.EmptyDockSpace;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.path.SpacePath;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.path.LayoutPath;
import software.coley.bentofx.path.PathBuilder;

/**
 * This is the top level of any docking-enabled layout. By itself it acts very similar to {@link LeafDockLayout}
 * except that this houses a {@link DockLayout} as its sole child.
 *
 * @author Matt Coley
 * @see Bento#newLayoutBuilder()
 */
public interface RootDockLayout extends Identifiable, RegionBacked {
	/**
	 * Attempts to find a given {@link DockLayout} from any child of any depth belonging to this root.
	 * If a {@code null} is returned, then the given layout does not exist in any child belonging to this root.
	 *
	 * @param layout
	 * 		Some layout to find.
	 *
	 * @return The path to the {@link DockLayout} if found, otherwise {@code null}.
	 */
	@Nullable
	default LayoutPath findLayout(@Nonnull DockLayout layout) {
		return findLayout(layout.getIdentifier());
	}

	/**
	 * Attempts to find a given {@link DockLayout} from any child of any depth belonging to this root.
	 * The identifier will be matched against {@link DockLayout#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link DockLayout} to find.
	 *
	 * @return The path to the {@link DockLayout} if found, otherwise {@code null}.
	 */
	@Nullable
	default LayoutPath findLayout(@Nonnull String identifier) {
		return getLayout().findLayout(newPathBuilder(), identifier);
	}

	/**
	 * Attempts to find a given {@link DockSpace} from any child {@link DockLayout} of any depth belonging to this root.
	 * If a {@code null} is returned, then the given content does not exist in any child {@link DockLayout}
	 * belonging to this root.
	 *
	 * @param space
	 * 		Some space to find.
	 *
	 * @return The path to the {@link Dockable} if found, otherwise {@code null}.
	 */
	@Nullable
	default SpacePath findSpace(@Nonnull DockSpace space) {
		return findSpace(space.getIdentifier());
	}

	/**
	 * Attempts to find a given {@link DockSpace} from any child {@link DockLayout} of any depth belonging to this root.
	 * The identifier will be matched against {@link DockSpace#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link DockSpace} to find.
	 *
	 * @return The path to the {@link DockSpace} if found, otherwise {@code null}.
	 */
	@Nullable
	default SpacePath findSpace(@Nonnull String identifier) {
		return getLayout().findSpace(newPathBuilder(), identifier);
	}

	/**
	 * Attempts to find a given {@link Dockable} from any child {@link DockSpace} of any depth belonging to this root.
	 * If a {@code null} is returned, then the given dockable does not exist in any child {@link DockLayout}
	 * belonging to this root.
	 *
	 * @param dockable
	 * 		Some dockable to find.
	 *
	 * @return The path to the {@link Dockable} if found, otherwise {@code null}.
	 */
	@Nullable
	default DockablePath findDockable(@Nonnull Dockable dockable) {
		return findDockable(dockable.getIdentifier());
	}

	/**
	 * Attempts to find a given {@link Dockable} from any child {@link DockSpace} of any depth belonging to this root.
	 * The identifier will be matched against {@link Dockable#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link Dockable} to find.
	 *
	 * @return The path to the {@link Dockable} if found, otherwise {@code null}.
	 */
	@Nullable
	default DockablePath findDockable(@Nonnull String identifier) {
		return getLayout().findDockable(newPathBuilder(), identifier);
	}

	/**
	 * Attempts to remove the given dockable from any child {@link DockSpace} of any depth belonging to this root.
	 * Be aware, this method will bypass {@link Dockable#closableProperty()}.
	 *
	 * @param dockable
	 * 		Dockable to remove.
	 *
	 * @return {@code true} if removed. {@code false} if not removed.
	 */
	default boolean removeDockable(@Nonnull Dockable dockable) {
		return getLayout().removeDockable(dockable);
	}

	/**
	 * Attempts to close the given dockable from any child {@link DockSpace} of any depth belonging to this root.
	 *
	 * @param dockable
	 * 		Dockable to close.
	 *
	 * @return {@code true} if closed. {@code false} if not closed.
	 */
	default boolean closeDockable(@Nonnull Dockable dockable) {
		return getLayout().closeDockable(dockable);
	}

	/**
	 * Updates the current child layout.
	 *
	 * @param layout
	 * 		The new child layout to assign.
	 */
	void setLayout(@Nonnull DockLayout layout);

	/**
	 * @return The current child layout.
	 */
	@Nonnull
	DockLayout getLayout();

	/**
	 * Removes any {@link #getLayout() child layot} and replaces it with a {@link LeafDockLayout}
	 * containing a single {@link EmptyDockSpace}.
	 */
	void clearLayout();

	/**
	 * @return New path builder for use in {@code findX(...)} methods.
	 */
	@Nonnull
	default PathBuilder newPathBuilder() {
		return new PathBuilder(this);
	}
}
