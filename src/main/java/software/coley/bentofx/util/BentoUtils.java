package software.coley.bentofx.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.css.Selector;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Dockable;
import software.coley.bentofx.DockableDestination;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.header.HeaderView;
import software.coley.bentofx.impl.content.ImplTabbedContent;
import software.coley.bentofx.path.DockablePath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Various utilities for bento internals.
 *
 * @author Matt Coley
 */
public class BentoUtils {
	public static final String PREFIX = "dnd-bento:";
	public static final Supplier<String> ID_PROVIDER = () -> UUID.randomUUID().toString();

	/**
	 * @return New random identifier string.
	 */
	@Nonnull
	public static String newIdentifier() {
		return ID_PROVIDER.get();
	}

	/**
	 * This goofy method exists because {@link DragEvent#getGestureSource()} is {@code null} when content
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
		DockablePath path = bento.findDockable(event);
		if (path == null)
			return null;
		if (path.content() instanceof ImplTabbedContent tabbedContent)
			return tabbedContent.getHeader(path.dockable());
		return null;
	}

	/**
	 * Find all children with the given CSS selector in the given parent.
	 * <p/>
	 * The search does not continue for children that match the selector. For instance if you had five
	 * panes embedded in a row all with the same selector, only the top-most pane would be yielded here.
	 *
	 * @param parent
	 * 		Parent to search in.
	 * @param cssSelector
	 * 		CSS selector of children to find.
	 *
	 * @return All matching children of any level with the given CSS selector.
	 */
	@Nonnull
	public static List<Node> getChildren(@Nonnull Parent parent, @Nonnull String cssSelector) {
		Selector selector = Selector.createSelector(cssSelector);
		List<Node> list = new ArrayList<>();
		visitAndMatchChildren(parent, selector, list);
		return list;
	}

	/**
	 * Find all children with the given type in the given parent.
	 * <p/>
	 * The search does not continue for children that match the type. For instance if you had five
	 * {@link BorderPane} embedded in a row all, only the top-most {@link BorderPane} would be yielded here.
	 *
	 * @param parent
	 * 		Parent to search in.
	 * @param nodeType
	 * 		Type of children to find.
	 *
	 * @return All matching children of any level with the given type.
	 */
	@Nonnull
	public static List<Node> getChildren(@Nonnull Parent parent, @Nonnull Class<?> nodeType) {
		List<Node> list = new ArrayList<>();
		visitAndMatchChildren(parent, nodeType, list);
		return list;
	}

	private static void visitAndMatchChildren(@Nonnull Parent parent,
	                                          @Nonnull Selector selector,
	                                          @Nonnull List<Node> list) {
		for (Node node : parent.getChildrenUnmodifiable()) {
			if (selector.applies(node)) {
				list.add(node);
			} else if (node instanceof Parent childParent) {
				visitAndMatchChildren(childParent, selector, list);
			}
		}
	}

	private static void visitAndMatchChildren(@Nonnull Parent parent,
	                                          @Nonnull Class<?> nodeType,
	                                          @Nonnull List<Node> list) {
		for (Node node : parent.getChildrenUnmodifiable()) {
			if (nodeType.isAssignableFrom(node.getClass())) {
				list.add(node);
			} else if (node instanceof Parent childParent) {
				visitAndMatchChildren(childParent, nodeType, list);
			}
		}
	}

