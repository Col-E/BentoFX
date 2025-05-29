package software.coley.bentofx.header;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import software.coley.bentofx.space.DockSpace;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableCloseListener;
import software.coley.bentofx.dockable.DockableDestination;
import software.coley.bentofx.dockable.DockableMenuFactory;
import software.coley.bentofx.impl.ImplBento;
import software.coley.bentofx.impl.ImplDockable;
import software.coley.bentofx.path.SpacePath;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.util.BentoUtils;
import software.coley.bentofx.util.DragDropStage;
import software.coley.bentofx.util.DropTargetType;

import java.util.function.Consumer;

public class Header extends Group {
	public static final PseudoClass PSEUDO_SELECTED = PseudoClass.getPseudoClass("selected");
	public static final PseudoClass PSEUDO_HOVER = PseudoClass.getPseudoClass("hover");
	public static final PseudoClass PSEUDO_SIDE_TOP = PseudoClass.getPseudoClass("top");
	public static final PseudoClass PSEUDO_SIDE_BOTTOM = PseudoClass.getPseudoClass("bottom");
	public static final PseudoClass PSEUDO_SIDE_LEFT = PseudoClass.getPseudoClass("left");
	public static final PseudoClass PSEUDO_SIDE_RIGHT = PseudoClass.getPseudoClass("right");
	private final BooleanProperty closableProperty = new SimpleBooleanProperty(true);
	private final ObjectProperty<Side> sideProperty = new SimpleObjectProperty<>(Side.TOP);
	private final ObjectProperty<Node> graphicProperty = new SimpleObjectProperty<>();
	private final ObjectProperty<Tooltip> tooltipProperty = new SimpleObjectProperty<>();
	private final GridPane grid = new GridPane();
	private final Text label = new Text();
	private final Pane graphicWrapper = new Pane();
	private final BorderPane ghostWrapper = new BorderPane();
	private final Dockable dockable;
	private ImplBento bento;
	private ContextMenu cachedContextMenu;

	public Header(@Nonnull Dockable dockable, @Nonnull Side side) {
		this.dockable = dockable;

		grid.getStyleClass().add("header");
		ghostWrapper.getStyleClass().add("dock-ghost-zone");

		switch (side) {
			case TOP -> grid.pseudoClassStateChanged(PSEUDO_SIDE_TOP, true);
			case BOTTOM -> grid.pseudoClassStateChanged(PSEUDO_SIDE_BOTTOM, true);
			case LEFT -> grid.pseudoClassStateChanged(PSEUDO_SIDE_LEFT, true);
			case RIGHT -> grid.pseudoClassStateChanged(PSEUDO_SIDE_RIGHT, true);
		}

		// Setup tooltip registration
		tooltipProperty.addListener((ob, old, cur) -> {
			if (old != null)
				Tooltip.uninstall(this, old);
			if (cur != null)
				Tooltip.install(this, cur);
		});

		// Bind dockable properties
		closableProperty.bind(dockable.closableProperty());
		textProperty().bind(dockable.titleProperty());
		tooltipProperty().bind(dockable.tooltipProperty());
		graphicProperty().bind(dockable.iconFactoryProperty().map(ic -> ic.build(dockable)));

		// Layout
		Label graphicHolder = new Label();
		graphicHolder.graphicProperty().bind(graphicProperty);
		graphicWrapper.getChildren().add(graphicHolder);
		sideProperty.set(side);
		sideProperty.addListener((ob, old, cur) -> recomputeLayout(cur));
		grid.setHgap(6);
		grid.setVgap(6);
		grid.setPadding(new Insets(6));
		grid.setAlignment(Pos.CENTER);
		BorderPane wrapper = new BorderPane();
		wrapper.setCenter(grid);
		wrapper.setLeft(ghostWrapper);
		getChildren().add(wrapper);
		recomputeLayout(side);
	}

