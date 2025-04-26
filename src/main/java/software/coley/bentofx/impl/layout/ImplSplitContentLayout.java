package software.coley.bentofx.impl.layout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.css.Selector;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Region;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Dockable;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.RegionBacked;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.SplitContentLayout;
import software.coley.bentofx.path.ContentPath;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.path.LayoutPath;
import software.coley.bentofx.path.PathBuilder;
import software.coley.bentofx.util.BentoUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("NullableProblems") // Mute the shading of "orientationProperty" not being annotated
public class ImplSplitContentLayout extends SplitPane implements SplitContentLayout {
	private static final Selector DIVIDER_SELECTOR = Selector.createSelector(".split-pane-divider");
	private final Bento bento;
	private final List<ContentLayout> childLayouts;
	private final String identifier;

	public ImplSplitContentLayout(@Nonnull Bento bento, @Nonnull Orientation orientation, @Nonnull ContentLayout... childLayouts) {
		this(bento, orientation, Arrays.asList(childLayouts), BentoUtils.newIdentifier());
	}

	public ImplSplitContentLayout(@Nonnull Bento bento, @Nonnull Orientation orientation, @Nonnull List<ContentLayout> childLayouts) {
		this(bento, orientation, childLayouts, BentoUtils.newIdentifier());
	}

	public ImplSplitContentLayout(@Nonnull Bento bento, @Nonnull Orientation orientation, @Nonnull List<ContentLayout> childLayouts, @Nonnull String identifier) {
		this.bento = bento;
		this.childLayouts = new ArrayList<>(childLayouts);
		this.identifier = identifier;

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
		for (ContentLayout childLayout : childLayouts) {
			LayoutPath path = childLayout.findLayout(builder.inside(childLayout), id);
			if (path != null)
				return path;
		}
		return null;
	}

	@Nullable
	@Override
	public ContentPath findContent(@Nonnull PathBuilder builder, @Nonnull String id) {
		for (ContentLayout childLayout : childLayouts) {
			ContentPath path = childLayout.findContent(builder.inside(childLayout), id);
			if (path != null)
				return path;
		}
		return null;
	}

	@Nullable
	@Override
	public DockablePath findDockable(@Nonnull PathBuilder builder, @Nonnull String id) {
		for (ContentLayout childLayout : childLayouts) {
			DockablePath path = childLayout.findDockable(builder.inside(childLayout), id);
			if (path != null)
				return path;
		}
		return null;
	}

	@Override
	public boolean removeDockable(@Nonnull Dockable dockable) {
		for (ContentLayout childLayout : childLayouts)
			if (childLayout.removeDockable(dockable))
				return true;
		return false;
	}

	@Override
	public boolean replaceChildLayout(@Nonnull ContentLayout child, @Nonnull ContentLayout replacement) {
		int i = childLayouts.indexOf(child);
		if (i >= 0 && i < getItems().size()) {
			childLayouts.set(i, replacement);
			getItems().set(i, replacement.getBackingRegion());
			return true;
		}
		return false;
	}

	@Override
	public boolean removeChildLayout(@Nonnull ContentLayout child) {
		int i = childLayouts.indexOf(child);
		if (i >= 0 && i < getItems().size()) {
			childLayouts.remove(i);
			getItems().remove(i);

			// Propagate scene graph simplification upwards.
			if (childLayouts.size() == 1) {
				ContentLayout parentLayout = getParentLayout();
				if (parentLayout != null)
					parentLayout.replaceChildLayout(this, childLayouts.getFirst());
			} else if (childLayouts.isEmpty())
				removeFromParent();

			return true;
		}
		return false;
	}

	@Nonnull
	@Override
	public List<ContentLayout> getChildLayouts() {
		return childLayouts;
	}

	@Override
	public void setChildSize(@Nonnull ContentLayout childLayout, double size) {
		int i = childLayouts.indexOf(childLayout);
		double max = orientationProperty().get() == Orientation.HORIZONTAL ? getWidth() : getHeight();

		double ratio = size / max;
		if (i == 0 && childLayouts.size() > 1) {
			// Child is first, move the first divider if one exists
			setDividerPosition(0, ratio);
		} else if (i > 0 && i == childLayouts.size() - 1) {
			// Child is last, move the last divider if one exists
			setDividerPosition(i - 1, 1 - ratio);
		}
	}

	@Override
	public void setChildResizable(@Nonnull ContentLayout childLayout, boolean resizable) {
		// Get our direct children that are dividers.
		List<Node> dividers = getChildren().stream().filter(DIVIDER_SELECTOR::applies).toList();
		if (dividers.isEmpty())
			return;

		// Get the divider to modify.
		Node divider;
		int i = childLayouts.indexOf(childLayout);
		if (i == 0 && childLayouts.size() > 1) {
			// Child is first, get the first divider if one exists.
			divider = dividers.getFirst();
		} else if (i > 0 && i == childLayouts.size() - 1) {
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
		int i = childLayouts.indexOf(childLayout);
		if (i == 0 && childLayouts.size() > 1) {
			// Child is first, get the first divider if one exists.
			divider = dividers.getFirst();
		} else if (i > 0 && i == childLayouts.size() - 1) {
			// Child is last, get the last divider if one exists.
			divider = dividers.getLast();
		} else {
			// Not supported.
			return true;
		}

		// Check the divider state.
		return !divider.isDisable();
	}

	@Nonnull
	@Override
	public Bento getBento() {
		return bento;
	}
}
