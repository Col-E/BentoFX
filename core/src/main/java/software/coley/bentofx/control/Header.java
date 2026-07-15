package software.coley.bentofx.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.AccessibleRole;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.Bento;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.util.BentoUtils;
import software.coley.bentofx.util.DragDropTarget;
import software.coley.bentofx.util.DragUtils;

import java.util.List;

import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.scene.input.KeyCode.*;
import static software.coley.bentofx.util.BentoStates.*;

/**
 * Visual model for a {@link Dockable}.
 *
 * @author Matt Coley
 * @see HeaderPane Parent control.
 */
public class Header extends Region {
	private final StringProperty titleProperty = new SimpleStringProperty();
	private final ObjectProperty<Node> graphicProperty = new SimpleObjectProperty<>();
	private final BooleanProperty closableProperty = new SimpleBooleanProperty();
	private final ObjectProperty<@Nullable Side> sideProperty = new SimpleObjectProperty<>();
	private final ObjectProperty<Tooltip> tooltipProperty = new SimpleObjectProperty<>();
	private final GridPane grid = new GridPane();
	private final Text label = new Text();
	private final Pane graphicWrapper = new Pane();
	private final Pane closeWrapper = new Pane();
	private final Line insertionIndicator = new Line();
	private final StackPane wrapper = new StackPane();
	private final HeaderPane parentPane;
	private final Dockable dockable;
	private @Nullable Header insertionPreviewSource;
	private @Nullable Boolean insertionAfter;
	private double insertionMidpoint = Double.NaN;

