package software.coley.bentofx.util;

import software.coley.bentofx.dockable.DockableDestination;
import software.coley.bentofx.header.Header;

/**
 * Type of drag-n-drop target for the completion of a {@link Header}'s drag operation.
 *
 * @author Matt Coley
 */
public enum DropTargetType {
	/**
	 * Drag-n-drop completed on a {@link Header}.
	 */
	HEADER,
	/**
	 * Drag-n-drop completed on a {@link DockableDestination}.
	 */
	REGION,
	/**
	 * Drag-n-drop completed on {@code null} <i>(nothing)</i>.
	 */
	EXTERNAL
}
