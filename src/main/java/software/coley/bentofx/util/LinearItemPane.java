package software.coley.bentofx.util;

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
import javafx.scene.layout.Pane;

/**
 * A basic pane that lays out children in a single line.
 * Children that go beyond the bounds of this pane are made invisible/unmanaged.
 */
@SuppressWarnings("DuplicatedCode")
public class LinearItemPane extends Pane {
	private static final int MIN_PERPENDICULAR = 16;
	private final Orientation orientation;
	private final BooleanProperty overflowing = new SimpleBooleanProperty();
	private final ObjectProperty<Node> keepInView = new SimpleObjectProperty<>();

	/**
	 * @param orientation
	 * 		Which axis to layout children on.
	 */
	public LinearItemPane(@Nonnull Orientation orientation) {
		this.orientation = orientation;

		// When the child to keep in view changes, update the layout.
		keepInView.addListener((ob, old, cur) -> requestLayout());
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

	@Override
	protected void layoutChildren() {
		if (orientation == Orientation.HORIZONTAL) {
			layoutHorizontal();
		} else {
			layoutVertical();
		}
	}

	private void layoutHorizontal() {
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
			Bounds childBounds = child.getBoundsInParent();
			double childWidth = childBounds.getWidth();
			double childHeight = Math.max(childBounds.getHeight(), MIN_PERPENDICULAR);
			double toFillWidth = maxX - x;
			boolean visible = x + childWidth >= 0 && x < maxX;

			// We can optimize a bit by making children that can't be shown not visible and handle layout requests.
			child.setManaged(visible);
			child.setVisible(visible);
			if (visible) {
				// The only bounds we need to modify is the width.
				// By adding +1 this will bump the size until the child is able to show all of its content.
				// At that point, adding +1 will not result in any further changes.
				layoutInArea(child, x, y, toFillWidth, childHeight,
						0, Insets.EMPTY, true, true,
						HPos.LEFT, VPos.TOP);
			} else {
				overflow = true;
			}

			x += (int) childWidth;
		}
		overflowing.set(overflow);
	}

	private void layoutVertical() {
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
			Bounds childBounds = child.getBoundsInParent();
			double childWidth = Math.max(childBounds.getWidth(), MIN_PERPENDICULAR);
			double childHeight = childBounds.getHeight();
			double toFillHeight = maxY - y;
			boolean visible = y + childHeight >= 0 && y < maxY;

			// We can optimize a bit by making children that can't be shown not visible and handle layout requests.
			child.setManaged(visible);
			child.setVisible(visible);
			if (visible) {
				// The only bounds we need to modify is the height.
				// By adding +1 this will bump the size until the child is able to show all of its content.
				// At that point, adding +1 will not result in any further changes.
				layoutInArea(child, x, y, childWidth, toFillHeight,
						0, Insets.EMPTY, true, true,
						HPos.LEFT, VPos.TOP);
			} else {
				overflow = true;
			}

			y += (int) childHeight;
		}
		overflowing.set(overflow);
	}
}
