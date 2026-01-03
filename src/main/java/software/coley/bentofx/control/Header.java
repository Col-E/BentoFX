package software.coley.bentofx.control;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import software.coley.bentofx.Bento;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableMenuFactory;
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
	private final ObjectProperty<Side> sideProperty = new SimpleObjectProperty<>();
	private final ObjectProperty<Tooltip> tooltipProperty = new SimpleObjectProperty<>();
	private final GridPane grid = new GridPane();
	private final Text label = new Text();
	private final Pane graphicWrapper = new Pane();
	private final Pane closeWrapper = new Pane();
	private final BorderPane ghostWrapper = new BorderPane();
	private final HeaderPane parentPane;
	private final Dockable dockable;

	/**
	 * @param dockable
	 * 		Dockable to wrap.
	 * @param parentPane
	 * 		Parent header pane.
	 */
	public Header(@Nonnull Dockable dockable, @Nonnull HeaderPane parentPane) {
		this.parentPane = parentPane;
		this.dockable = dockable;

		getStyleClass().add("header");
		ghostWrapper.getStyleClass().add("dock-ghost-zone");

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
		graphicHolder.graphicProperty().bind(graphicProperty);
		graphicWrapper.getChildren().add(graphicHolder);
		sideProperty.addListener((ob, old, cur) -> recomputeLayout(cur));
		closableProperty.addListener((ob, old, cur) -> recomputeLayout(getSide()));
		grid.setHgap(6);
		grid.setVgap(6);
		grid.setPadding(new Insets(6));
		grid.setAlignment(Pos.CENTER);
		BorderPane wrapper = new BorderPane();
		wrapper.setCenter(grid);
		wrapper.setLeft(ghostWrapper);
		getChildren().add(wrapper);
		recomputeLayout(getSide());
	}

	/**
	 * Populate drag-n-drop event handling.
	 *
	 * @return This.
	 */
	@Nonnull
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

		// Any header can be the target of drag-n-drop. Dropped items will be inserted before the target (this) tab.
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
						Dockable dragSourceDockable = dragSourcePath.dockable();
						DockContainerLeaf container = parentPane.getContainer();
						if (container.canReceiveDockable(dragSourceDockable, getSide())) {
							// TODO: This may be a bit dumb. If it ever presents a problem
							//  we can reimplement this as creating a new header from the drag-drop dockable.
							Header dragSourceHeader = dragSourcePath.leafContainer().getHeader(dragSourceDockable);
							if (dragSourceHeader != null)
								enableInsertionGhost(dragSourceHeader);
							container.drawCanvasHint(ghostWrapper);
						} else {
							disableInsertionGhost();
							container.clearCanvas();
						}
					}
				}
				e.acceptTransferModes(TransferMode.MOVE);
			}

			// Do not propagate upwards.
			e.consume();
		});
		setOnDragExited(e -> {
			// Update insertion ghost and re-layout the parent.
			disableInsertionGhost();

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
				int insertionIndex = parentContainer.getDockables().indexOf(dockable);

				// Need to offset the target when re-ordering in the same container to accommodate for the initial
				// removal shifting the intended target by one.
				if (sameContainer && insertionIndex >= parentContainer.getDockables().indexOf(sourceDockable))
					insertionIndex--;

				// Remove from source, put into target at given index, and select it.
				sourceContainer.removeDockable(sourceDockable);
				parentContainer.addDockable(insertionIndex, sourceDockable);
				parentContainer.selectDockable(sourceDockable);
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
	 * Draws the given header as a ghost, intended for insertion just before this header.
	 *
	 * @param header
	 * 		Some other header to draw as a ghost.
	 */
	private void enableInsertionGhost(@Nonnull Header header) {
		grid.setMouseTransparent(true);
		grid.setManaged(false);
		Orientation ourOrientation = BentoUtils.sideToOrientation(getSide());
		Orientation otherOrientation = BentoUtils.sideToOrientation(header.getSide());
		if (ourOrientation == HORIZONTAL) {
			grid.setTranslateX(otherOrientation == ourOrientation ? header.getLayoutBounds().getWidth() : header.getLayoutBounds().getHeight());
		} else {
			grid.setTranslateY(otherOrientation == ourOrientation ? header.getLayoutBounds().getHeight() : header.getLayoutBounds().getWidth());
		}
		ghostWrapper.setCenter(new Header(header.dockable, parentPane));
		getParent().requestLayout();
	}

	/**
	 * Clears the {@link #ghostWrapper}.
	 *
	 * @see #enableInsertionGhost(Header)
	 */
	private void disableInsertionGhost() {
		grid.setMouseTransparent(false);
		grid.setManaged(true);
		grid.setTranslateX(0);
		grid.setTranslateY(0);
		ghostWrapper.setCenter(null);
		getParent().requestLayout();
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
	@Nonnull
	public Dockable getDockable() {
		return dockable;
	}

	/**
	 * @return Side of the {@link #parentPane} at the time of construction.
	 */
	@Nullable
	private Side getSide() {
		return sideProperty.get();
	}


	@Override
	public String toString() {
		return "Header:" + titleProperty.get();
	}
}
