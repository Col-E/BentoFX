package software.coley.bentofx.control;

import jakarta.annotation.Nonnull;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * A basic pane that lays out children in a single line.
 * Children that go beyond the bounds of this pane are made invisible/unmanaged.
 *
 * @author Matt Coley
 */
@SuppressWarnings("DuplicatedCode")
public class LinearItemPane extends Pane {
	protected static final int MIN_PERPENDICULAR = 16;
	private final Orientation orientation;
	private final BooleanProperty overflowing = new SimpleBooleanProperty();
	private final BooleanProperty fitChildrenToPerpendicular = new SimpleBooleanProperty();
	private final ObjectProperty<Node> keepInView = new SimpleObjectProperty<>();

	/**
	 * @param orientation
	 * 		Which axis to layout children on.
	 */
	public LinearItemPane(@Nonnull Orientation orientation) {
		this.orientation = orientation;

		// When the child to keep in view changes, update the layout.
		keepInView.addListener((ob, old, cur) -> requestLayout());

		// Same for perpendicular fitting.
		fitChildrenToPerpendicular.addListener((ob, old, cur) -> requestLayout());
	}

	/**
	 * @return Orientation of this linear pane.
	 */
	@Nonnull
	public Orientation getOrientation() {
		return orientation;
	}

	/**
	 * @return {@code true} when children overflow beyond the visible bounds of this pane.
	 * {@code false} when all children are visible in-bounds.
	 */
	@Nonnull
	public BooleanProperty overflowingProperty() {
		return overflowing;
	}

	/**
	 * @return A child to keep in view.
	 */
	@Nonnull
	public ObjectProperty<Node> keepInViewProperty() {
		return keepInView;
	}

	/**
	 * Similar to {@link HBox#setFillHeight(boolean)} and {@link VBox#setFillWidth(boolean)}.
	 * Requires any child node added to this pane to {@link Node#isResizable() support resizing}.
	 *
	 * @return {@code true} to fit child widths/height to the dimensions of this pane on the perpendicular axis.
	 */
	@Nonnull
	public BooleanProperty fitChildrenToPerpendicularProperty() {
		return fitChildrenToPerpendicular;
	}

	/**
	 * Convenience call for {@code getChildren().add(node)}
	 *
	 * @param node
	 * 		Child to add.
	 */
	public void add(@Nonnull Node node) {
		getChildren().add(node);
	}

	@Override
	protected void layoutChildren() {
		if (orientation == Orientation.HORIZONTAL) {
			layoutHorizontal();
		} else {
			layoutVertical();
		}
	}

	protected void layoutHorizontal() {
		final int maxX = (int) getWidth();
		final int y = 0;
		int x = 0;

		// Offset initial X value to keep the target child in the view.
		Node viewTarget = keepInView.get();
		if (viewTarget != null) {
			double offset = 0;
			for (Node child : getChildren()) {
				Bounds childBounds = child.getBoundsInParent();
				double childWidth = childBounds.getWidth();
				offset += childWidth;
				if (child == viewTarget) {
					if (offset > maxX)
						x = (int) (maxX - offset);
					break;
				}
			}
		}

		// Layout all children.
		boolean overflow = false;
		for (Node child : getChildren()) {
			// Do layout on child to ensure the bounds lookup we do next is up-to-date.
			if (child instanceof Parent childParent)
				childParent.layout();
			Bounds childBounds = child.getBoundsInParent();
			double childWidth = childBounds.getWidth();
			double childHeight = computeChildPerpendicularSize(childBounds, Orientation.HORIZONTAL);
			boolean visible = x + childWidth >= 0 && x < maxX;

			if (!child.visibleProperty().isBound()) {
				// We can optimize a bit by making children that can't be shown not visible and handle layout requests.
				child.setManaged(visible);
				child.setVisible(visible);
			}
			if (visible) {
				// The only bounds we need to modify is the width.
				// By adding +1 this will bump the size until the child is able to show all of its content.
				// At that point, adding +1 will not result in any further changes.
				layoutInArea(child, x, y, childWidth, childHeight,
						0, Insets.EMPTY, false, true,
						HPos.LEFT, VPos.TOP);
			} else {
				overflow = true;
			}

			x += (int) childWidth;
		}
		overflowing.set(overflow);
	}

	protected void layoutVertical() {
		final int maxY = (int) getHeight();
		final int x = 0;
		int y = 0;

		// Offset initial Y value to keep the target child in the view.
		Node viewTarget = keepInView.get();
		if (viewTarget != null) {
			double offset = 0;
			for (Node child : getChildren()) {
				Bounds childBounds = child.getBoundsInParent();
				double childHeight = childBounds.getHeight();
				offset += childHeight;
				if (child == viewTarget) {
					if (offset > maxY)
						y = (int) (maxY - offset);
					break;
				}
			}
		}

		// Layout all children.
		boolean overflow = false;
		for (Node child : getChildren()) {
			// Do layout on child to ensure the bounds lookup we do next is up-to-date.
			if (child instanceof Parent childParent)
				childParent.layout();
			Bounds childBounds = child.getBoundsInParent();
			double childWidth = computeChildPerpendicularSize(childBounds, Orientation.VERTICAL);
			double childHeight = childBounds.getHeight();
			boolean visible = y + childHeight >= 0 && y < maxY;

			// We can optimize a bit by making children that can't be shown not visible and handle layout requests.
			if (!child.visibleProperty().isBound()) {
				child.setManaged(visible);
				child.setVisible(visible);
			}
			if (visible) {
				// The only bounds we need to modify is the height.
				// By adding +1 this will bump the size until the child is able to show all of its content.
				// At that point, adding +1 will not result in any further changes.
				layoutInArea(child, x, y, childWidth, childHeight,
						0, Insets.EMPTY, true, false,
						HPos.LEFT, VPos.TOP);
			} else {
				overflow = true;
			}

			y += (int) childHeight;
		}
		overflowing.set(overflow);
	}

	protected double computeChildPerpendicularSize(@Nonnull Bounds childBounds, @Nonnull Orientation orientation) {
		if (orientation == Orientation.HORIZONTAL) {
			return Math.max(fitChildrenToPerpendicular.get() ? getHeight() : childBounds.getHeight(), MIN_PERPENDICULAR);
		} else {
			return Math.max(fitChildrenToPerpendicular.get() ? getWidth() : childBounds.getWidth(), MIN_PERPENDICULAR);
		}
	}
}
