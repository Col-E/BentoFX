package software.coley.bentofx.layout.container;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.control.Header;
import software.coley.bentofx.control.HeaderPane;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.event.DockEvent;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.search.SearchVisitor;

import java.util.Objects;

import static software.coley.bentofx.util.BentoStates.PSEUDO_COLLAPSED;

/**
 * A container that displays multiple {@link Dockable} in a tab-pane like layout.
 *
 * @author Matt Coley
 */
public non-sealed class DockContainerLeaf extends StackPane implements DockContainer {
	private final ObservableList<Dockable> dockables = FXCollections.observableArrayList();
	private final ObservableList<Dockable> dockablesView = FXCollections.unmodifiableObservableList(dockables);
	private final ObjectProperty<Dockable> selectedDockable = new SimpleObjectProperty<>();
	private final ObjectProperty<Side> side = new SimpleObjectProperty<>(Side.TOP);
	private final BooleanProperty collapsed = new SimpleBooleanProperty();
	private final ObjectProperty<DockContainerLeafMenuFactory> menuFactory = new SimpleObjectProperty<>();
	private final DoubleProperty uncollapsedWidth = new SimpleDoubleProperty();
	private final DoubleProperty uncollapsedHeight = new SimpleDoubleProperty();
	private BooleanProperty canSplit;
	private final Canvas canvas = new Canvas();
	private final HeaderPane headerPane = new HeaderPane(this);
	private final Bento bento;
	private final String identifier;
	private DockContainerBranch parent;
	private boolean pruneWhenEmpty = true;

	/**
	 * @param bento
	 * 		Parent bento instance.
	 * @param identifier
	 * 		This container's identifier.
	 */
	public DockContainerLeaf(@Nonnull Bento bento, @Nonnull String identifier) {
		this.bento = bento;
		this.identifier = identifier;

		getStyleClass().addAll("bento", "container", "container-leaf");

		// Fit the canvas to the container size
		canvas.setManaged(false);
		canvas.setMouseTransparent(true);
		canvas.widthProperty().bind(widthProperty());
		canvas.heightProperty().bind(heightProperty());

		uncollapsedWidth.bind(widthProperty());
		uncollapsedHeight.bind(heightProperty());

		getChildren().addAll(headerPane, canvas);
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
		if (visitor.visitLeaf(this)) for (Dockable dockable : dockables)
			if (!visitor.visitDockable(dockable)) return false;
		return true;
	}

	@Nonnull
	@Override
	public ObservableList<Dockable> getDockables() {
		return dockablesView;
	}

	@Nullable
	public Dockable getSelectedDockable() {
		return selectedDockable.get();
	}

	@Nonnull
	public ObservableObjectValue<Dockable> selectedDockableProperty() {
		return selectedDockable;
	}

	/**
	 * @param dockable
	 * 		Dockable to mark as selected.
	 *
	 * @return {@code true} when updated.
	 */
	public boolean selectDockable(@Nullable Dockable dockable) {
		// Special case for clearing selection
		if (dockable == null) {
			selectedDockable.set(null);
			return true;
		}

		// Selecting some dockable this leaf container contains
		if (dockables.contains(dockable)) {
			selectedDockable.set(dockable);

			// Then focus the container
			if (!isFocusWithin()) requestFocus();

			bento.events().fire(new DockEvent.DockableSelected(dockable, this));
			return true;
		}

		return false;
	}

	@Override
	public boolean addDockable(@Nonnull Dockable dockable) {
		return addDockable(dockables.size(), dockable);
	}

	@Override
	public boolean addDockable(int index, @Nonnull Dockable dockable) {
		// Containment check
		if (dockables.contains(dockable)) return false;

		// Bounds check
		if (index < 0 || index > dockables.size()) return false;

		// Update dockable model
		dockables.add(index, dockable);
		dockable.setContainer(this);

		// Notify event listeners
		bento.events().fire(new DockEvent.DockableAdded(dockable, this));

		// If this is the first dockable being added, select it
		if (dockables.size() == 1) selectDockable(dockable);

		return true;
	}

	@Override
	public boolean removeDockable(@Nonnull Dockable dockable) {
		int i = dockables.indexOf(dockable);

		// Update dockable model
		if (i >= 0) {
			dockables.remove(i);
			dockable.setContainer(null);

			// Select the next available dockable if one is available
			if (!dockables.isEmpty()) {
				Dockable nextSourceDockable = dockables.get(Math.min(i, dockables.size() - 1));
				selectDockable(nextSourceDockable);
			} else {
				selectDockable(null);
			}

			// Notify event listeners
			bento.events().fire(new DockEvent.DockableRemoved(dockable, this));

			// Prune from parent layout if we're empty and set to auto-prune.
			if (doPruneWhenEmpty() && dockables.isEmpty()) removeFromParent();

			return true;
		}
		return false;
	}

	@Override
	public boolean closeDockable(@Nonnull Dockable dockable) {
		if (dockable.isClosable() && dockables.contains(dockable)) {
			dockable.fireCloseListeners();

			// Notify event listeners this dockable will close
			bento.events().fire(new DockEvent.DockableClosing(dockable, this));

			return removeDockable(dockable);
		}
		return false;
	}

	/**
	 * @param dockable
	 * 		Some dockable.
	 * @param receivedSide
	 * 		The side the dockable will be dropped to as part of a DnD operation.
	 *
	 * @return {@code true} when this container can receive the dockable.
	 */
	public boolean canReceiveDockable(@Nonnull Dockable dockable, @Nullable Side receivedSide) {
		// Must not already have the given dockable if not splitting.
		if (receivedSide == null && dockables.contains(dockable)) return false;

		// If there is a side provided and there are no dockables here, then we can receive the dockable.
		if (dockables.isEmpty()) return true;

		// If there are existing dockables, the incoming dockable must have a compatible group.
		return dockables.stream().anyMatch(d -> d.getDragGroup() == dockable.getDragGroup());
	}

	@Override
	public boolean doPruneWhenEmpty() {
		return pruneWhenEmpty;
	}

	@Override
	public void setPruneWhenEmpty(boolean pruneWhenEmpty) {
		this.pruneWhenEmpty = pruneWhenEmpty;
	}

	/**
	 * @param target
	 * 		Region to draw as an overlay on this container's canvas.
	 */
	public void drawCanvasHint(@Nonnull Region target) {
		drawCanvasHint(target, null);
	}

	/**
	 * @param target
	 * 		Region to draw as an overlay on this container's canvas.
	 * @param side
	 * 		Side of the region to draw, or {@code null} for the full region.
	 */
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
			// TODO: For accessibility, draw additional directional indicators
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

	/**
	 * Clear this container's overlay canvas.
	 */
	public void clearCanvas() {
		GraphicsContext g = canvas.getGraphicsContext2D();
		g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
	}

	/**
	 * @return {@code true} when collapsed.
	 */
	public boolean isCollapsed() {
		return getPseudoClassStates().contains(PSEUDO_COLLAPSED);
	}

	/**
	 * @param selectedDockable
	 * 		Dockable whose interaction is causing the collapsed state to toggle.
	 *
	 * @return {@link #isCollapsed()} after toggling.
	 */
	public boolean toggleCollapse(@Nullable Dockable selectedDockable) {
		if (isCollapsed()) {
			parent.setContainerCollapsed(this, false);
			selectDockable(selectedDockable);
		} else {
			parent.setContainerCollapsed(this, true);
			selectDockable(null);
		}
		return isCollapsed();
	}

	/**
	 * @param size
	 * 		Uncollapsed size.
	 */
	protected void updateCollapsedSize(double size) {
		// The same size is applied to the width and height because in the context that
		// this is used within, we only request the correct property later on, ignoring
		// the other one that is "incorrect".
		if (!uncollapsedWidth.isBound()) uncollapsedWidth.set(size);
		if (!uncollapsedHeight.isBound()) uncollapsedHeight.set(size);
	}

	/**
	 * @param collapse
	 * 		New collapsed state.
	 */
	protected void setCollapsedState(boolean collapse) {
		if (collapse) {
			// During our collapsed state, nothing should be selected.
			selectDockable(null);

			// We unbind the tracking properties so that the collapsed size is not recorded.
			// This is because when we uncollapse we want to restore our pre-collapsed size.
			uncollapsedWidth.unbind();
			uncollapsedHeight.unbind();
		} else {
			// During our uncollapsed state we want to consistently track our size.
			uncollapsedWidth.bind(widthProperty());
			uncollapsedHeight.bind(heightProperty());
		}

		// Update property/psueod-state
		collapsed.set(collapse);
		pseudoClassStateChanged(PSEUDO_COLLAPSED, collapse);
	}

	/**
	 * @return Collapsed size of this container.
	 */
	protected double getCollapsedSize() {
		return switch (getSide()) {
			case TOP, BOTTOM -> Objects.requireNonNull(headerPane.getHeaders()).getHeight();
			case LEFT, RIGHT -> Objects.requireNonNull(headerPane.getHeaders()).getWidth();
			case null -> throw new IllegalStateException("Container with null side should not be collapsed");
		};
	}

	/**
	 * @return Uncollapsed size of this container.
	 */
	protected double getUncollapsedSize() {
		return switch (getSide()) {
			case TOP, BOTTOM -> uncollapsedHeight.get();
			case LEFT, RIGHT -> uncollapsedWidth.get();
			case null -> throw new IllegalStateException("Container with null side should not be collapsed");
		};
	}

	/**
	 * @param dockable
	 * 		Some dockable.
	 *
	 * @return Associated header within this container that represents the given dockable.
	 */
	@Nullable
	public Header getHeader(@Nonnull Dockable dockable) {
		return headerPane.getHeader(dockable);
	}

	/**
	 * @return Side of this container to place {@link Header} displays on.
	 * {@code null} to not display any headers.
	 */
	@Nullable
	public Side getSide() {
		return side.get();
	}

	/**
	 * @param side
	 * 		Side of this container to place {@link Header} displays on.
	 *        {@code null} to not display any headers.
	 */
	public void setSide(@Nullable Side side) {
		this.side.set(side);
	}

	/**
	 * @return {@link Header} display side property.
	 */
	@Nonnull
	public ObjectProperty<Side> sideProperty() {
		return side;
	}

	/**
	 * @return Collapsed state property.
	 */
	@Nonnull
	public BooleanProperty collapsedProperty() {
		return collapsed;
	}

	/**
	 * @return Context menu for this container.
	 */
	@Nullable
	public ContextMenu buildContextMenu() {
		DockContainerLeafMenuFactory factory = getMenuFactory();
		return factory == null ? null : factory.build(this);
	}

	/**
	 * @return Menu factory for this container.
	 */
	@Nullable
	public DockContainerLeafMenuFactory getMenuFactory() {
		return menuFactory.get();
	}

	/**
	 * @return Menu factory property.
	 */
	@Nonnull
	public ObjectProperty<DockContainerLeafMenuFactory> menuFactoryProperty() {
		return menuFactory;
	}

	/**
	 * @param menuFactory
	 * 		Menu factory for this container.
	 */
	public void setMenuFactory(@Nullable DockContainerLeafMenuFactory menuFactory) {
		this.menuFactory.set(menuFactory);
	}

	/**
	 * @return {@code true} if this leaf can be split via drag-n-drop operations.
	 */
	public boolean isCanSplit() {
		if (parent == null) return false;
		if (canSplit == null) return true;
		return canSplit.get();
	}

	/**
	 * @return Splittable property.
	 */
	@Nonnull
	public BooleanProperty canSplitProperty() {
		if (canSplit == null) canSplit = new SimpleBooleanProperty(true);
		return canSplit;
	}

	/**
	 * @param canSplit
	 *        {@code true} if this leaf can be split via drag-n-drop operations.
	 */
	public void setCanSplit(boolean canSplit) {
		canSplitProperty().set(canSplit);
	}

	@Nonnull
	@Override
	public Bento getBento() {
		return bento;
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
	public String toString() {
		return "Container-Leaf:" + getIdentifier();
	}
}
