package software.coley.bentofx.util;

import jakarta.annotation.Nonnull;
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
	private final Orientation orientation;

	/**
	 * @param orientation
	 * 		Which axis to layout children on.
	 */
	public LinearItemPane(@Nonnull Orientation orientation) {
		this.orientation = orientation;
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
		for (Node child : getChildren()) {
			Bounds childBounds = child.getBoundsInParent();
			double childWidth = childBounds.getWidth();
			double childHeight = Math.max(childBounds.getHeight(), 16);
			double toFillWidth = maxX - x;
			boolean visible = x < maxX;

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
				x += (int) childWidth;
			}
		}
	}

	private void layoutVertical() {
		final int maxY = (int) getHeight();
		final int x = 0;
		int y = 0;
		for (Node child : getChildren()) {
			Bounds childBounds = child.getBoundsInParent();
			double childWidth = Math.max(childBounds.getWidth(), 16);
			double childHeight = childBounds.getHeight();
			double toFillHeight = maxY - y;
			boolean visible = y < maxY;

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
				y += (int) childHeight;
			}
		}
	}
}
