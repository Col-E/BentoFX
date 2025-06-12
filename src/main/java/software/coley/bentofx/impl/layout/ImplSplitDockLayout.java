package software.coley.bentofx.impl.layout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.css.PseudoClass;
import javafx.css.Selector;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Region;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.RegionBacked;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.impl.space.ImplSingleDockSpace;
import software.coley.bentofx.impl.space.ImplTabbedDockSpace;
import software.coley.bentofx.layout.DockLayout;
import software.coley.bentofx.layout.LeafDockLayout;
import software.coley.bentofx.layout.SplitDockLayout;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.path.LayoutPath;
import software.coley.bentofx.path.PathBuilder;
import software.coley.bentofx.path.SpacePath;
import software.coley.bentofx.space.EmptyDockSpace;
import software.coley.bentofx.space.SingleDockSpace;
import software.coley.bentofx.space.TabbedDockSpace;
import software.coley.bentofx.util.BentoUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("NullableProblems") // Mute the shading of "orientationProperty" not being annotated
public class ImplSplitDockLayout extends SplitPane implements SplitDockLayout {
	public static final PseudoClass PSEUDO_COLLAPSED = PseudoClass.getPseudoClass("collapsed");
	private static final Selector DIVIDER_SELECTOR = Selector.createSelector(".split-pane-divider");
	private final Bento bento;
	private final List<ChildData> children;
	private final String identifier;

	public ImplSplitDockLayout(@Nonnull Bento bento, @Nonnull Orientation orientation, @Nonnull DockLayout... childLayouts) {
		this(bento, orientation, Arrays.asList(childLayouts), BentoUtils.newIdentifier());
	}

	public ImplSplitDockLayout(@Nonnull Bento bento, @Nonnull Orientation orientation, @Nonnull List<DockLayout> childLayouts) {
		this(bento, orientation, childLayouts, BentoUtils.newIdentifier());
	}

