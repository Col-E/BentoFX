package software.coley.bentofx.util;

import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.stage.Stage;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.Bento;
import software.coley.bentofx.control.Header;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.path.DockablePath;

import java.util.Map;
import java.util.function.Function;

/**
 * Drag-n-drop based utilities.
 *
 * @author Matt Coley
 */
public class DragUtils {
	public static final String PREFIX = "dnd-bento;";

	/**
	 * Unique identifier for the BentoFX dockable MIME type.
	 */
	private static final String DOCKABLE_MIME_IDENTIFIER =
			"application/x-bentofx-dockable";

	/**
	 * A custom {@link DataFormat} to prevent dropping a dragged {@link Header}
	 * onto another application from pasting its content into it.
	 */
	private static final DataFormat DOCKABLE_FORMAT =
			getDataFormat(DOCKABLE_MIME_IDENTIFIER);

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
	public static Map<DataFormat, Object> content(Dockable dockable) {
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
	public static Map<DataFormat, Object> content(Dockable dockable, @Nullable DragDropTarget target) {
		ClipboardContent content = new ClipboardContent();
		String format = PREFIX + dockable.getDragGroupMask() + ";" + dockable.getIdentifier();
		if (target != null)
			format += ";" + target.name();
		content.put(DOCKABLE_FORMAT, format);
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
	public static void completeDnd(DragEvent event, Dockable dockable, DragDropTarget target) {
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
	public static String extractIdentifier(Dragboard dragboard) {
		String[] parts = splitContent(dragboard::getContent);
		if (parts == null || parts.length < 3)
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
	public static Integer extractDragGroup(Dragboard dragboard) {
		String[] parts = splitContent(dragboard::getContent);
		if (parts == null || parts.length < 2)
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
	public static DragDropTarget extractDropTargetType(Dragboard dragboard) {
		String[] parts = splitContent(dragboard::getContent);
		if (parts == null || parts.length < 4)
			return null;
		try {
			return DragDropTarget.valueOf(parts[3]);
		} catch (Exception ex) {
			// Not a recognized target type.
			return null;
		}
	}

	/**
	 * Checks if a drag carries {@link Header}, without depending on how its
	 * content is encoded.
	 * <p>
	 * Prefer this over inspecting the {@link Dragboard}'s content directly,
	 * which may change between releases.
	 *
	 * @param dragboard
	 * 		A {@link Dragboard} that may contain a {@link Header}.
	 *
	 * @return {@code true} if the board's {@link DragEvent} originates
	 * from a dragged {@link Header}.
	 *
	 * @see #content(Dockable)
	 */
	public static boolean isDockableDrag(Dragboard dragboard) {
		return isDockableDrag(dragboard::getContent);
	}

	/**
	 * {@return {@code true} if the content carries a dragged {@link Header}}
	 *
	 * @param content
	 * 		A {@link Dragboard}'s content.
	 */
	static boolean isDockableDrag(Function<DataFormat, @Nullable Object> content) {
		return splitContent(content) != null;
	}

	/**
	 * @param content
	 * 		A {@link Dragboard}'s content, by format (e.g. {@link Dragboard#getContent(DataFormat)}).
	 *
	 * @return The {@code ;} separated parts of the dragged {@link Header}'s content, or {@code null} if there is none.
	 */
	static String @Nullable [] splitContent(
			@Nullable Function<DataFormat, @Nullable Object> content
	) {
		if (content == null) {
			return null;
		}

		Object dockableContent = content.apply(DOCKABLE_FORMAT);

		return dockableContent instanceof String string ? string.split(";") : null;
	}

	/**
	 * Looks up a {@link DataFormat} before creating one.
	 * <p>
	 * JavaFX keeps one process-wide registry of formats and refuses a second
	 * registration of the same mime type, which could otherwise fail a class's
	 * initialization under class reloading or multiple class loaders.
	 *
	 * @param mimeType
	 * 		The unique identifier for a MIME type.
	 *
	 * @return The {@link DataFormat} for the provided {@code mimeType}.
	 */
	private static DataFormat getDataFormat(final @Nullable String mimeType) {
		DataFormat existing = DataFormat.lookupMimeType(mimeType);
		return existing != null ? existing : new DataFormat(mimeType);
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
	public static Header getHeader(Bento bento, DragEvent event) {
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
