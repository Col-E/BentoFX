package software.coley.bentofx;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.geometry.Side;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Region;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.SplitContentLayout;

import java.util.List;

/**
 * Outline of a region that a {@link Header} can be dropped into.
 *
 * @author Matt Coley
 */
public interface DockableDestination extends RegionBacked, Identifiable {
	/**
	 * @return First immediate {@link Content}.
	 */
	@Nullable
	Content getParentContent();

	/**
	 * @return First immediate {@link ContentLayout}.
	 */
	@Nullable
	ContentLayout getParentLayout();

	/**
	 * @return Contained {@link Dockable} items.
	 */
	@Nonnull
	List<Dockable> getDockables();

	/**
	 * @return Currently selected / visible dockable in this space.
	 */
	@Nullable
	Dockable getSelectedDockable();

	/**
	 * @param dragGroup
	 * 		Some arbitrary {@link Dockable#getDragGroup()}.
	 *
	 * @return {@code true} when this region will allow the given drag group to be dropped here.
	 */
	boolean canReceiveDragGroup(int dragGroup);

	/**
	 * @return {@code true} if this region allows splitting this space into {@link SplitContentLayout}.
	 */
	boolean canSplit();

	/**
	 * @param droppedSide
	 * 		Closest side of this space the header was dragged over.
	 * @param source
	 * 		Header being dragged.
	 *
	 * @return {@code true} when the given header can be dropped here.
	 *
	 * @see #receiveDroppedHeader(DragEvent, Side, Header)
	 */
	boolean canReceiveHeader(@Nullable Side droppedSide, @Nonnull Header source);

	/**
	 * @param event
	 * 		Drag event for {@link DragEvent#DRAG_DROPPED}.
	 * @param droppedSide
	 * 		Closest side of this space the header was dragged over.
	 * @param source
	 * 		Header being dropped.
	 *
	 * @return {@code true} when the given header was successfully dropped here.
	 *
	 * @see #canReceiveHeader(Side, Header)
	 */
	boolean receiveDroppedHeader(@Nonnull DragEvent event, @Nullable Side droppedSide, @Nonnull Header source);

	/**
	 * @param dockable
	 * 		Dockable to add.
	 *
	 * @return {@code true} when the dockable was added successfully.
	 * {@code false} generally implies the dockable already is present in this space.
	 */
	boolean addDockable(@Nonnull Dockable dockable);

	/**
	 * @param index
	 * 		Index to add the dockable at.
	 * @param dockable
	 * 		Dockable to add.
	 *
	 * @return {@code true} when the dockable was added successfully.
	 * {@code false} generally implies the dockable already is present in this space.
	 */
	boolean addDockable(int index, @Nonnull Dockable dockable);

	/**
	 * @param dockable
	 * 		Dockable to remove.
	 *
	 * @return {@code true} when the dockable was removed successfully.
	 * {@code false} generally implies the dockable was not present in this space.
	 */
	boolean removeDockable(@Nonnull Dockable dockable);

	/**
	 * @param dockable
	 * 		Dockable to select.
	 *
	 * @return {@code true} when the dockable was selected successfully.
	 * {@code false} generally implies the dockable was not present in this space.
	 */
	boolean selectDockable(@Nonnull Dockable dockable);

	/**
	 * Draws a rectangle over the given region.
	 *
	 * @param target
	 * 		Region to draw over on the canvas.
	 *
	 * @see #clearCanvas()
	 */
	default void drawCanvasHint(@Nonnull Region target) {
		drawCanvasHint(target, null);
	}

	/**
	 * Draws a rectangle over the given side of the given region.
	 *
	 * @param target
	 * 		Region to draw over on the canvas.
	 * @param side
	 * 		Side to draw over, or {@code null} to cover the full region.
	 *
	 * @see #clearCanvas()
	 */
	void drawCanvasHint(@Nonnull Region target, @Nullable Side side);

	/**
	 * Clears the canvas.
	 *
	 * @see #drawCanvasHint(Region, Side)
	 */
	void clearCanvas();

	/**
	 * Toggle this destination being <i>"collapsed"</i>.
	 * This is only supported if this space is on the edge of a parent {@link SplitContentLayout}.
	 *
	 * @see #isCollapsed()
	 */
	void toggleCollapsed();

	/**
	 * @return Current collapsed status of this space.
	 *
	 * @see #toggleCollapsed()
	 */
	boolean isCollapsed();
}
