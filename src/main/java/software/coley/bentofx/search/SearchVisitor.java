package software.coley.bentofx.search;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;

/**
 * Visitor model to traverse {@link DockContainer} hierarchies.
 *
 * @author Matt Coley
 */
public interface SearchVisitor {
	/**
	 * @param container
	 * 		Container to visit.
	 *
	 * @return {@code true} to continue visitation.
	 */
	default boolean visitBranch(@Nonnull DockContainerBranch container) {
		return true;
	}

	/**
	 * @param container
	 * 		Container to visit.
	 *
	 * @return {@code true} to continue visitation.
	 */
	default boolean visitLeaf(@Nonnull DockContainerLeaf container) {
		return true;
	}

	/**
	 * @param dockable
	 * 		Dockable to visit.
	 *
	 * @return {@code true} to continue visitation.
	 */
	default boolean visitDockable(@Nonnull Dockable dockable) {
		return true;
	}
}