	public Header(@Nonnull ImplBento bento, @Nonnull Dockable dockable, @Nonnull Side side) {
		this(dockable, side);

		this.bento = bento;

		// Hover support
		addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
			if (!isDisable()) grid.pseudoClassStateChanged(PSEUDO_HOVER, true);
		});
		addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
			if (!isDisable()) grid.pseudoClassStateChanged(PSEUDO_HOVER, false);
		});

		// Support for:
		//  - Selecting tabs
		//  - Toggling tab collapsing
		//  - Showing context menus
		// Handled on-click because on-mouse-down conflicts with drag initialization.
		setOnMouseClicked(e -> {
			// Primary click --> select dockable if not selected, otherwise toggle collapsed state.
			if (e.getButton() == MouseButton.PRIMARY)
				inParentDestination(d -> {
					if (d.getSelectedDockable() != dockable) {
						d.selectDockable(dockable);
					} else {
						// Clicked already selected dockable, (un)collapse it.
						d.toggleCollapsed();
					}
				});

			// Secondary click --> populate context menu
			if (e.getButton() == MouseButton.SECONDARY) {
				ContextMenu menu = cachedContextMenu;
				if (menu == null) {
					DockableMenuFactory factory = dockable.contextMenuFactoryProperty().getValue();
					if (factory != null)
						menu = factory.build(dockable);

					// Cache for next time
					if (dockable.cachedContextMenuProperty().get())
						cachedContextMenu = menu;
				}

				// Show if a menu was provided
				if (menu != null) {
					menu.setAutoHide(true);
					menu.show(this, e.getScreenX(), e.getScreenY());
				}
			}
		});

		// Closing support
		setOnMouseReleased(e -> {
			// Middle release --> close dockable
			if (e.getButton() == MouseButton.MIDDLE && getBoundsInLocal().contains(e.getX(), e.getY()))
				removeFromParent(RemovalReason.CLOSING);
		});

		// Start dragging this header
		if (dockable.canBeDragged().get()) {
			setOnDragDetected(e -> {
				// Drag can only be initiated by primary mouse button drags.
				if (e.getButton() == MouseButton.PRIMARY) {
					Image image = snapshot(null, null);
					Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
					dragboard.setContent(BentoUtils.content(dockable));
					dragboard.setDragView(image);
					e.consume();
				}
			});
		}

		// Any header can be the target of drag-n-drop. Dropped items will be inserted before the target (this) tab,
		setOnDragOver(e -> {
			Dragboard dragboard = e.getDragboard();
			String dockableIdentifier = BentoUtils.extractIdentifier(dragboard);
			if (dockableIdentifier != null) {
				// Only visually update when our header does not match the dragged one.
				// We still need to accept the other header even if it is a match so that the
				// drag-done handler doesn't try and plop the header into a new window.
				//
				// In the case where it is our own header, we'll handle that in the completion logic.
				if (!dockable.getIdentifier().equals(dockableIdentifier)) {
					inParentDestination(d -> {
						Integer dragGroup = BentoUtils.extractDragGroup(dragboard);
						if (dragGroup != null && d.canReceiveDragGroup(dragGroup)) {
							// Update insertion ghost and re-layout the parent.
							Header header = BentoUtils.getHeader(bento, e);
							if (header != null) {
								enableGhost(header);
								d.drawCanvasHint(ghostWrapper);
							}
						} else {
							disableGhost();
							d.clearCanvas();
						}
					});
				}
				e.acceptTransferModes(TransferMode.MOVE);
			}

			// Do not propagate upwards.
			e.consume();
		});
		setOnDragExited(e -> {
			// Update insertion ghost and re-layout the parent.
			disableGhost();

			// Clear canvas/drawing.
			inParentDestination(DockableDestination::clearCanvas);
		});
		setOnDragDropped(e -> {
			// Drag source must be a header.
			Header headerSource = BentoUtils.getHeader(bento, e);
			if (headerSource == null)
				return;

			// Drag source must have a parent destination it currently belongs to.
			DockableDestination sourceParent = headerSource.getParentDestination();
			if (sourceParent == null)
				return;

			// Drag completion. The target should be "this" header.
			if (e.getGestureTarget() instanceof Header headerTarget) {
				// Ensure the headers aren't the same.
				if (headerSource == headerTarget)
					return;

				// Ensure we can access the parent container of the dragged header
				// and the target where it will be dropped into.
				DockableDestination targetContainer = headerTarget.getParentDestination();
				if (targetContainer == null)
					return;

				// Check if the container (our parent) can receive this header.
				//  - Our parent and the source's parent are the same.
				//  - Our parents are different, but our parent can receive the header.
				//    - We pass 'null' because we do not intend to split. We're adding it after the current header.
				boolean sameContainer = targetContainer == sourceParent;
				if (sameContainer || targetContainer.canReceiveHeader(null, headerSource)) {
					// Move the header over to the target container and select it.
					int targetIndex = targetContainer.getDockables().indexOf(headerTarget.dockable);

					// Need to offset the target when re-ordering in the same container to accommodate for the initial
					// removal shifting the intended target by one.
					if (sameContainer && targetIndex >= sourceParent.getDockables().indexOf(headerSource.dockable))
						targetIndex--;

					// Remove from source, put into target at given index, and select it.
					sourceParent.removeDockable(headerSource.dockable);
					targetContainer.addDockable(targetIndex, headerSource.dockable);
					targetContainer.selectDockable(headerSource.dockable);
					BentoUtils.completeDnd(e, dockable, DropTargetType.HEADER);
				}
			}
		});
		setOnDragDone(e -> {
			// Drag source must be a header.
			Header headerSource = BentoUtils.getHeader(bento, e);
			if (headerSource == null)
				return;

			// Drag source must have a parent destination it currently belongs to.
			DockableDestination sourceParent = headerSource.getParentDestination();
			if (sourceParent == null)
				return;

			// Drag source must not be a drag-drop-stage with the source header as the only item.
			// We don't want to close the window just to open a new one with the same content, that would be dumb.
			Scene scene = sourceParent.getBackingRegion().getScene();
			if (scene.getWindow() instanceof DragDropStage && BentoUtils.getChildren(scene.getRoot(), Header.class).size() == 1)
				return;

			// Drag completion event must not have a drop target specified.
			if (BentoUtils.extractDropTargetType(e.getDragboard()) != null)
				return;

			// Handle opening in a new window when drag completes without a found target.
			if (e.getGestureTarget() == null
					&& headerSource.dockable.canBeDroppedToNewWindow().get()
					&& headerSource.removeFromParent(RemovalReason.MOVING)) {
				Stage stage = bento.newStageForDroppedHeader(sourceParent.getComposedDestinationRoot(), headerSource);
				stage.show();
				stage.toFront();
				stage.requestFocus();
				BentoUtils.completeDnd(e, headerSource.dockable, DropTargetType.EXTERNAL);
			}
		});
	}

	private void enableGhost(@Nonnull Header header) {
		grid.setMouseTransparent(true);
		grid.setManaged(false);
		Orientation ourOrientation = BentoUtils.sideToOrientation(getSide());
		Orientation otherOrientation = BentoUtils.sideToOrientation(header.getSide());
		if (ourOrientation == Orientation.HORIZONTAL) {
			grid.setTranslateX(otherOrientation == ourOrientation ? header.getLayoutBounds().getWidth() : header.getLayoutBounds().getHeight());
		} else {
			grid.setTranslateY(otherOrientation == ourOrientation ? header.getLayoutBounds().getHeight() : header.getLayoutBounds().getWidth());
		}
		ghostWrapper.setCenter(new Header(header.dockable, getSide()));
		getParent().requestLayout();
	}

	private void disableGhost() {
		grid.setMouseTransparent(false);
		grid.setManaged(true);
		grid.setTranslateX(0);
		grid.setTranslateY(0);
		ghostWrapper.setCenter(null);
		getParent().requestLayout();
	}

	/**
	 * @return Header's associated dockable.
	 */
	@Nonnull
	public Dockable getDockable() {
		return dockable;
	}

	/**
	 * @return Header width.
	 */
	public double getWidth() {
		return grid.getWidth();
	}

	/**
	 * @return Header height.
	 */
	public double getHeight() {
		return grid.getHeight();
	}

	/**
	 * Attempts to remove the header from its parent. This can be blocked if the {@link #dockable} is not closable.
	 *
	 * @param reason
	 * 		Reason for removal.
	 *
	 * @return {@code true} when removed. {@code false} when not removed.
	 */
	public boolean removeFromParent(@Nonnull RemovalReason reason) {
		// Skip if not closable and the removal is from closing.
		if (reason == RemovalReason.CLOSING && !isClosable())
			return false;

		// Skip if already removed.
		if (dockable == null)
			return false;

		// Remove if parent destination (dockable container) exists.
		DockableDestination destination = getParentDestination();
		if (destination != null) {
			DockablePath path = getPath();
			if (path != null && destination.removeDockable(dockable)) {
				if (reason == RemovalReason.CLOSING) {
					((ImplDockable) dockable).onClose(path);
					if (bento != null)
						for (DockableCloseListener listener : bento.getCloseListeners())
							listener.onClose(path, dockable);
				} else {
					((ImplDockable) dockable).setPriorPath(path);
				}
				cleanup();
				return true;
			}
		}
		return false;
	}

	@Nullable
	public DockablePath getPath() {
		DockableDestination destination = getParentDestination();
		if (destination == null)
			return null;

		DockSpace space = destination.getParentSpace();
		if (space == null)
			return null;

		SpacePath spacePath = space.getPath();
		if (spacePath == null)
			return null;

		return new DockablePath(spacePath, dockable);
	}

	@Nullable
	public DockableDestination getParentDestination() {
		return BentoUtils.getParent(this, DockableDestination.class);
	}

	@Nullable
	public HeaderView getParentHeaderView() {
		return BentoUtils.getParent(this, HeaderView.class);
	}

	public void inParentDestination(@Nonnull Consumer<DockableDestination> action) {
		DockableDestination destination = getParentDestination();
		if (destination != null)
			action.accept(destination);
	}

	private void cleanup() {
		closableProperty.unbind();
		textProperty().unbind();
		tooltipProperty().unbind();
		graphicProperty().unbind();
	}

	private void recomputeLayout(@Nonnull Side side) {
		grid.getChildren().clear();
		switch (side) {
			case TOP, BOTTOM -> {
				grid.add(graphicWrapper, 0, 0);
				grid.add(label, 1, 0);
				label.setRotate(0);
			}
			case LEFT -> {
				label.setRotate(-90);
				grid.add(new Group(label), 0, 0);
				grid.add(graphicWrapper, 0, 1);
			}
			case RIGHT -> {
				label.setRotate(90);
				grid.add(graphicWrapper, 0, 0);
				grid.add(new Group(label), 0, 1);
			}
		}
		requestLayout();
	}

	/**
	 * Ensures this header fits the parent on the perpendicular axis it is aligned to.
	 * <ul>
	 * <li>Horizontal headers will fill to the parent height.</li>
	 * <li>Vertical headers will fill to the parent width.</li>
	 * </ul>
	 */
	public void bindToParentDimensions() {
		HeaderRegion parent = BentoUtils.getParent(this, HeaderRegion.class);
		if (parent != null) {
			switch (getSide()) {
				case TOP, BOTTOM -> grid.prefHeightProperty().bind(parent.heightProperty());
				case LEFT, RIGHT -> grid.prefWidthProperty().bind(parent.widthProperty());
			}
		}
	}

	@Nonnull
	public StringProperty textProperty() {
		return label.textProperty();
	}

	@Nonnull
	public ObjectProperty<Node> graphicProperty() {
		return graphicProperty;
	}

	@Nonnull
	public ObjectProperty<Tooltip> tooltipProperty() {
		return tooltipProperty;
	}

	public boolean isClosable() {
		return closableProperty.get();
	}

	@Nonnull
	public Side getSide() {
		return sideProperty.get();
	}

	public void setSide(@Nonnull Side side) {
		sideProperty.set(side);
	}

	public void setSelected(boolean selected) {
		grid.pseudoClassStateChanged(Header.PSEUDO_SELECTED, selected);
	}

	public enum RemovalReason {
		CLOSING, MOVING
	}
}