	public ImplSplitDockLayout(@Nonnull Bento bento, @Nonnull Orientation orientation, @Nonnull List<DockLayout> childLayouts, @Nonnull String identifier) {
		this.bento = bento;
		this.identifier = identifier;
		this.children = childLayouts.stream()
				.map(ChildData::new)
				.collect(Collectors.toCollection(ArrayList::new));

		BentoUtils.disableWhenNoParent(this);

		getStyleClass().addAll("layout", "layout-split");

		orientationProperty().set(orientation);

		List<Region> layoutRegions = childLayouts.stream()
				.map(RegionBacked::getBackingRegion)
				.toList();
		getItems().addAll(layoutRegions);
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

	@Nonnull
	@Override
	public Region getBackingRegion() {
		return this;
	}

	@Nullable
	@Override
	public LayoutPath findLayout(@Nonnull PathBuilder builder, @Nonnull String id) {
		if (getIdentifier().equals(id))
			return builder.buildLayoutPath();
		for (DockLayout childLayout : getChildLayouts()) {
			LayoutPath path = childLayout.findLayout(builder.inside(childLayout), id);
			if (path != null)
				return path;
		}
		return null;
	}

	@Nullable
	@Override
	public SpacePath findSpace(@Nonnull PathBuilder builder, @Nonnull String id) {
		for (DockLayout childLayout : getChildLayouts()) {
			SpacePath path = childLayout.findSpace(builder.inside(childLayout), id);
			if (path != null)
				return path;
		}
		return null;
	}

	@Nullable
	@Override
	public DockablePath findDockable(@Nonnull PathBuilder builder, @Nonnull String id) {
		for (DockLayout childLayout : getChildLayouts()) {
			DockablePath path = childLayout.findDockable(builder.inside(childLayout), id);
			if (path != null)
				return path;
		}
		return null;
	}

	@Override
	public boolean removeDockable(@Nonnull Dockable dockable) {
		for (DockLayout childLayout : getChildLayouts())
			if (childLayout.removeDockable(dockable))
				return true;
		return false;
	}

	@Override
	public boolean closeDockable(@Nonnull Dockable dockable) {
		for (DockLayout childLayout : getChildLayouts())
			if (childLayout.closeDockable(dockable))
				return true;
		return false;
	}

	@Override
	public void addChildLayout(int index, @Nonnull DockLayout childLayout) {
		if (index >= 0 && index <= children.size()) {
			children.add(index, new ChildData(childLayout));
			getItems().add(index, childLayout.getBackingRegion());
		}
	}

	@Override
	public boolean replaceChildLayout(@Nonnull DockLayout child, @Nonnull DockLayout replacement) {
		if (child == replacement)
			return true;
		int i = indexOfChild(child);
		if (i >= 0 && i < getItems().size()) {
			// Record existing child size.
			Node existing = getItems().get(i);
			double width = existing.getLayoutBounds().getWidth();
			double height = existing.getLayoutBounds().getHeight();
			double size = orientationProperty().get() == Orientation.HORIZONTAL ? width : height;

			// Create replacement child data model, copying values from the existing display.
			ChildData newData = new ChildData(replacement);
			newData.lastWidth = width;
			newData.lastHeight = height;
			newData.showing = true;
			children.set(i, newData);

			// Replace the child in the split-pane.
			getItems().set(i, replacement.getBackingRegion());

			// Ensure the size of the replacement is set to the size of the prior child.
			setChildSize0(replacement, false, size);
			return true;
		}
		return false;
	}

	@Override
	public boolean removeChildLayout(@Nonnull DockLayout child) {
		int i = indexOfChild(child);
		if (i >= 0 && i < getItems().size()) {
			children.remove(i);
			getItems().remove(i);

			// Propagate scene graph simplification upwards.
			if (children.size() == 1) {
				DockLayout parentLayout = getParentLayout();
				if (parentLayout != null)
					parentLayout.replaceChildLayout(this, children.getFirst().layout);
			} else if (children.isEmpty())
				removeFromParent();

			return true;
		}
		return false;
	}

	@Nonnull
	@Override
	public List<DockLayout> getChildLayouts() {
		return children.stream().map(c -> c.layout).toList();
	}

	@Override
	public void setChildSize(@Nonnull DockLayout childLayout, double size) {
		setChildSize0(childLayout, true, size);
	}

	/**
	 * Internal implementation of {@link #setChildSize(DockLayout, double)}.
	 * This version allows skipping updates to {@link ChildData} "last size" caches.
	 * Generally if an end user is making a size update we want to update the cache.
	 * But if we're doing something internal with sizes, like collapsing, then we don't want to do that.
	 *
	 * @param childLayout
	 * 		Child layout to change size of.
	 * @param updateLastSizes
	 *        {@code true} to update {@link ChildData#lastWidth} and {@link ChildData#lastHeight}
	 * 		associated with the child layout.
	 * @param size
	 * 		New size <i>(In pixels)</i> to grant the given child in the containing {@link SplitPane}.
	 */
	private void setChildSize0(@Nonnull DockLayout childLayout, boolean updateLastSizes, double size) {
		ChildData data = getChildData(childLayout);
		if (data == null)
			return;

		// Delay action if the child is not showing.
		if (!data.showing) {
			data.addAction(() -> setChildSize0(childLayout, updateLastSizes, size));
			return;
		}

		// If collapsed, update the child's last size.
		// It will be this new size when it gets uncollapsed.
		if (updateLastSizes && isChildCollapsed(childLayout)) {
			Orientation orientation = orientationProperty().get();
			if (orientation == Orientation.HORIZONTAL)
				data.lastWidth = size;
			else
				data.lastHeight = size;
			return;
		}

		double max = orientationProperty().get() == Orientation.HORIZONTAL ? getWidth() : getHeight();
		double ratio = Math.clamp(size / max, 0, 1);

		int i = indexOfChild(childLayout);
		if (i == 0 && children.size() > 1) {
			// Child is first, move the first divider if one exists
			setDividerPosition(0, ratio);
		} else if (i > 0 && i == children.size() - 1) {
			// Child is last, move the last divider if one exists
			setDividerPosition(i - 1, 1 - ratio);
		}
	}

	@Override
	public void setChildPercent(@Nonnull DockLayout childLayout, double percent) {
		ChildData data = getChildData(childLayout);
		if (data == null)
			return;

		// Delay action if the child is not showing.
		if (!data.showing) {
			data.addAction(() -> setChildPercent(childLayout, percent));
			return;
		}

		// If collapsed, update the child's last size.
		// It will be this new size when it gets uncollapsed.
		if (isChildCollapsed(childLayout)) {
			Orientation orientation = orientationProperty().get();
			if (orientation == Orientation.HORIZONTAL)
				data.lastWidth = getWidth() * percent;
			else
				data.lastHeight = getHeight() * percent;
			return;
		}

		int i = indexOfChild(childLayout);
		if (i == 0 && children.size() > 1) {
			// Child is first, move the first divider if one exists
			setDividerPosition(0, percent);
		} else if (i > 0 && i == children.size() - 1) {
			// Child is last, move the last divider if one exists
			setDividerPosition(i - 1, 1 - percent);
		}
	}

	@Override
	public void setChildResizable(@Nonnull DockLayout childLayout, boolean resizable) {
		// Get our direct children that are dividers.
		List<Node> dividers = getChildren().stream().filter(DIVIDER_SELECTOR::applies).toList();
		if (dividers.isEmpty())
			return;

		// Get the divider to modify.
		Node divider;
		int i = indexOfChild(childLayout);
		if (i == 0 && children.size() > 1) {
			// Child is first, get the first divider if one exists.
			divider = dividers.getFirst();
		} else if (i > 0 && i == children.size() - 1) {
			// Child is last, get the last divider if one exists.
			divider = dividers.getLast();
		} else {
			// Not supported.
			return;
		}

		// Disable/enable the divider
		divider.setDisable(!resizable);
	}

	@Override
	public boolean isChildResizable(@Nonnull DockLayout childLayout) {
		List<Node> dividers = BentoUtils.getChildren(this, ".split-pane-divider");
		if (dividers.isEmpty())
			return true;

		// Get the divider to check.
		Node divider;
		int i = indexOfChild(childLayout);
		if (i == 0 && children.size() > 1) {
			// Child is first, get the first divider if one exists.
			divider = dividers.getFirst();
		} else if (i > 0 && i == children.size() - 1) {
			// Child is last, get the last divider if one exists.
			divider = dividers.getLast();
		} else {
			// Not supported.
			return true;
		}

		// Check the divider state.
		return !divider.isDisable();
	}

	@Override
	public boolean setChildCollapsed(@Nonnull DockLayout childLayout, boolean collapsed) {
		ChildData data = getChildData(childLayout);
		if (data == null)
			return false;

		// Delay action if the child is not showing.
		if (!data.showing) {
			data.addAction(() -> setChildCollapsed(childLayout, collapsed));
			return false;
		}

		// Only edge children are collapsible.
		if (children.getFirst() != data && children.getLast() != data)
			return false;

		// Skip if the child is already in the given collapsed state.
		if (isChildCollapsed(childLayout) == collapsed)
			return true;

		// Get the appropriate "side" the child holds for its contents.
		// We essentially just want to get where the "Header" children in the child are positioned.
		Side childSide = switch (childLayout) {
			case LeafDockLayout leaf -> switch (leaf.getSpace()) {
				case EmptyDockSpace ignored -> null;
				case SingleDockSpace single -> single.headerSideProperty().get();
				case TabbedDockSpace tabbed -> tabbed.sideProperty().get();
			};
			case SplitDockLayout ignored -> null;
		};

		// Skip if the child layout doesn't have any computable side for its headers (or lack thereof).
		if (childSide == null)
			return false;

		// Skip if this split-pane orientation is not compatible with the tabbed layout side.
		Orientation orientation = orientationProperty().get();
		if (orientation == Orientation.HORIZONTAL && (childSide == Side.TOP || childSide == Side.BOTTOM))
			return false;
		if (orientation == Orientation.VERTICAL && (childSide == Side.LEFT || childSide == Side.RIGHT))
			return false;

		// Delegate collapse handling to parents.
		if (!collapsed && isChildCollapsed(childLayout)) {
			// Update split-pane
			double newSize = orientation == Orientation.HORIZONTAL ?
					data.lastWidth :
					data.lastHeight;
			setChildSize0(childLayout, false, newSize);
			setChildResizable(childLayout, true);
			data.setCollapsed(false);
		} else if (collapsed) {
			// Skip if this would steal the divider away from some adjacent collapsed destination space.
			int i = indexOfChild(childLayout);
			if (i > 0 && children.get(i - 1).collapsed)
				return false;
			if (i <= children.size() - 2 && children.get(i + 1).collapsed)
				return false;

			// Remove/collapse space if possible.
			LeafDockLayout leaf = (LeafDockLayout) childLayout;
			if (leaf.getSpace() instanceof ImplTabbedDockSpace tabbedChild) {
				// We need a dockable in the tabbed child in order to look up the 'Header' component.
				// That header will be used for size calculations below.
				if (tabbedChild.getDockables().isEmpty())
					return false;
				Dockable dockable = tabbedChild.getDockables().getFirst();

				// Record the existing width/height of the current space.
				data.lastWidth = tabbedChild.getWidth();
				data.lastHeight = tabbedChild.getHeight();

				// Look up the header to compute the width or height we need to collapse into.
				// We only want to show the header, so we will match the width/height depending on our orientation.
				Header childHeader = tabbedChild.getHeader(dockable);
				if (childHeader == null)
					return false;

				// Update split-pane, make sure not to update the 'child-data' last size cache.
				// We want that cache to hold the last 'open' size which
				double newSize = orientation == Orientation.HORIZONTAL ?
						childHeader.getWidth() :
						childHeader.getHeight();
				setChildSize0(childLayout, false, newSize);
				setChildResizable(childLayout, false);

				// Mark this child as collapsed. We need to do this before we clear the selection
				// so that any state change in the child as a result sees "oh we are collapsed now".
				data.setCollapsed(true);

				// Deselect any dockable since we are now collapsed.
				tabbedChild.selectDockable(null);
				return true;
			} else if (leaf.getSpace() instanceof ImplSingleDockSpace singleChild) {
				Header childHeader = singleChild.getHeader();
				if (childHeader == null)
					return false;

				// Update parent split-pane
				double newSize = orientation == Orientation.HORIZONTAL ?
						childHeader.getWidth() :
						childHeader.getHeight();
				setChildSize0(childLayout, false, newSize);
				setChildResizable(childLayout, false);
				data.setCollapsed(true);
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isChildCollapsed(@Nonnull DockLayout childLayout) {
		for (ChildData child : children)
			if (child.layout == childLayout)
				return child.collapsed;
		return false;
	}

	@Nonnull
	@Override
	public Bento getBento() {
		return bento;
	}

	@Override
	protected void layoutChildren() {
		super.layoutChildren();

		// Handle queued actions after children get their layouts updated.
		// This allows things like toggling to work off of items with sizes computed
		// even when called before the sizes are set by the first layout pass.
		children.forEach(c -> {
			c.showing = true;
			c.runActions();
		});
	}

	private int indexOfChild(@Nonnull DockLayout childLayout) {
		for (int i = 0; i < children.size(); i++)
			if (children.get(i).layout == childLayout)
				return i;
		return -1;
	}

	@Nullable
	private ChildData getChildData(@Nonnull DockLayout childLayout) {
		for (ChildData child : children)
			if (child.layout == childLayout)
				return child;
		return null;
	}

	@Override
	public String toString() {
		return "Split: " + getIdentifier();
	}

	/**
	 * Mutable data holder for some given {@link DockLayout} child.
	 */
	private static class ChildData {
		private final DockLayout layout;
		private List<Runnable> actionQueue;
		private double lastWidth;
		private double lastHeight;
		private boolean collapsed;
		private boolean showing;

		private ChildData(@Nonnull DockLayout layout) {
			this.layout = layout;
		}

		/**
		 * @param r
		 * 		Action to run later.
		 */
		private void addAction(@Nonnull Runnable r) {
			if (actionQueue == null)
				actionQueue = new ArrayList<>();
			actionQueue.add(r);

			// Schedule a layout.
			// Our actions will run after the next layout pass.
			layout.getBackingRegion().requestLayout();
		}

		/**
		 * Run queued actions.
		 */
		private void runActions() {
			if (actionQueue != null) {
				actionQueue.forEach(Runnable::run);
				actionQueue.clear();
			}
		}

		/**
		 * @param state
		 * 		New collapsed state.
		 */
		public void setCollapsed(boolean state) {
			collapsed = state;
			layout.getBackingRegion().pseudoClassStateChanged(PSEUDO_COLLAPSED, state);
		}
	}
}
