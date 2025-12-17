package software.coley.bentofx.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.stage.Stage;
import software.coley.bentofx.Bento;
import software.coley.bentofx.control.Header;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.path.DockablePath;

import java.util.Map;

/**
 * Drag-n-drop based utilities.
 *
 * @author Matt Coley
 */
public class DragUtils {
	public static final String PREFIX = "dnd-bento;";

	/**
	 * Creates a map containing details about the given {@link Dockable} that can be retrieved later.
	 *
	 * @param dockable
	 * 		Dockable content being dragged.
	 *
	 * @return Content to put into {@link Dragboard#setContent(Map)}.
	 *
	 * @see #extractIdentifier(Dragboard)
	 * @see #extractDragGroup(Dragboard)
	 */
	@Nonnull
	public static Map<DataFormat, Object> content(@Nonnull Dockable dockable) {
		return content(dockable, null);
	}

	/**
	 * Creates a map containing details about the given {@link Dockable} that can be retrieved later.
	 *
	 * @param dockable
	 * 		Dockable content being dragged.
	 * @param target
	 * 		The completed drag-drop type for a completed operation. Otherwise {@code null} for incomplete operations.
	 *
	 * @return Content to put into {@link Dragboard#setContent(Map)}.
	 *
	 * @see #extractIdentifier(Dragboard)
	 * @see #extractDragGroup(Dragboard)
	 * @see #extractDropTargetType(Dragboard)
	 */
	@Nonnull
	public static Map<DataFormat, Object> content(@Nonnull Dockable dockable, @Nullable DragDropTarget target) {
		ClipboardContent content = new ClipboardContent();
		String format = PREFIX + dockable.getDragGroupMask() + ";" + dockable.getIdentifier();
		if (target != null)
			format += ";" + target.name();
		content.putString(format);
		return content;
	}

	/**
	 * Updates the event to model the completed drag-n-drop of a {@link Header}.
	 *
	 * @param event
	 * 		Event to update {@link Dragboard} content of.
	 * @param dockable
	 * 		Dockable content being dragged.
	 * @param target
	 * 		The completed drag-drop type for a completed operation
	 */
	public static void completeDnd(@Nonnull DragEvent event, @Nonnull Dockable dockable, @Nonnull DragDropTarget target) {
		event.getDragboard().setContent(content(dockable, target));
		event.consume();
	}

	/**
	 * @param dragboard
	 * 		Some dragboard that may contain a dragged {@link Header}.
	 *
	 * @return The {@link Dockable#getIdentifier()} of the dragged {@link Header}
	 * if the board's respective {@link DragEvent} originates from a dragged {@link Header}.
	 *
	 * @see #content(Dockable)
	 */
	@Nullable
	public static String extractIdentifier(@Nonnull Dragboard dragboard) {
		if (!dragboard.hasString())
			return null;
		String[] parts = dragboard.getString().split(";");
		if (parts.length < 3)
			return null;
		return parts[2];
	}

	/**
	 * @param dragboard
	 * 		Some dragboard that may contain a dragged {@link Header}.
	 *
	 * @return The {@link Dockable#getDragGroupMask()} of the dragged {@link Header}
	 * if the board's respective {@link DragEvent} originates from a dragged {@link Header}.
	 *
	 * @see #content(Dockable)
	 */
	@Nullable
	public static Integer extractDragGroup(@Nonnull Dragboard dragboard) {
		if (!dragboard.hasString())
			return null;
		String[] parts = dragboard.getString().split(";");
		if (parts.length < 2)
			return null;
		try {
			return Integer.parseInt(parts[1]);
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * @param dragboard
	 * 		Some dragboard that may contain a dragged {@link Header}.
	 *
	 * @return The {@link DragDropTarget} of the dragged {@link Header}
	 * if the board's respective {@link DragEvent} originates from a dragged {@link Header} that has been completed.
	 *
	 * @see #content(Dockable, DragDropTarget)
	 */
	@Nullable
	public static DragDropTarget extractDropTargetType(@Nonnull Dragboard dragboard) {
		if (!dragboard.hasString())
			return null;
		String[] parts = dragboard.getString().split(";");
		if (parts.length < 4)
			return null;
		try {
			return DragDropTarget.valueOf(parts[3]);
		} catch (Exception ex) {
			// Not a recognized target type.
			return null;
		}
	}

	/**
	 * This goofy method exists because {@link DragEvent#getGestureSource()} is {@code null} when anything
	 * is dragged between two separate {@link Stage}s. When that occurs we need some way to recover the {@link Header}.
	 *
	 * @param bento
	 * 		Bento instance to search in.
	 * @param event
	 * 		Drag event to extract the {@link Header}'s associated {@link Dockable#getIdentifier()}.
	 *
	 * @return The {@link Header} that initiated this drag gesture.
	 */
	@Nullable
	public static Header getHeader(@Nonnull Bento bento, @Nonnull DragEvent event) {
		// Ideally the header is just known to the event.
		Object source = event.getGestureSource();
		if (source instanceof Header headerSource)
			return headerSource;

		// If the source is NOT null and NOT a header, we're in an unexpected state.
		if (source != null)
			return null;

		// The source being 'null' happens when drag-n-drop happens across stages.
		// In this case, we search for the header based on the event contents.
		DockablePath path = bento.search().dockable(event);
		if (path == null)
			return null;
		return path.leafContainer().getHeader(path.dockable());
	}
}
