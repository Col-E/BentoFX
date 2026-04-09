package software.coley.bentofx.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.Selector;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import software.coley.bentofx.control.HeaderPane;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Various utilities for bento internals.
 *
 * @author Matt Coley
 */
public class BentoUtils {
	/**
	 * @param side
	 * 		Some side.
	 *
	 * @return Respective orientation if it were to be used for a {@link HeaderPane}.
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
	@SuppressWarnings("unchecked")
	public static <T> List<T> getCastChildren(@Nonnull Parent parent, @Nonnull Class<T> nodeType) {
		return (List<T>) getChildren(parent, nodeType);
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

	/**
	 * Schedules some action to be run later when a {@link Node} is attached to a {@link Scene}.
	 *
	 * @param node
	 * 		Node to operate on.
	 * @param action
	 * 		Action to run on the node when it is attached to a scene.
	 * @param <T>
	 * 		Node type.
	 */
	public static <T extends Node> void scheduleWhenShown(@Nonnull T node, @Nonnull Consumer<T> action) {
		// Already showing, do the action immediately.
		Scene scene = node.getScene();
		if (scene != null) {
			action.accept(node);
			return;
		}

		// Schedule again when the node is attached to a scene.
		node.sceneProperty().addListener(new ChangeListener<>() {
			@Override
			public void changed(ObservableValue<? extends Scene> observable, Scene oldScene, Scene newScene) {
				if (newScene != null) {
					node.sceneProperty().removeListener(this);

					// We schedule the action rather than handling things immediately.
					// The layout pass still needs to run, and if we operated now some properties like dimensions
					// would not be up-to-date with the expectations of when the target node is "showing".
					Platform.runLater(() -> {
						action.accept(node);

						//In the case that these actions were queued immediately after the final layout pass,
						//requestLayout to execute the queued action so that they aren't
						//idling until another layout pass occurs.
						//Another layout pass being the user moving a divider, collapsing a header, etc.
						if(node instanceof Parent parent){
							Runnable postListener = new Runnable() {
								@Override
								public void run() {
									newScene.removePostLayoutPulseListener(this);
									parent.requestLayout();
								}
							};
							newScene.addPostLayoutPulseListener(postListener);
						}

					});
				}
			}
		});
	}
}
