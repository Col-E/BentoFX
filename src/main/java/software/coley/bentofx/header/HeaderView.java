package software.coley.bentofx.header;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableDestination;
import software.coley.bentofx.impl.ImplBento;
import software.coley.bentofx.layout.DockLayout;
import software.coley.bentofx.layout.SplitDockLayout;
import software.coley.bentofx.space.DockSpace;
import software.coley.bentofx.space.TabbedDockSpace;
import software.coley.bentofx.space.TabbedSpaceMenuFactory;
import software.coley.bentofx.util.BentoUtils;

import java.util.List;

public class HeaderView extends StackPane implements DockableDestination {
	public static final PseudoClass PSEUDO_ACTIVE = PseudoClass.getPseudoClass("active");
	private final ImplBento bento;
	private final String identifier = BentoUtils.newIdentifier();
	private final ContentWrapper contentWrapper;
	private final HeaderRegion headerRegion;
	private final Canvas canvas = new Canvas();
	private final ObjectProperty<TabbedSpaceMenuFactory> menuFactory = new SimpleObjectProperty<>();

	public HeaderView(@Nonnull ImplBento bento, @Nonnull Side side) {
		this.bento = bento;

		headerRegion = new HeaderRegion(bento, this, side);
		contentWrapper = new ContentWrapper(bento, this);

		Button dockableListButton = createDockableListButton();
		Button spaceConfigButton = createSpaceConfigButton();

		getStyleClass().add("header-view");
		switch (side) {
			case TOP -> pseudoClassStateChanged(Header.PSEUDO_SIDE_TOP, true);
			case BOTTOM -> pseudoClassStateChanged(Header.PSEUDO_SIDE_BOTTOM, true);
			case LEFT -> pseudoClassStateChanged(Header.PSEUDO_SIDE_LEFT, true);
			case RIGHT -> pseudoClassStateChanged(Header.PSEUDO_SIDE_RIGHT, true);
		}

		// Track that this view has focus somewhere in the hierarchy.
		// This will allow us to style the active view's subclasses specially.
		focusWithinProperty().addListener((ob, old, cur) -> pseudoClassStateChanged(PSEUDO_ACTIVE, cur));

		// Fit the canvas to the container size
		canvas.setManaged(false);
		canvas.setMouseTransparent(true);
		canvas.widthProperty().bind(widthProperty());
		canvas.heightProperty().bind(heightProperty());

		// Put the headers on the appropriate side, content in the center will be placed later
		BorderPane layoutWrapper = new BorderPane();
		BorderPane regionWrapper = new BorderPane(headerRegion);
		Rectangle clip = new Rectangle(); // Use a clip to prevent headers from rendering beyond expected bounds
		clip.widthProperty().bind(regionWrapper.widthProperty());
		clip.heightProperty().bind(regionWrapper.heightProperty());
		regionWrapper.setClip(clip);
		regionWrapper.getStyleClass().add("header-region-wrapper");
		layoutWrapper.setCenter(contentWrapper);
		switch (side) {
			// TODO: Reduce duplicate code here
			case TOP -> {
				HBox headerControls = new HBox(new Group(dockableListButton), new Group(spaceConfigButton));
				headerControls.visibleProperty().bind(dockableListButton.visibleProperty().or(spaceConfigButton.visibleProperty()));
				headerControls.getStyleClass().add("button-bar");
				headerControls.setSpacing(-1);

				dockableListButton.prefHeightProperty().bind(headerControls.heightProperty());
				spaceConfigButton.prefHeightProperty().bind(headerControls.heightProperty());

				regionWrapper.setRight(headerControls);
				layoutWrapper.setTop(regionWrapper);
			}
			case BOTTOM -> {
				HBox headerControls = new HBox(new Group(dockableListButton), new Group(spaceConfigButton));
				headerControls.visibleProperty().bind(dockableListButton.visibleProperty().or(spaceConfigButton.visibleProperty()));
				headerControls.getStyleClass().add("button-bar");
				headerControls.setSpacing(-1);

				dockableListButton.prefHeightProperty().bind(headerControls.heightProperty());
				spaceConfigButton.prefHeightProperty().bind(headerControls.heightProperty());

				regionWrapper.setRight(headerControls);
				layoutWrapper.setBottom(regionWrapper);
			}
			case LEFT -> {
				VBox headerControls = new VBox(new Group(dockableListButton), new Group(spaceConfigButton));
				headerControls.visibleProperty().bind(dockableListButton.visibleProperty().or(spaceConfigButton.visibleProperty()));
				headerControls.getStyleClass().add("button-bar");
				headerControls.setSpacing(-1);

				dockableListButton.prefWidthProperty().bind(headerControls.widthProperty());
				spaceConfigButton.prefWidthProperty().bind(headerControls.widthProperty());

				regionWrapper.setBottom(headerControls);
				layoutWrapper.setLeft(regionWrapper);
			}
			case RIGHT -> {
				VBox headerControls = new VBox(new Group(dockableListButton), new Group(spaceConfigButton));
				headerControls.visibleProperty().bind(dockableListButton.visibleProperty().or(spaceConfigButton.visibleProperty()));
				headerControls.getStyleClass().add("button-bar");
				headerControls.setSpacing(-1);

				dockableListButton.prefWidthProperty().bind(headerControls.widthProperty());
				spaceConfigButton.prefWidthProperty().bind(headerControls.widthProperty());

				regionWrapper.setBottom(headerControls);
				layoutWrapper.setRight(regionWrapper);
			}
		}

		// Put canvas on top
		getChildren().addAll(layoutWrapper, canvas);
	}