	@Nullable
	public static <T> T getParent(@Nonnull Node node, @Nonnull Class<T> type) {
		// TODO: The level of expected parent being fetched here vs the value we pass in various usages is probably "wrong".
		//  - It seemingly works at the moment, but should be re-evaluated
		return getOrParent(node.getParent(), type);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T getOrParent(@Nullable Node node, @Nonnull Class<T> type) {
		if (node == null)
			return null;
		Parent parent = node.getParent();
		while (parent != null && !(type.isAssignableFrom(parent.getClass())))
			parent = parent.getParent();
		if (parent == null)
			return null;
		return (T) parent;
	}

	/**
	 * @param target
	 * 		Some target to base calculations in.
	 * @param x
	 * 		Target x.
	 * @param y
	 * 		Target y.
	 *
	 * @return The closest side for the given target position in the given region.
	 */
	@Nullable
	public static Side computeClosestSide(@Nonnull Region target, double x, double y) {
		double w = target.getWidth();
		double h = target.getHeight();
		double mw = w / 2;
		double mh = h / 2;

		Point2D top = new Point2D(mw, 0);
		Point2D bottom = new Point2D(mw, h);
		Point2D left = new Point2D(0, mh);
		Point2D right = new Point2D(w, mh);
		Point2D center = new Point2D(mw, mh);
		Point2D[] candidates = new Point2D[]{center, top, bottom, left, right};
		Side[] sides = new Side[]{null, Side.TOP, Side.BOTTOM, Side.LEFT, Side.RIGHT};
		int closest = 0;
		double closestDistance = Double.MAX_VALUE;
		for (int i = 0; i < candidates.length; i++) {
			Point2D candidate = candidates[i];
			double distance = candidate.distance(x, y);
			if (distance < closestDistance) {
				closest = i;
				closestDistance = distance;
			}
		}

		return sides[closest];
	}

	/**
	 * @param side
	 * 		Some side.
	 *
	 * @return Respective orientation if it were to be used for a {@link HeaderView}.
	 */
	@Nonnull
	public static Orientation sideToOrientation(@Nullable Side side) {
		return switch (side) {
			case TOP, BOTTOM -> Orientation.HORIZONTAL;
			case LEFT, RIGHT -> Orientation.VERTICAL;
			case null -> Orientation.HORIZONTAL;
		};
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
		String[] parts = dragboard.getString().split(":");
		if (parts.length < 3)
			return null;
		return parts[2];
	}

	/**
	 * @param dragboard
	 * 		Some dragboard that may contain a dragged {@link Header}.
	 *
	 * @return The {@link Dockable#getDragGroup()} of the dragged {@link Header}
	 * if the board's respective {@link DragEvent} originates from a dragged {@link Header}.
	 *
	 * @see #content(Dockable)
	 */
	@Nullable
	public static Integer extractDragGroup(@Nonnull Dragboard dragboard) {
		if (!dragboard.hasString())
			return null;
		String[] parts = dragboard.getString().split(":");
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
	 * @return The {@link DropTargetType} of the dragged {@link Header}
	 * if the board's respective {@link DragEvent} originates from a dragged {@link Header} that has been completed.
	 *
	 * @see #content(Dockable, DropTargetType)
	 */
	@Nullable
	public static DropTargetType extractDropTargetType(@Nonnull Dragboard dragboard) {
		if (!dragboard.hasString())
			return null;
		String[] parts = dragboard.getString().split(":");
		if (parts.length < 4)
			return null;
		try {
			return DropTargetType.valueOf(parts[3]);
		} catch (Exception ex) {
			// Not a recognized target type.
			return null;
		}
	}

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
	public static Map<DataFormat, Object> content(@Nonnull Dockable dockable, @Nullable DropTargetType target) {
		ClipboardContent content = new ClipboardContent();
		String format = PREFIX + dockable.getDragGroup() + ":" + dockable.getIdentifier();
		if (target != null)
			format += ":" + target.name();
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
	public static void completeDnd(@Nonnull DragEvent event, @Nonnull Dockable dockable, @Nonnull DropTargetType target) {
		event.getDragboard().setContent(content(dockable, target));
		event.consume();
	}

	/**
	 * Set up basic drop support in the given {@link DockableDestination}.
	 *
	 * @param destination
	 * 		Destination to set up drag support within.
	 * @param withSideSupport
	 *        {@code true} to allow splitting within the given destination.
	 */
	public static void setupCommonDragSupport(@Nonnull DockableDestination destination, boolean withSideSupport) {
		Region region = destination.getBackingRegion();

		// Any content wrapper can be the target of drag-n-drop.
		// Dropped items will be added to this view at the of the header region.
		region.setOnDragOver(e -> {
			Dragboard dragboard = e.getDragboard();
			String dockableIdentifier = extractIdentifier(dragboard);
			if (dockableIdentifier != null) {
				Integer dragGroup = extractDragGroup(dragboard);
				if (dragGroup != null && destination.canReceiveDragGroup(dragGroup)) {
					Side side = (withSideSupport && destination.canSplit()) ? computeClosestSide(region, e.getX(), e.getY()) : null;
					destination.drawCanvasHint(region, side);
				} else {
					destination.clearCanvas();
				}

				// We always need to accept content if there is a dockable identifier.
				// In the case where it is not actually receivable, we'll handle that in the completion logic.
				e.acceptTransferModes(TransferMode.MOVE);
			}

			// Do not propagate upwards.
			e.consume();
		});
		region.setOnDragExited(e -> {
			destination.clearCanvas();
			region.setEffect(null);
		});
	}
}
