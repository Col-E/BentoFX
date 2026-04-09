package software.coley.bentofx.dockable;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.geometry.Side;
import software.coley.bentofx.layout.container.DockContainerLeaf;

/**
 * Drag drop operations.
 *
 * @author Matt Coley
 */
public interface DockableDragDropBehavior {
	/**
	 * Determines if a given dockable can be placed into a container.
	 * Generally this is used to control how the {@link Dockable#getDragGroupMask()} behaves.
	 * You can override this method to support alternative grouping models.
	 * <p/>
	 * For example, the default implementation is a simple equality check.
	 * Any dockable can be put into a container that has other dockables of the same mask.
	 * <pre>{@code
	 * return targetContainer.getDockables().stream()
	 * 		.anyMatch(d -> d.getDragMask() == dockable.getDragMask());
	 * }</pre>
	 * <p/>
	 * As an alternative, you can make the mask... more like a mask!
	 * In this example, drag groups are specified as bit-masks, allowing more fine-control over
	 * what can go where.
	 * <pre>{@code
	 * return targetContainer.getDockables().stream()
	 * 		.anyMatch(d -> (d.getDragMask() & dockable.getDragMask()) != 0);
	 * }</pre>
	 *
	 * @param targetContainer
	 * 		Target container the dockable is dragged over.
	 * @param targetSide
	 * 		The side the dockable will be dropped to as part of a DnD operation into the target container.
	 * @param dockable
	 * 		Some dockable being dragged.
	 *
	 * @return {@code true} when this container can receive the dockable.
	 */
	default boolean canReceiveDockable(@Nonnull DockContainerLeaf targetContainer,
	                                   @Nullable Side targetSide,
	                                   @Nonnull Dockable dockable) {
		// The incoming dockable must have a compatible group.
		return targetContainer.getDockables().stream()
				.anyMatch(d -> d.getDragGroupMask() == dockable.getDragGroupMask());
	}
}
