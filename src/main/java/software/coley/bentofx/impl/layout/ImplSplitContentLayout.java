package software.coley.bentofx.impl.layout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.css.Selector;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Region;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Dockable;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.RegionBacked;
import software.coley.bentofx.content.EmptyContent;
import software.coley.bentofx.content.SingleContent;
import software.coley.bentofx.content.TabbedContent;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.impl.content.ImplSingleContent;
import software.coley.bentofx.impl.content.ImplTabbedContent;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.LeafContentLayout;
import software.coley.bentofx.layout.SplitContentLayout;
import software.coley.bentofx.path.ContentPath;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.path.LayoutPath;
import software.coley.bentofx.path.PathBuilder;
import software.coley.bentofx.util.BentoUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("NullableProblems") // Mute the shading of "orientationProperty" not being annotated
public class ImplSplitContentLayout extends SplitPane implements SplitContentLayout {
	private static final Selector DIVIDER_SELECTOR = Selector.createSelector(".split-pane-divider");
	private final Bento bento;
	private final List<ChildData> children;
	private final String identifier;

	public ImplSplitContentLayout(@Nonnull Bento bento, @Nonnull Orientation orientation, @Nonnull ContentLayout... childLayouts) {
		this(bento, orientation, Arrays.asList(childLayouts), BentoUtils.newIdentifier());
	}

	public ImplSplitContentLayout(@Nonnull Bento bento, @Nonnull Orientation orientation, @Nonnull List<ContentLayout> childLayouts) {
		this(bento, orientation, childLayouts, BentoUtils.newIdentifier());
	}

	public ImplSplitContentLayout(@Nonnull Bento bento, @Nonnull Orientation orientation, @Nonnull List<ContentLayout> childLayouts, @Nonnull String identifier) {
		this.bento = bento;
		this.identifier = identifier;
		this.children = childLayouts.stream()
				.map(ChildData::new)
				.collect(Collectors.toCollection(ArrayList::new));

		BentoUtils.disableWhenNoParent(this);

		orientationProperty().set(orientation);

		List<Region> contentLayoutRegions = childLayouts.stream()
				.map(RegionBacked::getBackingRegion)
				.toList();
		getItems().addAll(contentLayoutRegions);
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
		for (ContentLayout childLayout : getChildLayouts()) {
			LayoutPath path = childLayout.findLayout(builder.inside(childLayout), id);
			if (path != null)
				return path;
		}
		return null;
	}

	@Nullable
	@Override
	public ContentPath findContent(@Nonnull PathBuilder builder, @Nonnull String id) {
		for (ContentLayout childLayout : getChildLayouts()) {
			ContentPath path = childLayout.findContent(builder.inside(childLayout), id);
			if (path != null)
				return path;
		}
		return null;
	}

	@Nullable
	@Override
	public DockablePath findDockable(@Nonnull PathBuilder builder, @Nonnull String id) {
		for (ContentLayout childLayout : getChildLayouts()) {
			DockablePath path = childLayout.findDockable(builder.inside(childLayout), id);
			if (path != null)
				return path;
		}
		return null;
	}

	@Override
	public boolean removeDockable(@Nonnull Dockable dockable) {
		for (ContentLayout childLayout : getChildLayouts())
			if (childLayout.removeDockable(dockable))
				return true;
		return false;
	}

	@Override
	public boolean replaceChildLayout(@Nonnull ContentLayout child, @Nonnull ContentLayout replacement) {
		int i = indexOfChild(child);
		if (i >= 0 && i < getItems().size()) {
			children.set(i, new ChildData(replacement));
			getItems().set(i, replacement.getBackingRegion());
			return true;
		}
		return false;
	}

