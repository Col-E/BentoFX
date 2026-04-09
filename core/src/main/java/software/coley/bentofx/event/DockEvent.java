package software.coley.bentofx.event;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.bentofx.Bento;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;

/**
 * Outline of all docking events.
 *
 * @author Matt Coley
 */
public sealed interface DockEvent {
	/**
	 * Event for when a {@link DockContainer} is {@link Bento#registerRoot(DockContainerRootBranch) registered}.
	 *
	 * @param container
	 * 		Root container added.
	 */
	record RootContainerAdded(@Nonnull DockContainer container) implements DockEvent {}

	/**
	 * Event for when a {@link DockContainer} is {@link Bento#unregisterRoot(DockContainerRootBranch) uunregistered}.
	 *
	 * @param container
	 * 		Root container removed.
	 */
	record RootContainerRemoved(@Nonnull DockContainer container) implements DockEvent {}

	/**
	 * Event for when a {@link DockContainer}'s parent is changed.
	 *
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
	 * Event for when a {@link DockContainerBranch} adds a child {@link DockContainer}.
	 *
	 * @param container
	 * 		Container being updated.
	 * @param child
	 * 		Child added to the container.
	 */
	record ContainerChildAdded(@Nonnull DockContainerBranch container,
	                           @Nonnull DockContainer child) implements DockEvent {}

	/**
	 * Event for when a {@link DockContainerBranch} removes a child {@link DockContainer}.
	 *
	 * @param container
	 * 		Container being updated.
	 * @param child
	 * 		Child removed from the container.
	 */
	record ContainerChildRemoved(@Nonnull DockContainerBranch container,
	                             @Nonnull DockContainer child) implements DockEvent {}

	/**
	 * Event for when a {@link DockContainerLeaf} adds a {@link Dockable} item.
	 *
	 * @param container
	 * 		Container the dockable was added to.
	 * @param dockable
	 * 		Dockable added.
	 */
	record DockableAdded(@Nonnull DockContainerLeaf container, @Nonnull Dockable dockable) implements DockEvent {}

	/**
	 * Event for when a {@link DockContainerLeaf} closes a {@link Dockable} item.
	 * Can be cancelled to prevent closure.
	 */
	final class DockableClosing implements DockEvent {
		private final @Nonnull Dockable dockable;
		private final @Nonnull DockContainerLeaf container;
		private boolean cancelled;

		/**
		 * @param dockable
		 * 		Dockable being closed.
		 * @param container
		 * 		Container the dockable belongs to.
		 */
		public DockableClosing(@Nonnull Dockable dockable, @Nonnull DockContainerLeaf container) {
			this.dockable = dockable;
			this.container = container;
		}

		/**
		 * @return Dockable being closed.
		 */
		@Nonnull
		public Dockable dockable() {
			return dockable;
		}

		/**
		 * @return Container the dockable belongs to.
		 */
		@Nonnull
		public DockContainerLeaf container() {
			return container;
		}

		/**
		 * Cancel closing this dockable.
		 */
		public void cancel() {
			cancelled = true;
		}

		/**
		 * @return {@code true} when this closure has been cancelled.
		 */
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof DockableClosing that)) return false;
			return cancelled == that.cancelled
					&& dockable.equals(that.dockable)
					&& container.equals(that.container);
		}

		@Override
		public int hashCode() {
			int result = dockable.hashCode();
			result = 31 * result + container.hashCode();
			return result;
		}

		@Override
		public String toString() {
			return "DockableClosing[" +
					"dockable=" + dockable +
					", container=" + container +
					", shouldClose=" + cancelled +
					"]";
		}
	}

	/**
	 * Event for when a {@link DockContainerLeaf} removes a {@link Dockable} item.
	 *
	 * @param dockable
	 * 		Dockable being removed.
	 * @param container
	 * 		Container the dockable belonged to.
	 */
	record DockableRemoved(@Nonnull Dockable dockable, @Nonnull DockContainerLeaf container) implements DockEvent {}

	/**
	 * Event for when a {@link DockContainerLeaf} updates its selected {@link Dockable} item.
	 *
	 * @param dockable
	 * 		Dockable being selected.
	 * @param container
	 * 		Container the dockable belongs to.
	 */
	record DockableSelected(@Nonnull Dockable dockable, @Nonnull DockContainerLeaf container) implements DockEvent {}

	/**
	 * Event for when a {@link Dockable}'s parent is changed.
	 *
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