	/**
	 * @param dockable
	 * 		Dockable to wrap.
	 * @param parentPane
	 * 		Parent header pane.
	 */
	public Header(Dockable dockable, HeaderPane parentPane) {
		this.parentPane = parentPane;
		this.dockable = dockable;

		getStyleClass().add("header");
		insertionIndicator.getStyleClass().add("dock-insertion-indicator");
		insertionIndicator.setVisible(false);
		insertionIndicator.setManaged(false);
		insertionIndicator.setMouseTransparent(true);

		// Setup current side/orientation state
		sideProperty.set(parentPane.getContainer().getSide());
		switch (getSide()) {
			case TOP -> pseudoClassStateChanged(PSEUDO_SIDE_TOP, true);
			case BOTTOM -> pseudoClassStateChanged(PSEUDO_SIDE_BOTTOM, true);
			case LEFT -> pseudoClassStateChanged(PSEUDO_SIDE_LEFT, true);
			case RIGHT -> pseudoClassStateChanged(PSEUDO_SIDE_RIGHT, true);
			case null -> { /* no-op when there is no side */ }
		}

		// Setup tooltip registration
		tooltipProperty.addListener((ob, old, cur) -> {
			if (old != null)
				Tooltip.uninstall(this, old);
			if (cur != null)
				Tooltip.install(this, cur);
		});

		// Facilitate [tab] + directional keys to navigate
		setAccessibleRole(AccessibleRole.TAB_ITEM);
		setFocusTraversable(true);
		setOnKeyPressed(e -> {
			Orientation orientation = BentoUtils.sideToOrientation(getSide());
			DockContainerLeaf container = parentPane.getContainer();
			KeyCode code = e.getCode();
			if ((orientation == HORIZONTAL && code == RIGHT)
					|| (orientation == VERTICAL && code == DOWN)) {
				// Go forward
				List<Dockable> dockables = container.getDockables();
				int i = dockables.indexOf(dockable);
				int nextIndex = (i + 1) % dockables.size();
				Dockable nextDockable = dockables.get(nextIndex);
				container.selectDockable(nextDockable);
				Header nextHeader = container.getHeader(nextDockable);
				if (nextHeader != null) nextHeader.requestFocus();
			} else if ((orientation == HORIZONTAL && code == LEFT)
					|| (orientation == VERTICAL && code == UP)) {
				// Go back
				List<Dockable> dockables = container.getDockables();
				int i = dockables.indexOf(dockable);
				int prev = i - 1;
				if (prev < 0)
					prev = dockables.size() - 1;
				Dockable prevDockable = dockables.get(prev);
				container.selectDockable(prevDockable);
				Header prevHeader = container.getHeader(prevDockable);
				if (prevHeader != null)
					prevHeader.requestFocus();
			} else if (code == DELETE) {
				// Close current header
				container.closeDockable(dockable);
			} else if (code == ENTER) {
				// Focus current display
				parentPane.getCenter().requestFocus();
			} else {
				// In this case if a user presses [tab] at the end of the list of headers
				// then the focus will move onto the next dockable container. We do not want
				// the event to be consumed in this circumstance, so we return immediately.
				return;
			}

			// Consume the event so there isn't double handling of traversal keys.
			e.consume();
		});

		// Bind dockable properties
		closableProperty.bind(dockable.closableProperty());
		titleProperty.bind(dockable.titleProperty());
		tooltipProperty.bind(dockable.tooltipProperty());
		graphicProperty.bind(dockable.iconFactoryProperty().map(ic -> ic.build(dockable)));
		label.textProperty().bind(titleProperty);

		// Hover support
		addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
			if (!isDisable()) pseudoClassStateChanged(PSEUDO_HOVER, true);
		});
		addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
			if (!isDisable()) pseudoClassStateChanged(PSEUDO_HOVER, false);
		});

		// Focusing a tab (via tab press) should select it.
		focusedProperty().addListener((ob, old, cur) -> {
			if (cur) parentPane.getContainer().selectDockable(dockable);
		});

		// Delegate click handling to whatever is specified by the bento behavior implementation.
		setOnMouseClicked(e -> dockable.getBento().getClickBehavior().onMouseClick(parentPane.getContainer(), dockable, this, e));

		// Layout
		Label graphicHolder = new Label();
		graphicHolder.managedProperty().bind(graphicProperty.isNotNull()); // Only take up space when there is a graphic to show
		graphicHolder.graphicProperty().bind(graphicProperty);
		graphicWrapper.getChildren().add(graphicHolder);
		sideProperty.addListener((ob, old, cur) -> recomputeLayout(cur));
		closableProperty.addListener((ob, old, cur) -> recomputeLayout(getSide()));
		grid.setHgap(6);
		grid.setVgap(6);
		grid.setPadding(new Insets(6));
		grid.setAlignment(Pos.CENTER);
		wrapper.getChildren().addAll(grid, insertionIndicator);
		wrapper.widthProperty().addListener((ob, old, cur) -> updateInsertionIndicator());
		wrapper.heightProperty().addListener((ob, old, cur) -> updateInsertionIndicator());
		getChildren().add(wrapper);
		recomputeLayout(getSide());
	}

	/**
	 * Populate drag-n-drop event handling.
	 *
	 * @return This.
	 */
	public Header withDragDrop() {
		Bento bento = dockable.getBento();

		// Closing support
		Button closeButton = new Button("✕");
		closeButton.setFocusTraversable(false);
		closeButton.getStyleClass().add("close-button");
		closeButton.setOnAction(e -> parentPane.getContainer().closeDockable(dockable));
		closeWrapper.getChildren().add(closeButton);
		setOnMouseReleased(e -> {
			// Middle release --> close dockable
			if (e.getButton() == MouseButton.MIDDLE && getBoundsInLocal().contains(e.getX(), e.getY()))
				parentPane.getContainer().closeDockable(dockable);
		});

		// Start dragging this header
		setOnDragDetected(e -> {
			if (!dockable.isCanBeDragged()) return;

			// Drag can only be initiated by primary mouse button drags.
			if (e.getButton() == MouseButton.PRIMARY) {
				e.consume();

				Image image = snapshot(null, null);
				Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
				dragboard.setContent(DragUtils.content(dockable));
				dragboard.setDragView(image);
			}
		});

		// Any header can be the target of drag-n-drop. Dropped items are inserted before or after
		// the target depending on which half of the header receives the drop.
		setOnDragOver(e -> {
			Dragboard dragboard = e.getDragboard();
			String dockableIdentifier = DragUtils.extractIdentifier(dragboard);
			if (dockableIdentifier != null) {
				// Only visually update when our header does not match the dragged one.
				// We still need to accept the other header even if it is a match so that the
				// drag-done handler doesn't try and plop the header into a new window.
				//
				// In the case where it is our own header, we'll handle that in the completion logic.
				if (!dockable.getIdentifier().equals(dockableIdentifier)) {
					DockablePath dragSourcePath = bento.search().dockable(dockableIdentifier);
					if (dragSourcePath != null) {
						// Must be able to receive the dockable in order to show a preview.
						// Either the source is the same container as this header, or the target container can receive it.
						Dockable dragSourceDockable = dragSourcePath.dockable();
						DockContainerLeaf container = parentPane.getContainer();
						if (dragSourcePath.leafContainer() == container
								|| container.canReceiveDockable(dragSourceDockable, getSide())) {
							Header dragSourceHeader = dragSourcePath.leafContainer().getHeader(dragSourceDockable);
							if (dragSourceHeader != null) {
								enableInsertionIndicator(dragSourceHeader, isDropAfter(e));
								container.clearCanvas();
							} else {
								disableInsertionIndicator();
								container.clearCanvas();
							}
						} else {
							// Cannot receive the dockable, so don't show a preview.
							disableInsertionIndicator();
							container.clearCanvas();
						}
					} else {
						// Cannot find the dockable in the bento instance, so don't show a preview.
						disableInsertionIndicator();
						parentPane.getContainer().clearCanvas();
					}
				} else {
					// Cannot show a preview for our own header, so don't show a preview.
					disableInsertionIndicator();
					parentPane.getContainer().clearCanvas();
				}

				// Accept the drag so that the drag-done handler doesn't try and plop the header into a new window.
				e.acceptTransferModes(TransferMode.MOVE);
			} else {
				// Dragboard does not contain a dockable identifier, so don't show a preview.
				disableInsertionIndicator();
				parentPane.getContainer().clearCanvas();
			}

			// Do not propagate upwards.
			e.consume();
		});
		setOnDragExited(e -> {
			// Clear the insertion indicator.
			disableInsertionIndicator();

			// Clear canvas/drawing.
			parentPane.getContainer().clearCanvas();
		});

		// Handle an item being dropped on this header.
		setOnDragDropped(e -> {
			// We must know our own parent in order to receive the incoming dockable.
			DockContainerLeaf parentContainer = dockable.getContainer();
			if (parentContainer == null)
				return;

			// Skip if dragboard doesn't contain a dockable identifier.
			Dragboard dragboard = e.getDragboard();
			String dockableIdentifier = DragUtils.extractIdentifier(dragboard);
			if (dockableIdentifier == null)
				return;

			// Skip if the dragged item represents the same dockable as our own header.
			if (dockable.getIdentifier().equals(dockableIdentifier))
				return;

			// Skip if the dockable cannot be found in our bento instance.
			DockablePath dragSourcePath = bento.search().dockable(dockableIdentifier);
			if (dragSourcePath == null)
				return;

			// Check if our container can receive the dockable.
			DockContainerLeaf sourceContainer = dragSourcePath.leafContainer();
			Dockable sourceDockable = dragSourcePath.dockable();
			boolean sameContainer = parentContainer == sourceContainer;
			if (sameContainer || parentContainer.canReceiveDockable(sourceDockable, getSide())) {
				// Move the header over to the target container and select it.
				int targetIndex = parentContainer.getDockables().indexOf(dockable);
				int sourceIndex = sourceContainer.getDockables().indexOf(sourceDockable);
				int insertionIndex = targetIndex + (isDropAfter(e) ? 1 : 0);
				if (sameContainer && insertionIndex > sourceIndex)
					insertionIndex--;

				// Remove from source, put into target at given index, and select it.
				sourceContainer.removeDockable(sourceDockable);
				parentContainer.addDockable(insertionIndex, sourceDockable);
				parentContainer.selectDockable(sourceDockable);

				// Clear the insertion indicator and canvas.
				disableInsertionIndicator();
				parentContainer.clearCanvas();

				// Finish the drag operation.
				DragUtils.completeDnd(e, sourceDockable, DragDropTarget.HEADER);
			}
		});

		// Handle drag completion. If this header was dragged onto a viable surface, the event should have
		// been updated to indicate so. However, if the header was dragged someplace not viable and no such
		// update occurred then we will pop it out into a new window.
		//
		// Q: Why is this wrapped in 'scheduleWhenShown'?
		// A: Because there is a one pulse delay before the header receives its new scene value when moving.
		// We fire off too early with this, so by operating on the scene property (for when it gets set the next pulse)
		// then everything is back to working as intended.
		setOnDragDone(e -> BentoUtils.scheduleWhenShown(this, h -> {
			// Drag source must not be a drag-drop-stage with the source header as the only item.
			// We don't want to close the window just to open a new one with the same content, that would be dumb.
			Scene scene = getScene();
			if (scene.getWindow() instanceof DragDropStage && BentoUtils.getChildren(scene.getRoot(), Header.class).size() == 1)
				return;

			// Drag completion event must not have a drop target specified.
			if (DragUtils.extractDropTargetType(e.getDragboard()) != null)
				return;

			// Handle opening in a new window when drag completes without a found target.
			DockContainerLeaf parentContainer = dockable.getContainer();
			if (parentContainer == null)
				return;
			Scene currentScene = parentContainer.getScene();
			if (e.getGestureTarget() == null
					&& dockable.isCanBeDroppedToNewWindow()
					&& parentContainer.removeDockable(dockable)) {
				// Open a stage with the new dockable as its primary content.
				Stage stage = bento.stageBuilding().newStageForDockable(currentScene, parentContainer, dockable);
				stage.show();
				stage.toFront();
				stage.requestFocus();
				DragUtils.completeDnd(e, dockable, DragDropTarget.EXTERNAL);
			}
		}));

		return this;
	}

	/**
	 * Recompute the layout of the header based on the given side/orientation.
	 *
	 * @param side
	 * 		Side/orientation to use for layout.
	 */
	private void recomputeLayout(@Nullable Side side) {
		grid.getChildren().clear();
		switch (side) {
			case TOP, BOTTOM -> {
				label.setRotate(0);
				grid.add(graphicWrapper, 0, 0);
				grid.add(label, 1, 0);
				if (dockable.closableProperty().get()) grid.add(closeWrapper, 2, 0);
			}
			case LEFT -> {
				label.setRotate(-90);
				grid.add(new Group(label), 0, 0);
				grid.add(graphicWrapper, 0, 1);
				if (dockable.closableProperty().get()) grid.add(closeWrapper, 0, 2);
			}
			case RIGHT -> {
				label.setRotate(90);
				grid.add(graphicWrapper, 0, 0);
				grid.add(new Group(label), 0, 1);
				if (dockable.closableProperty().get()) grid.add(closeWrapper, 0, 2);
			}
			case null -> {
				// When there is no side, the grid does not get updated
			}
		}
		requestLayout();
	}

	/**
	 * Shows a thin insertion indicator before or after this header.
	 *
	 * @param header
	 * 		Some other header being dragged.
	 */
	private void enableInsertionIndicator(Header header, boolean after) {
		boolean sourceChanged = insertionPreviewSource != header;
		boolean sideChanged = insertionAfter == null || insertionAfter != after;
		if (!sourceChanged && !sideChanged)
			return;

		insertionPreviewSource = header;
		insertionAfter = after;
		insertionIndicator.setVisible(true);
		updateInsertionIndicator();
	}

	/**
	 * Clears the insertion indicator.
	 *
	 * @see #enableInsertionIndicator(Header, boolean)
	 */
	private void disableInsertionIndicator() {
		insertionIndicator.setVisible(false);
		insertionPreviewSource = null;
		insertionAfter = null;
		insertionMidpoint = Double.NaN;
	}

	/**
	 * Updates the insertion indicator to be at the left/right
	 * or top/bottom edge of this header depending on the current side/orientation.
	 */
	private void updateInsertionIndicator() {
		// Skip if the indicator is not visible or if we don't know where to put it.
		if (!insertionIndicator.isVisible() || insertionAfter == null)
			return;

		// For horizontal headers, the indicator is a vertical line at the left or right edge of the header.
		// For vertical headers, the indicator is a horizontal line at the top or bottom edge of the header.
		Orientation orientation = BentoUtils.sideToOrientation(getSide());
		if (orientation == HORIZONTAL) {
			double x = insertionAfter ? Math.max(0, wrapper.getWidth() - 1) : 1;
			insertionIndicator.setStartX(x);
			insertionIndicator.setEndX(x);
			insertionIndicator.setStartY(0);
			insertionIndicator.setEndY(wrapper.getHeight());
		} else {
			double y = insertionAfter ? Math.max(0, wrapper.getHeight() - 1) : 1;
			insertionIndicator.setStartX(0);
			insertionIndicator.setEndX(wrapper.getWidth());
			insertionIndicator.setStartY(y);
			insertionIndicator.setEndY(y);
		}
	}

	/**
	 * Determines if the given drag event is on the "after" half of this header.
	 *
	 * @param event
	 * 		Drag event to check.
	 *
	 * @return {@code true} when the event is on the "after" half of this header,
	 * {@code false} when it is on the "before" half.
	 */
	private boolean isDropAfter(DragEvent event) {
		Orientation orientation = BentoUtils.sideToOrientation(getSide());

		// Determine the coordinate of the event and the extent of the header in the orientation direction.
		double coordinate;
		double extent;
		if (orientation == HORIZONTAL) {
			coordinate = event.getX();
			extent = getWidth();
		} else {
			coordinate = event.getY();
			extent = getHeight();
		}

		// Capture the midpoint before the first preview so the insertion boundary remains
		// stable for the whole drag, even if the target is resized while it is hovered.
		if (Double.isNaN(insertionMidpoint))
			insertionMidpoint = extent / 2;
		return coordinate > insertionMidpoint;
	}

	/**
	 * Update the {@code selected} pseudo-state.
	 *
	 * @param selected
	 * 		Selected stage.
	 */
	public void setSelected(boolean selected) {
		pseudoClassStateChanged(PSEUDO_SELECTED, selected);
	}

	/**
	 * @return Wrapped dockable.
	 */
	public Dockable getDockable() {
		return dockable;
	}

	/**
	 * @return Side of the {@link #parentPane} at the time of construction.
	 */
	private @Nullable Side getSide() {
		return sideProperty.get();
	}

	@Override
	public String toString() {
		return "Header:" + titleProperty.get();
	}
}