	@Override
	public boolean removeChildLayout(@Nonnull ContentLayout child) {
		int i = indexOfChild(child);
		if (i >= 0 && i < getItems().size()) {
			children.remove(i);
			getItems().remove(i);

			// Propagate scene graph simplification upwards.
			if (children.size() == 1) {
				ContentLayout parentLayout = getParentLayout();
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
	public List<ContentLayout> getChildLayouts() {
		return children.stream().map(c -> c.layout).toList();
	}

	@Override
	public void setChildSize(@Nonnull ContentLayout childLayout, double size) {
		BentoUtils.scheduleWhenShown(this, split -> {
			int i = indexOfChild(childLayout);
			double max = orientationProperty().get() == Orientation.HORIZONTAL ? getWidth() : getHeight();

			double ratio = Math.clamp(size / max, 0, 1);

			if (i == 0 && children.size() > 1) {
				// Child is first, move the first divider if one exists
				setDividerPosition(0, ratio);
			} else if (i > 0 && i == children.size() - 1) {
				// Child is last, move the last divider if one exists
				setDividerPosition(i - 1, 1 - ratio);
			}
		});
	}

	@Override
	public void setChildPercent(@Nonnull ContentLayout childLayout, double percent) {
		BentoUtils.scheduleWhenShown(this, split -> {
			int i = indexOfChild(childLayout);

			if (i == 0 && children.size() > 1) {
				// Child is first, move the first divider if one exists
				setDividerPosition(0, percent);
			} else if (i > 0 && i == children.size() - 1) {
				// Child is last, move the last divider if one exists
				setDividerPosition(i - 1, 1 - percent);
			}
		});
	}

	@Override
	public void setChildResizable(@Nonnull ContentLayout childLayout, boolean resizable) {
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
	public boolean isChildResizable(@Nonnull ContentLayout childLayout) {
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
	public boolean setChildCollapsed(@Nonnull ContentLayout childLayout, boolean collapsed) {
		ChildData data = getChildData(childLayout);
		if (data == null)
			return false;

		// Only edge children are collapsible.
		if (children.getFirst() != data && children.getLast() != data)
			return false;

		// Skip if the child is already in the given collapsed state.
		if (isChildCollapsed(childLayout) == collapsed)
			return true;

		// Get the appropriate "side" the child holds for its contents.
		// We essentially just want to get where the "Header" children in the child are positioned.
		Side childSide = switch (childLayout) {
			case LeafContentLayout leafContentLayout -> switch (leafContentLayout.getContent()) {
				case EmptyContent ignored -> null;
				case SingleContent singleContent -> singleContent.headerSideProperty().get();
				case TabbedContent tabbedContent -> tabbedContent.sideProperty().get();
			};
			case SplitContentLayout ignored -> null;
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
			setChildSize(childLayout, newSize);
			setChildResizable(childLayout, true);
			data.collapsed = false;
		} else if (collapsed) {
			// Skip if this would steal the divider away from some adjacent collapsed destination space.
			int i = indexOfChild(childLayout);
			if (i > 0 && children.get(i - 1).collapsed)
				return false;
			if (i <= children.size() - 2 && children.get(i + 1).collapsed)
				return false;

			// Remove/collapse content if possible.
			LeafContentLayout leaf = (LeafContentLayout) childLayout;
			if (leaf.getContent() instanceof ImplTabbedContent tabbedChild) {
				Dockable dockable = tabbedChild.selectedDockableProperty().get();
				if (dockable == null)
					return false;

				// Record the existing width/height of the current selected item.
				data.lastWidth = tabbedChild.getWidth();
				data.lastHeight = tabbedChild.getHeight();

				// Need to look up the header to compute the width or height we need to
				Header childHeader = tabbedChild.getHeader(dockable);
				if (childHeader == null)
					return false;

				// Update parent split-pane
				double newSize = orientation == Orientation.HORIZONTAL ?
						childHeader.getWidth() :
						childHeader.getHeight();
				setChildSize(childLayout, newSize);
				setChildResizable(childLayout, false);

				// Deselect any dockable since we are now collapsed
				tabbedChild.selectDockable(null);
				data.collapsed = true;
				return true;
			} else if (leaf.getContent() instanceof ImplSingleContent singleChild) {
				Header childHeader = singleChild.getHeader();
				if (childHeader == null)
					return false;

				// Update parent split-pane
				double newSize = orientation == Orientation.HORIZONTAL ?
						childHeader.getWidth() :
						childHeader.getHeight();
				setChildSize(childLayout, newSize);
				setChildResizable(childLayout, false);
				data.collapsed = true;
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isChildCollapsed(@Nonnull ContentLayout childLayout) {
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

	private int indexOfChild(@Nonnull ContentLayout childLayout) {
		for (int i = 0; i < children.size(); i++)
			if (children.get(i).layout == childLayout)
				return i;
		return -1;
	}

	@Nullable
	private ChildData getChildData(@Nonnull ContentLayout childLayout) {
		for (ChildData child : children)
			if (child.layout == childLayout)
				return child;
		return null;
	}

	/**
	 * Mutable data holder for some given {@link ContentLayout} child.
	 */
	private static class ChildData {
		private final ContentLayout layout;
		private double lastWidth;
		private double lastHeight;
		private boolean collapsed;

		private ChildData(@Nonnull ContentLayout layout) {
			this.layout = layout;
		}
	}
}