	@Nonnull
	private Button createDockableListButton() {
		Button button = new Button("▼");
		button.setEllipsisString("▼");
		button.getStyleClass().addAll("corner-button", "list-button");
		button.setOnMousePressed(e -> {
			// TODO: A name filter that appears when you begin to type would be nice
			ContextMenu menu = new ContextMenu();
			menu.getItems().addAll(getDockables().stream().map(d -> {
				MenuItem item = new MenuItem();
				item.textProperty().bind(d.titleProperty());
				item.graphicProperty().bind(d.iconFactoryProperty().map(ic -> ic.build(d)));
				item.setOnAction(ignored -> selectDockable(d));
				return item;
			}).toList());
			button.setContextMenu(menu);
		});
		button.setOnMouseClicked(e -> button.getContextMenu().show(button, e.getScreenX(), e.getScreenY()));
		button.visibleProperty().bind(headerRegion.overflowingProperty());
		return button;
	}

	@Nonnull
	private Button createSpaceConfigButton() {
		Button button = new Button("≡");
		button.setEllipsisString("≡");
		button.getStyleClass().addAll("corner-button", "context-button");
		button.setOnMousePressed(e -> {
			if (getParentSpace() instanceof TabbedDockSpace tabbed) {
				button.setContextMenu(menuFactory.get().build(tabbed));
			} else {
				button.setContextMenu(null);
			}
		});
		button.setOnMouseClicked(e -> button.getContextMenu().show(button, e.getScreenX(), e.getScreenY()));
		button.visibleProperty().bind(menuFactory.isNotNull());
		return button;
	}

	@Nonnull
	public Region getContentWrapper() {
		return contentWrapper;
	}

	@Nonnull
	public ObservableValue<? extends Dockable> selectedProperty() {
		return headerRegion.selectedProperty();
	}

	@Nonnull
	public ObjectProperty<TabbedSpaceMenuFactory> menuFactoryProperty() {
		return menuFactory;
	}

	@Nullable
	public Header getHeader(@Nonnull Dockable dockable) {
		return headerRegion.getHeader(dockable);
	}

	@Override
	public boolean addDockable(@Nonnull Dockable dockable) {
		return headerRegion.addDockable(dockable);
	}

	@Override
	public boolean addDockable(int index, @Nonnull Dockable dockable) {
		return headerRegion.addDockable(index, dockable);
	}

	@Override
	public boolean removeDockable(@Nonnull Dockable dockable) {
		return headerRegion.removeDockable(dockable);
	}

	@Override
	public boolean selectDockable(@Nullable Dockable dockable) {
		return headerRegion.selectDockable(dockable);
	}

	@Nullable
	@Override
	public DockSpace getParentSpace() {
		return BentoUtils.getOrParent(getParent(), DockSpace.class);
	}

	@Nullable
	@Override
	public DockLayout getParentLayout() {
		return BentoUtils.getOrParent(getParent(), DockLayout.class);
	}

	@Nonnull
	@Override
	public List<Dockable> getDockables() {
		return headerRegion.getDockables();
	}

