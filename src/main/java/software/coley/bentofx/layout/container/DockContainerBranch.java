package software.coley.bentofx.layout.container;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.Selector;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.event.DockEvent;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.search.SearchVisitor;
import software.coley.bentofx.util.BentoUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A container that holds other containers as resizable children.
 *
 * @author Matt Coley
 */
public non-sealed class DockContainerBranch extends SplitPane implements DockContainer {
	private static final Selector DIVIDER_SELECTOR = Selector.createSelector(".split-pane-divider");
	private final ObservableList<DockContainer> childContainers = FXCollections.observableArrayList();
	private final ObservableList<DockContainer> childContainersView = FXCollections.unmodifiableObservableList(childContainers);
	private final Bento bento;
	private final String identifier;
	private List<Runnable> queue;
	private DockContainerBranch parent;
	private boolean pruneWhenEmpty = true;

	/**
	 * @param bento
	 * 		Parent bento instance.
	 * @param identifier
	 * 		This container's identifier.
	 */
	public DockContainerBranch(@Nonnull Bento bento, @Nonnull String identifier) {
		this.bento = bento;
		this.identifier = identifier;

		getStyleClass().addAll("bento", "container", "container-branch");
	}

	@Nonnull
	@Override
	public Bento getBento() {
		return bento;
	}

	@Nullable
	@Override
	public DockContainerBranch getParentContainer() {
		return parent;
	}

	@Override
	public void setParentContainer(@Nonnull DockContainerBranch parent) {
		DockContainerBranch priorParent = this.parent;
		this.parent = parent;
		bento.events().fire(new DockEvent.ContainerParentChanged(this, priorParent, parent));
	}

	@Override
	public void removeAsParentContainer(@Nonnull DockContainerBranch parent) {
		if (this.parent == parent) {
			DockContainerBranch priorParent = this.parent;
			this.parent = null;
			bento.events().fire(new DockEvent.ContainerParentChanged(this, priorParent, parent));
		}
	}

	@Override
	public boolean visit(@Nonnull SearchVisitor visitor) {
		if (visitor.visitBranch(this))
			for (DockContainer container : childContainers)
				if (!container.visit(visitor))
					return false;
		return true;
	}

	/**
	 * @param containers
	 * 		Containers to add.
	 *
	 * @return {@code true} if one or more of the containers were added.
	 */
	public boolean addContainers(@Nonnull DockContainer... containers) {
		boolean changed = false;
		for (DockContainer container : containers)
			changed |= addContainer(container);
		return changed;
	}

	/**
	 * @param container
	 * 		Container to add.
	 *
	 * @return {@code true} when added.
	 */
	public boolean addContainer(@Nonnull DockContainer container) {
		return addContainer(childContainers.size(), container);
	}

	/**
	 * @param index
	 * 		Index to add the container at.
	 * @param container
	 * 		Container to add.
	 *
	 * @return {@code true} when added.
	 */
	public boolean addContainer(int index, @Nonnull DockContainer container) {
		if (index < 0 || index > childContainers.size())
			return false;

		if (childContainers.contains(container))
			return false;

		childContainers.add(container);

		container.setParentContainer(this);
		getItems().add(index, container.asRegion());

		bento.events().fire(new DockEvent.ContainerChildAdded(this, container));
		return true;
	}

	/**
	 * @param child
	 * 		A child container within this container.
	 * @param replacement
	 * 		A container to replace the existing child with.
	 *
	 * @return {@code true} when replaced.
	 */
	public boolean replaceContainer(@Nonnull DockContainer child, @Nonnull DockContainer replacement) {
		if (childContainers.contains(child)) {
			child.removeAsParentContainer(this);

			int i = childContainers.indexOf(child);
			childContainers.set(i, replacement);
			replacement.setParentContainer(this);

			double[] dividers = getDividerPositions(); // Cache existing divider positions
			getItems().set(i, replacement.asRegion());
			setDividerPositions(dividers); // Set dividers to prior positions (replace operation resets positions)

			bento.events().fire(new DockEvent.ContainerChildRemoved(this, child));
			bento.events().fire(new DockEvent.ContainerChildAdded(this, replacement));
			return true;
		}
		return false;
	}

	/**
	 * @param child
	 * 		A child container within this container.
	 *
	 * @return {@code true} when removed.
	 */
	public boolean removeContainer(@Nonnull DockContainer child) {
		if (childContainers.remove(child)) {
			getItems().remove(child.asRegion());
			child.removeAsParentContainer(this);

			bento.events().fire(new DockEvent.ContainerChildRemoved(this, child));

			// Propagate scene graph simplification upwards.
			//  - Empty branches are pruned.
			//  - Single child branches are replaced with their child.
			if (doPruneWhenEmpty()) {
				if (childContainers.isEmpty()) {
					removeFromParent();
				} else if (childContainers.size() == 1 && parent instanceof DockContainerBranch parentBranch) {
					parentBranch.replaceContainer(this, childContainers.getFirst());
				}
			}

			return true;
		}
		return false;
	}

	/**
	 * @param child
	 * 		A child container within this container.
	 * @param size
	 * 		Size in pixels to set the child container width or height to <i>(Depending on {@link #getOrientation()})</i>
	 *
	 * @return {@code true} when updated.
	 */
	public boolean setContainerSizePx(@Nonnull DockContainer child, double size) {
		return setContainerSizePx0(child, size, true);
	}

	private boolean setContainerSizePx0(@Nonnull DockContainer child, double size, boolean updateSize) {
		// We rely on knowing the current layout sizes for this implementation, so we need to delegate
		// any requests to this method to later when the layout for this container and all children
		// has been computed.
		if (getScene() == null) {
			addQueue(() -> setContainerSizePx0(child, size, updateSize));
			return false;
		}

		// If the container is collapsed, update the size it will take up when it becomes uncollapsed.
		if (updateSize && isContainerCollapsed(child) && child instanceof DockContainerLeaf leaf) {
			leaf.updateCollapsedSize(size);
			return true;
		}

		int i = childContainers.indexOf(child);
		if (i >= 0) {
			Orientation orientation = getOrientation();

			// When we set the size of the container we need to consider the divider as part of the size.
			// Dividers get set to positions by their center point. So if we have a 8px wide divider, that
			// is 4px that implicitly acts as padding to our container that we need to accommodate for.
			Node divider = getChildren().stream()
					.filter(DIVIDER_SELECTOR::applies)
					.findFirst().orElse(null);
			if (divider == null)
				return false;
			double dividerSize = orientation == Orientation.VERTICAL ?
					divider.getLayoutBounds().getHeight() :
					divider.getLayoutBounds().getWidth();
			double adjustedSize = size + dividerSize / 2;

			// Calculate the ratio we need to support setting the container to the requested size even
			// though split-pane only offers percentage setters.
			double max = orientation == Orientation.HORIZONTAL ? getWidth() : getHeight();
			double ratio = Math.clamp(adjustedSize / max, 0, 1);
			if (i == 0 && childContainers.size() > 1) {
				// Child is first, move the first divider if one exists
				setDividerPosition(0, ratio);
			} else if (i > 0 && i == childContainers.size() - 1) {
				// Child is last, move the last divider if one exists
				setDividerPosition(i - 1, 1 - ratio);
			}
			return true;
		} else {
			// Try and see if any child container has the given container and set its size.
			for (DockContainer childContainer : childContainers)
				if (childContainer instanceof DockContainerBranch childBranch && childBranch.setContainerSizePx0(child, size, updateSize))
					return true;
		}
		return false;
	}

	/**
	 * @param child
	 * 		A child container within this container.
	 * @param percent
	 * 		Size in percentage of the total width/height of this container to set the child container to <i>(Depending on {@link #getOrientation()})</i>
	 *
	 * @return {@code true} when updated.
	 */
	public boolean setContainerSizePercent(@Nonnull DockContainer child, double percent) {
		// TODO: This does not need to be queued in the same way the SizePx does however...
		//  - when the child is collapsed we need to determine how to persist the percent
		//    so that when it uncollapses the percentage is correct.

		int i = childContainers.indexOf(child);
		if (i >= 0) {
			if (i == 0 && childContainers.size() > 1) {
				// Child is first, move the first divider if one exists
				setDividerPosition(0, percent);
			} else if (i > 0 && i == childContainers.size() - 1) {
				// Child is last, move the last divider if one exists
				setDividerPosition(i - 1, 1 - percent);
			}
		} else {
			// Try and see if any child container has the given container and set its size.
			for (DockContainer childContainer : childContainers)
				if (childContainer instanceof DockContainerBranch childBranch && childBranch.setContainerSizePercent(child, percent))
					return true;
		}
		return false;
	}

	/**
	 * @param child
	 * 		A child container within this container.
	 *
	 * @return {@code true} if the child is resizable.
	 */
	public boolean isContainerResizable(@Nonnull DockContainer child) {
		// Get our direct children that are dividers.
		List<Node> dividers = getChildren().stream().filter(DIVIDER_SELECTOR::applies).toList();
		if (dividers.isEmpty())
			return true;

		// Get the divider to modify.
		Node divider;
		int i = childContainers.indexOf(child);
		if (i == 0 && childContainers.size() > 1) {
			// Child is first, get the first divider if one exists.
			divider = dividers.getFirst();
		} else if (i > 0 && i == childContainers.size() - 1) {
			// Child is last, get the last divider if one exists.
			divider = dividers.getLast();
		} else {
			// Not supported.
			return true;
		}

		// Check the divider state.
		return !divider.isDisable();
	}

	/**
	 * @param child
	 * 		A child container within this container.
	 * @param resizable
	 * 		Resizable state to apply to the child.
	 *
	 * @return {@code true} when updated.
	 */
	public boolean setContainerResizable(@Nonnull DockContainer child, boolean resizable) {
		// We rely on the split-pane skin having laid out the divders for this implementation, so we need to delegate
		// any requests to this method to later when the layout for this container and all children
		// has been computed.
		if (getScene() == null) {
			addQueue(() -> setContainerResizable(child, resizable));
			return false;
		}

		// Get our direct children that are dividers.
		List<Node> dividers = getChildren().stream().filter(DIVIDER_SELECTOR::applies).toList();
		if (dividers.isEmpty())
			return false;

		// Get the divider to modify.
		Node divider;
		int i = childContainers.indexOf(child);
		if (i == 0 && childContainers.size() > 1) {
			// Child is first, get the first divider if one exists.
			divider = dividers.getFirst();
		} else if (i > 0 && i == childContainers.size() - 1) {
			// Child is last, get the last divider if one exists.
			divider = dividers.getLast();
		} else {
			// Not supported.
			return false;
		}

		// Disable/enable the divider
		divider.setDisable(!resizable);
		return true;
	}

	/**
	 * @param child
	 * 		A child container within this container.
	 *
	 * @return {@code true} if the child is collapsed.
	 */
	public boolean isContainerCollapsed(@Nonnull DockContainer child) {
		return child instanceof DockContainerLeaf leaf && leaf.isCollapsed();
	}

	/**
	 * @param child
	 * 		A child container within this container.
	 * @param collapse
	 * 		Collapsed state to apply to the child.
	 *
	 * @return {@code true} when updated.
	 */
	public boolean setContainerCollapsed(@Nonnull DockContainerLeaf child, boolean collapse) {
		// Skip if there is nothing to branch between. If there is only one child collapsing makes no sense
		// as the same layout space will still be occupied by the leaf, but the leaf display will be hidden.
		// Collapsing can only occur if there is a splitter between two or more child containers.
		if (collapse && childContainers.size() <= 1)
			return false;

		// Skip if we don't have the given child as the first or last entry.
		int i = childContainers.indexOf(child);
		if (i < 0 || (i != 0 && i != childContainers.size() - 1))
			return false;

		// Skip if the child already has the given collapsed state.
		boolean isCollapsed = child.isCollapsed();
		if (isCollapsed == collapse)
			return false;

		// Skip if this split-pane orientation is not compatible with the tabbed layout side.
		Orientation orientation = orientationProperty().get();
		Side childSide = child.getSide();
		if (childSide == null)
			return false;
		if (orientation == Orientation.HORIZONTAL && (childSide == Side.TOP || childSide == Side.BOTTOM))
			return false;
		if (orientation == Orientation.VERTICAL && (childSide == Side.LEFT || childSide == Side.RIGHT))
			return false;

		// Skip if this would steal the divider away from some adjacent collapsed destination space.
		if (i > 0 && childContainers.get(i - 1) instanceof DockContainerLeaf adjacentChild && adjacentChild.isCollapsed())
			return false;
		if (i <= childContainers.size() - 2 && childContainers.get(i + 1) instanceof DockContainerLeaf adjacentChild && adjacentChild.isCollapsed())
			return false;

		// Toggle collapse state and update the dimensions of the child to only show the headers.
		if (collapse) {
			double collapsedSize = child.getCollapsedSize();
			child.setCollapsedState(true);
			setContainerSizePx0(child, collapsedSize, false);
			setContainerResizable(child, false);
		} else {
			double originalSize = child.getUncollapsedSize();
			child.setCollapsedState(false);
			setContainerSizePx0(child, originalSize, false);
			setContainerResizable(child, true);

		}
		return true;
	}

	/**
	 * @return Unmodifiable list of containers within this container.
	 */
	@Nonnull
	public ObservableList<DockContainer> getChildContainers() {
		return childContainersView;
	}

	@Nonnull
	@Override
	public List<Dockable> getDockables() {
		return childContainers.stream()
				.flatMap(c -> c.getDockables().stream())
				.toList();
	}

	@Override
	public boolean addDockable(@Nonnull Dockable dockable) {
		for (DockContainer container : childContainers)
			if (container.addDockable(dockable))
				return true;
		return false;
	}

	@Override
	public boolean addDockable(int index, @Nonnull Dockable dockable) {
		// Calling the indexed add on the branch container is probably a bad idea.
		for (DockContainer container : childContainers)
			if (container.addDockable(index, dockable))
				return true;
		return false;
	}

	@Override
	public boolean removeDockable(@Nonnull Dockable dockable) {
		DockContainer updatedContainer = null;
		for (DockContainer container : childContainers)
			if (container.removeDockable(dockable)) {
				updatedContainer = container;
				break;
			}

		if (updatedContainer != null) {
			if (updatedContainer.doPruneWhenEmpty() && updatedContainer.getDockables().isEmpty())
				removeContainer(updatedContainer);
			return true;
		}

		return false;
	}

	@Override
	public boolean closeDockable(@Nonnull Dockable dockable) {
		DockContainer updatedContainer = null;
		for (DockContainer container : childContainers)
			if (container.closeDockable(dockable)) {
				updatedContainer = container;
				break;
			}

		if (updatedContainer != null) {
			if (updatedContainer.doPruneWhenEmpty() && updatedContainer.getDockables().isEmpty())
				removeContainer(updatedContainer);
			return true;
		}

		return false;
	}

	@Override
	public boolean doPruneWhenEmpty() {
		return pruneWhenEmpty;
	}

	@Override
	public void setPruneWhenEmpty(boolean pruneWhenEmpty) {
		this.pruneWhenEmpty = pruneWhenEmpty;
	}

	@Nonnull
	@Override
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public boolean matchesIdentity(@Nonnull Identifiable other) {
		return identifier.equals(other.getIdentifier());
	}

	@Override
	protected void layoutChildren() {
		super.layoutChildren();

		if (queue != null) {
			queue.forEach(Runnable::run);
			queue = null;
		}
	}

	private void addQueue(@Nonnull Runnable action) {
		// Gee, two layers of indirection?
		// Yes. I know this is stupid, but it delays registering the actions to a point
		// later where the reliance on the current layout is actually correct and not
		// some half-way "its laid out but not properly yet" state.
		BentoUtils.scheduleWhenShown(this, b -> {
			if (queue == null)
				queue = new ArrayList<>();
			queue.add(action);
		});
	}

	@Override
	public String toString() {
		return "Container-Branch:" + getIdentifier();
	}
}
