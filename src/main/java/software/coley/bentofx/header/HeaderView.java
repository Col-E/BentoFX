package software.coley.bentofx.header;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Dockable;
import software.coley.bentofx.DockableDestination;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.SplitContentLayout;
import software.coley.bentofx.util.BentoUtils;

import java.util.List;

public class HeaderView extends StackPane implements DockableDestination {
	public static final PseudoClass PSEUDO_ACTIVE = PseudoClass.getPseudoClass("active");
	private final Bento bento;
	private final String identifier = BentoUtils.newIdentifier();
	private final ContentWrapper contentWrapper;
	private final HeaderRegion headerRegion;
	private final Canvas canvas = new Canvas();
	private final Side side;
	private double lastWidth;
	private double lastHeight;
	private boolean collapsed;

	public HeaderView(@Nonnull Bento bento, @Nonnull Side side) {
		this.bento = bento;
		this.side = side;

		headerRegion = new HeaderRegion(bento, this, side);
		contentWrapper = new ContentWrapper(bento, this);

		getStyleClass().add("dock-container");

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
		layoutWrapper.setCenter(contentWrapper);
		switch (side) {
			case TOP -> layoutWrapper.setTop(headerRegion);
			case BOTTOM -> layoutWrapper.setBottom(headerRegion);
			case LEFT -> layoutWrapper.setLeft(headerRegion);
			case RIGHT -> layoutWrapper.setRight(headerRegion);
		}

		// Put canvas on top
		getChildren().addAll(layoutWrapper, canvas);
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
	public boolean selectDockable(@Nonnull Dockable dockable) {
		return headerRegion.selectDockable(dockable);
	}

	@Nullable
	@Override
	public Content getParentContent() {
		return BentoUtils.getOrParent(getParent(), Content.class);
	}

	@Nullable
	@Override
	public ContentLayout getParentLayout() {
		return BentoUtils.getOrParent(getParent(), ContentLayout.class);
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
	public void toggleCollapsed() {
		SplitContentLayout parentSplitLayout = BentoUtils.getOrParent(this, SplitContentLayout.class);
		ContentLayout parentLayout = BentoUtils.getOrParent(this, ContentLayout.class);

		// Skip if there is no parent split or parent layout.
		if (parentSplitLayout == null || parentLayout == null)
			return;

		// Skip if the split-pane orientation is not compatible with the tabbed layout side.
		Orientation orientation = parentSplitLayout.orientationProperty().get();
		if (orientation == Orientation.HORIZONTAL && (side == Side.TOP || side == Side.BOTTOM))
			return;
		if (orientation == Orientation.VERTICAL && (side == Side.LEFT || side == Side.RIGHT))
			return;

		// TODO: If you have a horizontal split with tabs on the window edges and collapse both
		//  this breaks.

		// Delegate collapse handling to parents.
		if (isCollapsed()) {
			// Uncollapse if possible
			Dockable selected = headerRegion.getSelectedDockable();
			if (selected != null) {
				// Restore content
				contentWrapper.setCenter(selected.getNode());

				// Update parent split-pane
				double newSize = orientation == Orientation.HORIZONTAL ?
						lastWidth :
						lastHeight;
				parentSplitLayout.setChildSize(parentLayout, newSize);
				parentSplitLayout.setChildResizable(parentLayout, true);

				// Unmark
				collapsed = false;
			}
		} else {
			// Skip if this would steal the divider away from some adjacent collapsed destination space
			List<Node> destinations = BentoUtils.getChildren(parentSplitLayout.getBackingRegion(), DockableDestination.class);
			int i = destinations.indexOf(this);
			if (i > 0 && ((DockableDestination) destinations.get(i - 1)).isCollapsed())
				return;
			if (i <= destinations.size() - 2 && ((DockableDestination) destinations.get(i + 1)).isCollapsed())
				return;

			// Remove content
			lastWidth = getWidth();
			lastHeight = getHeight();
			contentWrapper.setCenter(null);

			// Update parent split-pane
			double newSize = orientation == Orientation.HORIZONTAL ?
					headerRegion.getWidth() :
					headerRegion.getHeight();
			parentSplitLayout.setChildSize(parentLayout, newSize);
			parentSplitLayout.setChildResizable(parentLayout, false);

			// Deselect any dockable since we are now collapsed
			headerRegion.selectedProperty().set(null);

			// Mark
			collapsed = true;
		}
	}

	@Override
	public boolean isCollapsed() {
		return collapsed;
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

			// Always show selected content
			parentView.headerRegion.selectedProperty().addListener((ob, old, cur) -> {
				if (cur != null) {
					Node node = cur.getNode();
					setCenter(node);
					if (node.isFocusTraversable())
						node.requestFocus();
				} else {
					ContentLayout parentLayout = getParentLayout();
					if (parentLayout != null) {
						// Replace the content with the "empty-content" template.
						Content replacementContent = parentView.bento.newEmptyContent(parentLayout);
						setCenter(replacementContent.getBackingRegion());
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
				// Ensure the origin is a header, and the target is a content-region.
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
		public Content getParentContent() {
			return parentView.getParentContent();
		}

		@Nullable
		@Override
		public ContentLayout getParentLayout() {
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
		public void toggleCollapsed() {
			parentView.toggleCollapsed();
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