	@Nullable
	@Override
	public Dockable getSelectedDockable() {
		return headerRegion.getSelectedDockable();
	}

	@Override
	public boolean canReceiveDragGroup(int dragGroup) {
		return headerRegion.canReceiveDragGroup(dragGroup);
	}

	@Override
	public boolean canSplit() {
		return headerRegion.canSplit();
	}

	@Override
	public boolean canReceiveHeader(@Nullable Side droppedSide, @Nonnull Header source) {
		return headerRegion.canReceiveHeader(droppedSide, source);
	}

	@Override
	public boolean receiveDroppedHeader(@Nonnull DragEvent event, @Nullable Side droppedSide, @Nonnull Header source) {
		return headerRegion.receiveDroppedHeader(event, droppedSide, source);
	}

	@Nonnull
	@Override
	public Region getBackingRegion() {
		return this;
	}

	@Override
	public void drawCanvasHint(@Nonnull Region target, @Nullable Side side) {
		// Compute xy offset when 'target' is not a direct child of this view.
		double ox = 0;
		double oy = 0;
		Parent parent = target.getParent();
		while (parent != null && parent != this) {
			ox += parent.getLayoutX();
			oy += parent.getLayoutY();
			parent = parent.getParent();
		}

		// Clear any old graphics.
		GraphicsContext g = canvas.getGraphicsContext2D();
		g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
		g.setStroke(Color.RED);
		g.setFill(Color.rgb(255, 0, 0, 0.25));

		// Draw a rect around the given target region.
		final double x = ox + target.getLayoutX();
		final double y = oy + target.getLayoutY();
		final double w = target.getWidth();
		final double h = target.getHeight();
		switch (side) {
			case TOP -> {
				g.fillRect(x, y, w, h / 2);
				g.strokeRect(x, y, w, h / 2);
			}
			case BOTTOM -> {
				g.fillRect(x, y + h / 2, w, h / 2);
				g.strokeRect(x, y + h / 2, w, h / 2);
			}
			case LEFT -> {
				g.fillRect(x, y, w / 2, h);
				g.strokeRect(x, y, w / 2, h);
			}
			case RIGHT -> {
				g.fillRect(x + w / 2, y, w / 2, h);
				g.strokeRect(x + w / 2, y, w / 2, h);
			}
			case null -> {
				g.fillRect(x, y, w, h);
				g.strokeRect(x, y, w, h);
			}
		}
	}

	@Override
	public void clearCanvas() {
		GraphicsContext g = canvas.getGraphicsContext2D();
		g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
	}

	@Override
	public boolean toggleCollapsed() {
		SplitDockLayout parentSplitLayout = BentoUtils.getOrParent(this, SplitDockLayout.class);
		DockLayout parentLayout = BentoUtils.getOrParent(this, DockLayout.class);

		// Skip if there is no parent split or parent layout.
		if (parentSplitLayout == null || parentLayout == null)
			return false;

		return parentSplitLayout.setChildCollapsed(parentLayout, !parentSplitLayout.isChildCollapsed(parentLayout));
	}

	@Override
	public boolean isCollapsed() {
		SplitDockLayout parentSplitLayout = BentoUtils.getOrParent(this, SplitDockLayout.class);
		DockLayout parentLayout = BentoUtils.getOrParent(this, DockLayout.class);

		// Skip if there is no parent split or parent layout.
		if (parentSplitLayout == null || parentLayout == null)
			return false;

		return parentSplitLayout.isChildCollapsed(parentLayout);
	}

	@Nonnull
	@Override
	public DockableDestination getComposedDestinationRoot() {
		return this;
	}

	@Nonnull
	@Override
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public boolean matchesIdentity(@Nullable Identifiable other) {
		if (other == null) return false;
		return identifier.equals(other.getIdentifier());
	}

	private static class ContentWrapper extends BorderPane implements DockableDestination {
		private final HeaderView parentView;

