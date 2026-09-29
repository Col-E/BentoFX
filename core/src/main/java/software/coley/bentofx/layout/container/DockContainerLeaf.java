package software.coley.bentofx.layout.container;

import javafx.beans.property.*;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.Bento;
import software.coley.bentofx.control.Header;
import software.coley.bentofx.control.HeaderPane;
import software.coley.bentofx.control.canvas.PixelCanvas;
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
	private final ObjectProperty<@Nullable Dockable> selectedDockable = new SimpleObjectProperty<>();
	private final ObjectProperty<Side> side = new SimpleObjectProperty<>(Side.TOP);
	private final ObservableValue<Orientation> orientation = side.map(s -> s.isHorizontal() ? Orientation.HORIZONTAL : Orientation.VERTICAL);
	private final BooleanProperty collapsed = new SimpleBooleanProperty();
	private final ObjectProperty<DockContainerLeafMenuFactory> menuFactory = new SimpleObjectProperty<>();
	private final DoubleProperty uncollapsedWidth = new SimpleDoubleProperty();
	private final DoubleProperty uncollapsedHeight = new SimpleDoubleProperty();
	private @Nullable BooleanProperty canSplit;
	private final PixelCanvas canvas;
	private final HeaderPane headerPane;
	private final Bento bento;
	private final String identifier;
	private @Nullable DockContainerBranch parent;
	private boolean pruneWhenEmpty = true;

	/**
	 * Creates a leaf container.
	 *
	 * @param bento
	 * 		Parent bento instance.
	 * @param identifier
	 * 		This container's identifier.
	 */
	public DockContainerLeaf(Bento bento, String identifier) {
		this.bento = bento;
		this.identifier = identifier;
		this.headerPane = bento.controlsBuilding().newHeaderPane(this);

		getStyleClass().addAll("bento", "container", "container-leaf");

		// Fit the canvas to the container size
		canvas = bento.controlsBuilding().newCanvas(this);
		canvas.setMouseTransparent(true);
		canvas.prefWidthProperty().bind(widthProperty());
		canvas.prefHeightProperty().bind(heightProperty());

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
	public void setParentContainer(DockContainerBranch parent) {
		DockContainerBranch priorParent = this.parent;
		this.parent = parent;
		bento.events().fire(new DockEvent.ContainerParentChanged(this, priorParent, parent));
	}

	@Override
	public void removeAsParentContainer(DockContainerBranch parent) {
		if (this.parent == parent) {
			DockContainerBranch priorParent = this.parent;
			this.parent = null;
			bento.events().fire(new DockEvent.ContainerParentChanged(this, priorParent, parent));
		}
	}

	@Override
	public boolean visit(SearchVisitor visitor) {
		if (visitor.visitLeaf(this)) for (Dockable dockable : dockables)
			if (!visitor.visitDockable(dockable)) return false;
		return true;
	}

	@Override
	public ObservableList<Dockable> getDockables() {
		return dockablesView;
	}

	@Nullable
	public Dockable getSelectedDockable() {
		return selectedDockable.get();
	}

	public ObservableObjectValue<@Nullable Dockable> selectedDockableProperty() {
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
	public boolean addDockable(Dockable dockable) {
		return addDockable(dockables.size(), dockable);
	}

	@Override
	public boolean addDockable(int index, Dockable dockable) {
		// Containment check
		if (dockables.contains(dockable)) return false;

		// Bounds check
		if (index < 0 || index > dockables.size()) return false;

		// Update dockable model
		dockables.add(index, dockable);
		dockable.setContainer(this);

		// Notify event listeners
		bento.events().fire(new DockEvent.DockableAdded(this, dockable));

		// If this is the first dockable being added, select it
		if (dockables.size() == 1) selectDockable(dockable);

		return true;
	}

	@Override
	public boolean removeDockable(Dockable dockable) {
		int i = dockables.indexOf(dockable);

		// Update dockable model
		if (i >= 0) {
			boolean wasSelected = getSelectedDockable() == dockable;
			dockables.remove(i);
			dockable.setContainer(null);

			// If the removed dockable was the selected one, then select
			// the next available dockable if one is available
			if (wasSelected) {
				if (!dockables.isEmpty()) {
					Dockable nextSourceDockable = dockables.get(Math.min(i, dockables.size() - 1));
					selectDockable(nextSourceDockable);
				} else {
					selectDockable(null);
				}
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
	public boolean closeDockable(Dockable dockable) {
		if (dockable.isClosable() && dockables.contains(dockable)) {
			dockable.fireCloseListeners();

			// Notify event listeners this dockable will close
			final DockEvent.DockableClosing event = new DockEvent.DockableClosing(dockable, this);
			bento.events().fire(event);

			if (event.isCancelled())
				return false;

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
	public boolean canReceiveDockable(Dockable dockable, @Nullable Side receivedSide) {
		// Must not already have the given dockable if not splitting.
		if (receivedSide == null && dockables.contains(dockable)) return false;

		// If there is a side provided and there are no dockables here, then we can receive the dockable.
		if (dockables.isEmpty()) return true;

		// If there are existing dockables, check if the DnD behavior allows the dockable to be placed here.
		return bento.getDragDropBehavior().canReceiveDockable(this, receivedSide, dockable);
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
	 * Draws a full-region overlay hint on this container's canvas.
	 *
	 * @param target
	 * 		Region to draw as an overlay on this container's canvas.
	 */
	public void drawCanvasHint(Region target) {
		drawCanvasHint(target, null);
	}

	/**
	 * Draws an overlay hint on this container's canvas.
	 *
	 * @param target
	 * 		Region to draw as an overlay on this container's canvas.
	 * @param side
	 * 		Side of the region to draw, or {@code null} for the full region.
	 */
	public void drawCanvasHint(Region target, @Nullable Side side) {
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
		canvas.clear();

		// Draw a rect around the given target region.
		final int color = 0x44FF0000;
		final int borderColor = 0x88FF0000;
		final int borderWidth = 2;
		final double x = ox + target.getLayoutX();
		final double y = oy + target.getLayoutY();
		final double w = target.getWidth();
		final double h = target.getHeight();
		switch (side) {
			// TODO: For accessibility, draw additional directional indicators
			case TOP -> canvas.fillBorderedRect(x, y, w, h / 2, borderWidth, color, borderColor);
			case BOTTOM -> canvas.fillBorderedRect(x, y + h / 2, w, h / 2, borderWidth, color, borderColor);
			case LEFT -> canvas.fillBorderedRect(x, y, w / 2, h, borderWidth, color, borderColor);
			case RIGHT -> canvas.fillBorderedRect(x + w / 2, y, w / 2, h, borderWidth, color, borderColor);
			case null -> canvas.fillBorderedRect(x, y, w, h, borderWidth, color, borderColor);
		}
		canvas.commit();
	}

	/**
	 * Clear this container's overlay canvas.
	 */
	public void clearCanvas() {
		canvas.clear();
		canvas.commit();
	}

	/**
	 * @return Overlay canvas.
	 */
	public PixelCanvas getCanvas() {
		return canvas;
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
		DockContainerBranch currentParent = parent;

		// No parent means there is nothing to collapse this leaf against.
		if (currentParent == null)
			return isCollapsed();

		boolean result;
		if (isCollapsed()) {
			currentParent.setContainerCollapsed(this, false);
			result = isCollapsed();

			// If we were collapsed, and no longer are, select the given dockable.
			if (!result)
				selectDockable(selectedDockable);
		} else {
			currentParent.setContainerCollapsed(this, true);
			result = isCollapsed();

			// If we were uncollapsed but now are collapsed, clear the selected dockable.
			if (result)
				selectDockable(null);
		}
		return result;
	}

	/**
	 * Updates the tracked size to apply once this container is uncollapsed.
	 *
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
	 * Sets the collapsed pseudo-state, without adjusting layout.
	 *
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
	public double getUncollapsedSize() {
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
	public Header getHeader(Dockable dockable) {
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
	 * Sets the side to place {@link Header} displays on.
	 *
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
	public ObjectProperty<Side> sideProperty() {
		return side;
	}

	/**
	 * @return Orientation of the {@link #sideProperty() side} of this container.
	 * {@code null} when the side value is {@code null}.
	 *
	 * @see Side#isHorizontal()
	 * @see Side#isVertical()
	 */
	@Nullable
	public Orientation getOrientation() {
		return orientation.getValue();
	}

	/**
	 * @return Property mapping the {@link #sideProperty() side} of this container to an orientation.
	 *
	 * @see Side#isHorizontal()
	 * @see Side#isVertical()
	 */
	public ObservableValue<Orientation> orientationProperty() {
		return orientation;
	}

	/**
	 * @return Collapsed state property.
	 */
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
	public ObjectProperty<DockContainerLeafMenuFactory> menuFactoryProperty() {
		return menuFactory;
	}

	/**
	 * Sets the context menu factory for this container.
	 *
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
	public BooleanProperty canSplitProperty() {
		if (canSplit == null) canSplit = new SimpleBooleanProperty(true);
		return canSplit;
	}

	/**
	 * Sets whether this leaf can be split via drag-n-drop operations.
	 *
	 * @param canSplit
	 *        {@code true} if this leaf can be split via drag-n-drop operations.
	 */
	public void setCanSplit(boolean canSplit) {
		canSplitProperty().set(canSplit);
	}

	/**
	 * Sets the size this container will occupy when it is uncollapsed. The public
	 * counterpart to {@link #getUncollapsedSize()}, intended for restoring a size
	 * that was recorded earlier - for instance by a layout persistence layer
	 * re-applying a saved layout.
	 * <p>
	 * While this container is uncollapsed the tracking properties are bound to its
	 * live width and height, so a value set here would be overwritten immediately
	 * and is ignored. Call this only while {@link #isCollapsed()} is {@code true},
	 * which is when the tracking properties are unbound and the value is the one
	 * that will be restored on expansion.
	 *
	 * @param size
	 * 		Size to occupy once uncollapsed.
	 *
	 * @return {@code true} when the size was applied, {@code false} when it was
	 * ignored because this container is not collapsed.
	 *
	 * @see #getUncollapsedSize()
	 */
	public boolean setUncollapsedSize(double size) {
		if (uncollapsedWidth.isBound() || uncollapsedHeight.isBound())
			return false;

		updateCollapsedSize(size);
		return true;
	}

	@Override
	public Bento getBento() {
		return bento;
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public String toString() {
		return "Container-Leaf:" + getIdentifier();
	}
}
