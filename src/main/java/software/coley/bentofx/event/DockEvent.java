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
	final class DockableClosing implements DockEvent {
		private final @Nonnull Dockable dockable;
		private final @Nonnull DockContainerLeaf container;

		private boolean preventDefault = false;

		public DockableClosing(@Nonnull Dockable dockable, @Nonnull DockContainerLeaf container) {
			this.dockable = dockable;
			this.container = container;
		}

		@Nonnull
		public Dockable dockable() {
			return dockable;
		}

		@Nonnull
		public DockContainerLeaf container() {
			return container;
		}

		public void preventDefault() {
			this.preventDefault = true;
		}

		public boolean shouldPreventDefault() {
			return preventDefault;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof DockableClosing that)) {
				return false;
			}
			return dockable.equals(that.dockable)
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
			return "DockableClosing[dockable=" + dockable + ", container=" + container + ", preventDefault=" + preventDefault + "]";
		}
	}

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