		private ContentWrapper(@Nonnull Bento bento, @Nonnull HeaderView parentView) {
			this.parentView = parentView;

			getStyleClass().add("node-wrapper");

			// Always show selected content
			parentView.headerRegion.selectedProperty().addListener((ob, old, cur) -> {
				// We need to always unbind the center property so that we are free to either
				// set the value to something else or rebind it.
				centerProperty().unbind();

				if (cur != null) {
					// Bind the current selected dockable's content.
					// - We need to ensure that the dockable's prior containing display unbinds it as a child.
					//   - https://bugs.openjdk.org/browse/JDK-8137251
					//   - This control will unbind its prior value when we tell it to bind the new value
					ObjectProperty<Node> dockableNode = cur.nodeProperty();
					if (dockableNode.get().getParent() instanceof ContentWrapper otherParent)
						otherParent.centerProperty().unbind();
					centerProperty().bind(dockableNode);

					// If the new content is focusable, focus it.
					Node center = getCenter();
					if (center != null && center.isFocusTraversable())
						center.requestFocus();
				} else {
					DockLayout parentLayout = getParentLayout();
					if (parentLayout != null && !isCollapsed()) {
						// Replace the content with the "empty-content" template.
						DockSpace replacement = parentView.bento.newEmptySpace(parentLayout);
						setCenter(replacement.getBackingRegion());
					} else {
						// Shouldn't happen unless the header-view doesn't belong to a scene graph.
						// In that case it doesn't really matter anyways.
						setCenter(null);
					}
				}
			});

			// Setup drag-n-drop
			BentoUtils.setupCommonDragSupport(this, true);
			setOnDragDropped(e -> {
				// Ensure the origin is a header, and the target is a region that can be dropped into.
				Header headerSource = BentoUtils.getHeader(bento, e);
				if (headerSource == null)
					return;
				if (!(e.getGestureTarget() instanceof DockableDestination target))
					return;

				// Ensure the header knows where to remove itself from.
				DockableDestination sourceParent = headerSource.getParentDestination();
				if (sourceParent == null)
					return;

				// Delegate drop handling to the target.
				Side targetSide = target.canSplit() ? BentoUtils.computeClosestSide(this, e.getX(), e.getY()) : null;
				target.receiveDroppedHeader(e, targetSide, headerSource);
			});
		}

		@Nullable
		@Override
		public DockSpace getParentSpace() {
			return parentView.getParentSpace();
		}

		@Nullable
		@Override
		public DockLayout getParentLayout() {
			return parentView.getParentLayout();
		}

		@Nonnull
		@Override
		public List<Dockable> getDockables() {
			return parentView.getDockables();
		}

		@Nullable
		@Override
		public Dockable getSelectedDockable() {
			return parentView.getSelectedDockable();
		}

		@Override
		public boolean canReceiveDragGroup(int dragGroup) {
			return parentView.canReceiveDragGroup(dragGroup);
		}

		@Override
		public boolean canSplit() {
			return parentView.canSplit();
		}

		@Override
		public boolean canReceiveHeader(@Nullable Side droppedSide, @Nonnull Header source) {
			return parentView.canReceiveHeader(droppedSide, source);
		}

		@Override
		public boolean receiveDroppedHeader(@Nonnull DragEvent event, @Nullable Side droppedSide, @Nonnull Header source) {
			return parentView.receiveDroppedHeader(event, droppedSide, source);
		}

		@Override
		public boolean addDockable(@Nonnull Dockable dockable) {
			return parentView.addDockable(dockable);
		}

		@Override
		public boolean addDockable(int index, @Nonnull Dockable dockable) {
			return parentView.addDockable(index, dockable);
		}

		@Override
		public boolean removeDockable(@Nonnull Dockable dockable) {
			return parentView.removeDockable(dockable);
		}

		@Override
		public boolean selectDockable(@Nonnull Dockable dockable) {
			return parentView.selectDockable(dockable);
		}

		@Override
		public void drawCanvasHint(@Nonnull Region target, @Nullable Side side) {
			parentView.drawCanvasHint(target, side);
		}

		@Override
		public void clearCanvas() {
			parentView.clearCanvas();
		}

		@Override
		public boolean toggleCollapsed() {
			return parentView.toggleCollapsed();
		}

		@Override
		public boolean isCollapsed() {
			return parentView.isCollapsed();
		}

		@Nonnull
		@Override
		public DockableDestination getComposedDestinationRoot() {
			return parentView;
		}

		@Nonnull
		@Override
		public Region getBackingRegion() {
			return this;
		}

		@Nonnull
		@Override
		public String getIdentifier() {
			return parentView.getIdentifier();
		}

		@Override
		public boolean matchesIdentity(@Nullable Identifiable other) {
			return parentView.matchesIdentity(other);
		}
	}
}
