package software.coley.bentofx.header;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Dockable;
import software.coley.bentofx.DockableDestination;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.content.TabbedContent;
import software.coley.bentofx.impl.content.ImplTabbedContent;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.RootContentLayout;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.util.BentoUtils;
import software.coley.bentofx.util.DropTargetType;
import software.coley.bentofx.util.LinearItemPane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HeaderRegion extends StackPane implements DockableDestination {
	private final ObjectProperty<Dockable> selectedProperty = new SimpleObjectProperty<>();
	private final Bento bento;
	private final HeaderView parentView;
	private final LinearItemPane itemPane;
	private final Side side;

	public HeaderRegion(@Nonnull Bento bento, @Nonnull HeaderView parentView, @Nonnull Side side) {
		this.bento = bento;
		this.parentView = parentView;
		this.side = side;

		getStyleClass().add("dock-header-box");
		switch (side) {
			case TOP -> pseudoClassStateChanged(Header.PSEUDO_SIDE_TOP, true);
			case BOTTOM -> pseudoClassStateChanged(Header.PSEUDO_SIDE_BOTTOM, true);
			case LEFT -> pseudoClassStateChanged(Header.PSEUDO_SIDE_LEFT, true);
			case RIGHT -> pseudoClassStateChanged(Header.PSEUDO_SIDE_RIGHT, true);
		}

		// Determine region orientation from target side
		Orientation orientation = BentoUtils.sideToOrientation(side);
		itemPane = new LinearItemPane(orientation);

		// Update selection pseudo-state
		selectedProperty.addListener((ob, old, cur) -> {
			// Deselect prior header
			if (old != null) {
				Header header = getHeader(old);
				if (header != null)
					header.setSelected(false);
			}

			// Select new header
			if (cur != null) {
				Header header = getHeader(cur);
				if (header != null)
					header.setSelected(true);
			}
		});

		// Layout
		getChildren().add(itemPane);
		layoutOrientation(orientation);

		// Drag support
		BentoUtils.setupCommonDragSupport(this, false);
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
			target.receiveDroppedHeader(e, null, headerSource);
		});

		// Auto-close
		itemPane.getChildren().addListener((ListChangeListener<Node>) c -> {
			if (itemPane.getChildren().isEmpty()) {
				// Skip if auto-prune is disabled
				if (getParentContent() instanceof TabbedContent tabbedParent && !tabbedParent.autoPruneWhenEmptyProperty().get())
					return;

				// Prune from layout
				ContentLayout contentParent = getParentLayout();
				if (contentParent != null)
					// We put this in a "runLater" because otherwise dragging a header from a region with only one item
					// would instantly prune it, preventing the eventual drop handler from showing the header in the
					// new content-layout space.
					Platform.runLater(contentParent::removeFromParent);
			}
		});
	}

	private void layoutOrientation(@Nonnull Orientation orientation) {
		if (orientation == Orientation.HORIZONTAL) {
			itemPane.prefHeightProperty().unbind();
			itemPane.prefWidthProperty().bind(widthProperty());
		} else {
			itemPane.prefWidthProperty().unbind();
			itemPane.prefHeightProperty().bind(heightProperty());
		}
	}

	@Nonnull
	public ObjectProperty<Dockable> selectedProperty() {
		return selectedProperty;
	}

	@Nullable
	public Header getHeader(@Nonnull Dockable dockable) {
		for (Node child : itemPane.getChildren())
			if (child instanceof Header header && header.getDockable() == dockable)
				return header;
		return null;
	}

	@Nullable
	@Override
	public Content getParentContent() {
		return BentoUtils.getParent(this, Content.class);
	}

	@Nullable
	@Override
	public ContentLayout getParentLayout() {
		return BentoUtils.getParent(this, ContentLayout.class);
	}

	@Nonnull
	public List<Dockable> getDockables() {
		ObservableList<Node> children = itemPane.getChildren();
		List<Dockable> list = new ArrayList<>(children.size());
		for (Node child : children)
			if (child instanceof Header header)
				list.add(header.getDockable());
		return Collections.unmodifiableList(list);
	}

	@Nullable
	@Override
	public Dockable getSelectedDockable() {
		return selectedProperty.get();
	}

	@Override
	public boolean canReceiveDragGroup(int dragGroup) {
		return canReceiveDragGroup(getDockables(), dragGroup);
	}

	@Override
	public boolean canSplit() {
		return getParentContent() instanceof TabbedContent parentTabbed
				&& parentTabbed.canSplitProperty().get();
	}

	@Override
	public boolean canReceiveHeader(@Nullable Side droppedSide, @Nonnull Header source) {
		// If the side is null, we're adding it without doing any splitting.
		boolean isSplitting = droppedSide != null;

		// If splitting isn't supported, we can only receive the header if there is no associated side.
		if (isSplitting && !canSplit())
			return false;

		// If our identity matches the source's existing parent then we are the source's parent.
		// Since that means there would be no effective change, we'll just abort right here.
		if (!isSplitting && matchesIdentity(source.getParentDestination()))
			return false;

		// We will only receive the dockable if we don't already own it, unless we're doing a split.
		// Unless of course the split would make our dockables collection empty.
		List<Dockable> ourDockables = getDockables();
		Dockable dockable = source.getDockable();
		if (!isSplitting && ourDockables.contains(dockable))
			return false;
		if (Collections.singletonList(dockable).equals(ourDockables))
			return false;

		// If we already have dockables in this space, we can only add new ones
		// if they belong to the same drag group.
		return canReceiveDragGroup(ourDockables, dockable.getDragGroup());
	}

	@Override
	public boolean receiveDroppedHeader(@Nonnull DragEvent event, @Nullable Side droppedSide, @Nonnull Header source) {
		if (!canReceiveHeader(droppedSide, source))
			return false;

		// Handle splitting by side.
		Dockable dockable = source.getDockable();
		if (droppedSide == null) {
			// Must be able to remove the source header from its parent.
			if (source.removeFromParent(Header.RemovalReason.MOVING)) {
				BentoUtils.completeDnd(event, dockable, DropTargetType.REGION);
				addDockable(dockable);
				selectDockable(dockable);
				return true;
			}
		} else {
			// Must be able to remove the source header from its parent.
			ContentLayout parentLayout = getParentLayout();
			if (parentLayout != null && source.removeFromParent(Header.RemovalReason.MOVING)) {
				// Create a new tabbed region. The side will be the same side as ours if it will result
				// in a visually consistent header-region. IE, a vertical split with two LEFT/RIGHT sides.
				// However, if the split in that case would be horizontal then you'd have a disjoint header-region.
				// In this case we will default to using the TOP side.
				//
				// The split will always be across the orientation of the dropped side.
				Side newContentSide = side;
				if (newContentSide.isVertical() == droppedSide.isVertical())
					newContentSide = Side.TOP;
				ImplTabbedContent newContent = new ImplTabbedContent(parentLayout.getBento(), newContentSide, List.of(dockable));
				ContentLayout newLayout = parentLayout.asSplitWith(newContent, droppedSide);

				// We want to put the new split layout where our current parent layout is.
				ContentLayout parentParentLayout = parentLayout.getParentLayout();
				if (parentParentLayout != null) {
					// We just tell the parent of our parent to replace it.
					BentoUtils.completeDnd(event, dockable, DropTargetType.REGION);
					parentParentLayout.replaceChildLayout(parentLayout, newLayout);
					return true;
				} else {
					// We have no parent-of-parent, so our parent must belong to the root.
					// Have the root replace its layout.
					RootContentLayout rootLayout = parentLayout.getRootLayout();
					if (rootLayout != null) {
						BentoUtils.completeDnd(event, dockable, DropTargetType.REGION);
						rootLayout.setLayout(newLayout);
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean addDockable(@Nonnull Dockable dockable) {
		return addDockable(getDockables().size(), dockable);
	}

	@Override
	public boolean addDockable(int index, @Nonnull Dockable dockable) {
		if (getHeader(dockable) == null) {
			Header header = new Header(bento, dockable, side);
			itemPane.getChildren().add(index, header);
			itemPane.requestLayout();

			// Handle selection.
			Dockable selectedDockable = selectedProperty.get();
			if (selectedDockable == null)
				// If this is the first tab being added, select it.
				selectedProperty.set(dockable);
			else if (selectedDockable == dockable)
				// If the newly created header wraps the current selected tab, mark it as selected.
				header.setSelected(true);

			return true;
		}
		return false;
	}

	@Override
	public boolean removeDockable(@Nonnull Dockable dockable) {
		ObservableList<Node> children = itemPane.getChildren();
		List<Dockable> dockables = getDockables();
		int index = 0;
		for (Node child : children) {
			if (child instanceof Header header) {
				if (header.getDockable() == dockable) {
					children.remove(header);

					// Update selection to be next available dockable item
					int nextSelectionIndex = Math.min(index, children.size() - 1);
					if (nextSelectionIndex >= 0)
						parentView.selectDockable(dockables.get(nextSelectionIndex));
					else
						selectedProperty.set(null);
					return true;
				}
				index++;
			}
		}
		return false;
	}

	@Override
	public boolean selectDockable(@Nonnull Dockable dockable) {
		if (getDockables().contains(dockable)) {
			selectedProperty.set(dockable);

			// If we were collapsed, ensure we restore the previous expected
			// dimensions of this content-layout now that we're showing
			// some dockable content.
			if (isCollapsed())
				toggleCollapsed();

			return true;
		}
		return false;
	}

	@Override
	public void drawCanvasHint(@Nonnull Region target, @Nullable Side side) {
		DockableDestination parentDestination = BentoUtils.getOrParent(getParent(), DockableDestination.class);
		if (parentDestination != null)
			parentDestination.drawCanvasHint(target, side);
	}

	@Override
	public void clearCanvas() {
		DockableDestination parentDestination = BentoUtils.getOrParent(getParent(), DockableDestination.class);
		if (parentDestination != null)
			parentDestination.clearCanvas();
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

	private static boolean canReceiveDragGroup(@Nonnull List<Dockable> dockables, int dragGroup) {
		return dockables.isEmpty()
				|| dockables.stream().anyMatch(d -> d.getDragGroup() == dragGroup);
	}
}
