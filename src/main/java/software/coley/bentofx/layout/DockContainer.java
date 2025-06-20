package software.coley.bentofx.layout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.layout.Region;
import software.coley.bentofx.BentoBacked;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.path.DockContainerPath;
import software.coley.bentofx.search.SearchVisitor;

import java.util.Collections;
import java.util.List;

/**
 * Outlines the types of containers housing dockable content.
 *
 * @author Matt Coley
 * @see DockContainerBranch
 * @see DockContainerLeaf
 */
public sealed interface DockContainer extends BentoBacked, Identifiable permits DockContainerBranch, DockContainerLeaf {
	/**
	 * @return Path to this container from the root container that holds this container.
	 */
	@Nonnull
	default DockContainerPath getPath() {
		DockContainer parent = getParentContainer();
		if (parent != null)
			return parent.getPath().withChild(this);
		return new DockContainerPath(Collections.singletonList(this));
	}

	/**
	 * @return Parent container that holds this container. {@code null} when this container is a root.
	 */
	@Nullable
	DockContainerBranch getParentContainer();

	/**
	 * Record the given container as this container's parent.
	 * Does not actually mutate the hierarchy and is just for state tracking.
	 *
	 * @param container
	 * 		Container to assign as this container's parent.
	 */
	void setParentContainer(@Nonnull DockContainerBranch container);

	/**
	 * Remove the given container as this container's parent.
	 * Does not actually mutate the hierarchy and is just for state tracking.
	 *
	 * @param parent
	 * 		Container to remove as this container's parent.
	 */
	void removeAsParentContainer(@Nonnull DockContainerBranch parent);

	/**
	 * @param visitor
	 * 		Visitor to control continued traversal.
	 *
	 * @return {@code true} when the visit shall continue.
	 */
	boolean visit(@Nonnull SearchVisitor visitor);

	/**
	 * @return Unmodifiable list of dockables within this container.
	 */
	@Nonnull
	List<Dockable> getDockables();

	/**
	 * @param dockables
	 * 		Dockables to add.
	 *
	 * @return {@code true} if one or more of the dockables were added.
	 */
	default boolean addDockables(@Nonnull Dockable... dockables) {
		boolean changed = false;
		for (Dockable dockable : dockables)
			changed |= addDockable(dockable);
		return changed;
	}

	/**
	 * @param dockable
	 * 		Dockable to add.
	 *
	 * @return {@code true} when added.
	 */
	boolean addDockable(@Nonnull Dockable dockable);

	/**
	 * @param dockable
	 * 		Dockable to add.
	 * @param index
	 * 		Index to add the dockable at.
	 *
	 * @return {@code true} when added.
	 */
	boolean addDockable(int index, @Nonnull Dockable dockable);

	/**
	 * @param dockable
	 * 		Dockable to remove.
	 *
	 * @return {@code true} when removed.
	 */
	boolean removeDockable(@Nonnull Dockable dockable);

	/**
	 * @param dockable
	 * 		Dockable to close and then remove.
	 *
	 * @return {@code true} when removed.
	 */
	boolean closeDockable(@Nonnull Dockable dockable);

	/**
	 * Remove this container within the {@link #getParentContainer() parent container}.
	 *
	 * @return {@code true} when removed.
	 */
	default boolean removeFromParent() {
		DockContainerBranch parent = getParentContainer();
		if (parent != null)
			return parent.removeContainer(this);
		return false;
	}

	/**
	 * @return {@code true} to {@link #removeFromParent() prune} when this container has no remaining dockables.
	 */
	boolean doPruneWhenEmpty();

	/**
	 * @param pruneWhenEmpty
	 *        {@code true} to {@link #removeFromParent() prune} when this container has no remaining dockables.
	 */
	void setPruneWhenEmpty(boolean pruneWhenEmpty);

	/**
	 * @return Self, cast to region.
	 */
	@Nonnull
	default Region asRegion() {
		return (Region) this;
	}
}
