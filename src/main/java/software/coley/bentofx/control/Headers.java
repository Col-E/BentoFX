package software.coley.bentofx.control;

import jakarta.annotation.Nonnull;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.util.DragDropTarget;
import software.coley.bentofx.util.DragUtils;

import static software.coley.bentofx.util.BentoStates.*;

/**
 * Linear item pane to hold {@link Header} displays of {@link DockContainerLeaf#getDockables()}.
 *
 * @author Matt Coley
 */
public class Headers extends LinearItemPane {
	/**
	 * @param container
	 * 		Parent container.
	 * @param orientation
	 * 		Which axis to layout children on.
	 * @param side
	 * 		Side in the parent container where tabs are displayed.
	 */
	public Headers(@Nonnull DockContainerLeaf container, @Nonnull Orientation orientation, @Nonnull Side side) {
		super(orientation);

		// Create side-specific header region class.
		getStyleClass().add("header-region");
		switch (side) {
			case TOP -> pseudoClassStateChanged(PSEUDO_SIDE_TOP, true);
			case BOTTOM -> pseudoClassStateChanged(PSEUDO_SIDE_BOTTOM, true);
			case LEFT -> pseudoClassStateChanged(PSEUDO_SIDE_LEFT, true);
			case RIGHT -> pseudoClassStateChanged(PSEUDO_SIDE_RIGHT, true);
		}

		// Make this pane fill the full width/height (matching orientation) of the parent container.
		if (orientation == Orientation.HORIZONTAL) {
			prefWidthProperty().bind(container.widthProperty());
		} else {
			prefHeightProperty().bind(container.heightProperty());
		}

		// Keep the minimum size with the last added header item.
		// This will ensure this pane doesn't resize to 0 width/height when the last child is removed,
		// allowing the user to later drag another header back into this space.
		setupMinSizeTracking();

		// Make children fill the full width/height of this pane on the perpendicular (to orientation) axis.
		fitChildrenToPerpendicularProperty().set(true);

		// Keep the selected dockable in view.
		keepInViewProperty().bind(container.selectedDockableProperty().map(container::getHeader));

		// Support drag-drop.
		setupDragDrop(container);
	}

	protected void setupMinSizeTracking() {
		getChildren().addListener((ListChangeListener<Node>) c -> {
			Orientation orientation = getOrientation();

			double min = MIN_PERPENDICULAR;
			while (c.next()) {
				for (Node child : c.getAddedSubList()) {
					if (child instanceof Region r) {
						if (orientation == Orientation.HORIZONTAL) {
							min = Math.max(min, r.getHeight());
						} else {
							min = Math.max(min, r.getWidth());
						}
					}
				}
				for (Node child : c.getRemoved()) {
					if (child instanceof Region r) {
						if (orientation == Orientation.HORIZONTAL) {
							min = Math.max(min, r.getHeight());
						} else {
							min = Math.max(min, r.getWidth());
						}
					}
				}
			}

			if (orientation == Orientation.HORIZONTAL) {
				minHeightProperty().setValue(min);
			} else {
				minWidthProperty().setValue(min);
			}
		});
	}

	protected void setupClip() {
		// Use a clip to prevent headers from rendering beyond expected bounds.
		// With the example CSS in use this is not needed, but some users who make their own may need this.
		Rectangle clip = new Rectangle();
		clip.widthProperty().bind(widthProperty());
		clip.heightProperty().bind(heightProperty());
		setClip(clip);
	}

	protected void setupDragDrop(@Nonnull DockContainerLeaf container) {
		setOnDragOver(e -> {
			Dragboard dragboard = e.getDragboard();
			String dockableIdentifier = DragUtils.extractIdentifier(dragboard);
			if (dockableIdentifier != null) {
				DockablePath dragSourcePath = container.getBento().search().dockable(dockableIdentifier);
				if (dragSourcePath != null) {
					Dockable dragSourceDockable = dragSourcePath.dockable();
					if (container.canReceiveDockable(dragSourceDockable, null)) {
						container.drawCanvasHint(this);
					} else {
						container.clearCanvas();
					}
				}

				// We always need to accept content if there is a dockable identifier.
				// In the case where it is not actually receivable, we'll handle that in the completion logic.
				e.acceptTransferModes(TransferMode.MOVE);
			}

			// Do not propagate upwards.
			e.consume();
		});
		setOnDragDropped(e -> {
			// Skip if dragboard doesn't contain a dockable identifier.
			Dragboard dragboard = e.getDragboard();
			String dockableIdentifier = DragUtils.extractIdentifier(dragboard);
			if (dockableIdentifier == null)
				return;

			// Skip if the dockable cannot be found in our bento instance.
			DockablePath dragSourcePath = container.getBento().search().dockable(dockableIdentifier);
			if (dragSourcePath == null)
				return;

			// If our container can receive the header, move it over.
			DockContainerLeaf sourceContainer = dragSourcePath.leafContainer();
			Dockable sourceDockable = dragSourcePath.dockable();
			if (container.canReceiveDockable(sourceDockable, null)) {
				sourceContainer.removeDockable(sourceDockable);
				container.addDockable(sourceDockable);
				container.selectDockable(sourceDockable);
				DragUtils.completeDnd(e, sourceDockable, DragDropTarget.REGION);
			}
		});
		setOnDragExited(e -> container.clearCanvas());
	}
}
