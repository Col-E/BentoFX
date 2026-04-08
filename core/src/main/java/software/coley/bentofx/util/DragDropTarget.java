package software.coley.bentofx.util;

import software.coley.bentofx.control.Header;
import software.coley.bentofx.layout.container.DockContainerLeaf;

/**
 * Type of drag-n-drop target for the completion of a {@link Header}'s drag operation.
 *
 * @author Matt Coley
 */
public enum DragDropTarget {
	/**
	 * Drag-n-drop completed on a {@link Header}.
	 */
	HEADER,
	/**
	 * Drag-n-drop completed on a {@link DockContainerLeaf}.
	 */
	REGION,
	/**
	 * Drag-n-drop completed on {@code null} <i>(nothing)</i>.
	 */
	EXTERNAL
}