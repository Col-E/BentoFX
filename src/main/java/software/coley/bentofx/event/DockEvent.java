package software.coley.bentofx.event;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;

/**
 * Outline of all docking events.
 *
 * @author Matt Coley
 */
public sealed interface DockEvent {
	/**
	 * @param container
	 * 		Root container added.
	 */
	record RootContainerAdded(@Nonnull DockContainer container) implements DockEvent {}

	/**
	 * @param container
	 * 		Root container removed.
	 */
	record RootContainerRemoved(@Nonnull DockContainer container) implements DockEvent {}

	/**
	 * @param container
	 * 		Container being updated.
	 * @param priorParent
	 * 		The container's prior parent.
	 * @param newParent
	 * 		The container's new parent.
	 */
	record ContainerParentChanged(@Nonnull DockContainer container, @Nullable DockContainerBranch priorParent,
	                              @Nullable DockContainerBranch newParent) implements DockEvent {}

	/**
	 * @param container
	 * 		Container being updated.
	 * @param child
	 * 		Child added to the container.
	 */
	record ContainerChildAdded(@Nonnull DockContainerBranch container,
	                           @Nonnull DockContainer child) implements DockEvent {}

	/**
	 * @param container
	 * 		Container being updated.
	 * @param child
	 * 		Child removed from the container.
	 */
	record ContainerChildRemoved(@Nonnull DockContainerBranch container,
	                             @Nonnull DockContainer child) implements DockEvent {}

	/**
	 * @param dockable
	 * 		Dockable added.
	 * @param container
	 * 		Container the dockable was added to.
	 */
	record DockableAdded(@Nonnull Dockable dockable, @Nonnull DockContainerLeaf container) implements DockEvent {}

	/**
	 * @param dockable
	 * 		Dockable being closed.
	 * @param container
	 * 		Container the dockable belongs to.
	 */
	record DockableClosing(@Nonnull Dockable dockable, @Nonnull DockContainerLeaf container) implements DockEvent {}

	/**
	 * @param dockable
	 * 		Dockable being removed.
	 * @param container
	 * 		Container the dockable belonged to.
	 */
	record DockableRemoved(@Nonnull Dockable dockable, @Nonnull DockContainerLeaf container) implements DockEvent {}

	/**
	 * @param dockable
	 * 		Dockable being selected.
	 * @param container
	 * 		Container the dockable belongs to.
	 */
	record DockableSelected(@Nonnull Dockable dockable, @Nonnull DockContainerLeaf container) implements DockEvent {}

	/**
	 * @param dockable
	 * 		Dockable being updated.
	 * @param priorParent
	 * 		Dockable's prior parent.
	 * @param newParent
	 * 		Dockable's new parent.
	 */
	record DockableParentChanged(@Nonnull Dockable dockable, @Nullable DockContainerLeaf priorParent,
	                             @Nullable DockContainerLeaf newParent) implements DockEvent {}
}
