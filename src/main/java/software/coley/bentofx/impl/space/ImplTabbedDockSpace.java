package software.coley.bentofx.impl.space;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Side;
import javafx.scene.layout.Region;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.builder.TabbedSpaceArgs;
import software.coley.bentofx.dockable.DockableDestination;
import software.coley.bentofx.space.TabbedDockSpace;
import software.coley.bentofx.space.TabbedSpaceMenuFactory;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.header.HeaderView;
import software.coley.bentofx.impl.ImplBento;
import software.coley.bentofx.impl.ImplDockable;
import software.coley.bentofx.path.SpacePath;
import software.coley.bentofx.path.DockablePath;

import java.util.List;

public class ImplTabbedDockSpace extends ImplDockSpaceBase implements TabbedDockSpace {
	private final BooleanProperty autoPruneWhenEmptyProperty;
	private final BooleanProperty canSplitProperty;
	private final ObjectProperty<Dockable> selectedProperty;
	private final ObjectProperty<Side> sideProperty;
	private final ObjectProperty<TabbedSpaceMenuFactory> tabbedSpaceMenuFactoryProperty;
	private final String identifier;
	private HeaderView view;

	public ImplTabbedDockSpace(@Nonnull ImplBento bento, @Nonnull TabbedSpaceArgs args) {
		super(bento);

		Side side = args.getSide();

		this.identifier = args.getIdentifier();
		this.sideProperty = new SimpleObjectProperty<>(side);
		this.selectedProperty = new SimpleObjectProperty<>();
		this.autoPruneWhenEmptyProperty = new SimpleBooleanProperty(args.isAutoPruneWhenEmpty());
		this.canSplitProperty = new SimpleBooleanProperty(args.isCanSplit());
		this.tabbedSpaceMenuFactoryProperty = new SimpleObjectProperty<>(args.getMenuFactory());

		getStyleClass().addAll("space", "space-tabbed");

		// Setup initial header view
		setupHeaderView(side, args.getDockables());

		// Refresh the header view when the side property updates
		sideProperty.addListener((ob, old, cur) -> setupHeaderView(cur, view.getDockables()));

		// Pass to decorator
		bento.decorate(this);
	}

	private void setupHeaderView(@Nonnull Side side, @Nonnull List<Dockable> dockables) {
		// Track existing view state.
		Dockable oldSelected = null;
		boolean wasFocused = false;
		if (view != null) {
			oldSelected = view.getSelectedDockable();
			wasFocused = view.isFocusWithin();

			// Unbind prior properties.
			view.menuFactoryProperty().unbind();

			// If the old view is collapsed, we need to uncollapse before changing the target side.
			// This prevents soft-locking the user out of interacting with the updated layout.
			if (view.isCollapsed())
				view.toggleCollapsed();
		}

		// Create a new view for the given side.
		view = new HeaderView(bento, side);
		view.menuFactoryProperty().bind(tabbedSpaceMenuFactoryProperty);
		layout.setCenter(view);

		// Populate the dockable tabs.
		// We must do this AFTER the header view is added to the layout so that our Dockable#spaceProperty() tracking is accurate.
		for (Dockable dockable : dockables)
			view.addDockable(dockable);

		// Restore state from previous instance.
		if (oldSelected != null)
			view.selectDockable(oldSelected);
		if (wasFocused)
			view.getContentWrapper().requestFocus();

		// Update selection to target the new view.
		selectedProperty.unbind();
		selectedProperty.bind(view.selectedProperty());
	}

	@Nonnull
	public TabbedSpaceArgs toBuilderArgs() {
		return new TabbedSpaceArgs()
				.setAutoPruneWhenEmpty(autoPruneWhenEmptyProperty.get())
				.setCanSplit(canSplitProperty.get())
				.setSide(sideProperty.get())
				.setMenuFactory(tabbedSpaceMenuFactoryProperty.get())
				.setIdentifier(identifier);
	}

	@Nullable
	public Header getHeader(@Nonnull Dockable dockable) {
		return view.getHeader(dockable);
	}

	@Nonnull
	@Override
	public Region getBackingRegion() {
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

	@Nonnull
	@Override
	public List<Dockable> getDockables() {
		return view.getDockables();
	}

	@Override
	public boolean addDockable(@Nonnull Dockable dockable) {
		return view.addDockable(dockable);
	}

	@Override
	public boolean removeDockable(@Nonnull Dockable dockable) {
		return view.removeDockable(dockable);
	}

	@Override
	public boolean closeDockable(@Nonnull Dockable dockable) {
		if (!dockable.closableProperty().get())
			return false;

		SpacePath spacePath = getPath();
		if (spacePath == null)
			return false;

		if (!removeDockable(dockable))
			return false;

		((ImplDockable) dockable).onClose(new DockablePath(spacePath, dockable));
		return true;
	}

	@Override
	public boolean selectDockable(@Nullable Dockable dockable) {
		return view.selectDockable(dockable);
	}

	@Nonnull
	@Override
	public BooleanProperty autoPruneWhenEmptyProperty() {
		return autoPruneWhenEmptyProperty;
	}

	@Nonnull
	@Override
	public BooleanProperty canSplitProperty() {
		return canSplitProperty;
	}

	@Nonnull
	@Override
	public ObjectProperty<Side> sideProperty() {
		return sideProperty;
	}

	@Nonnull
	@Override
	public ReadOnlyObjectProperty<Dockable> selectedDockableProperty() {
		return selectedProperty;
	}

	@Nonnull
	@Override
	public ObjectProperty<TabbedSpaceMenuFactory> menuFactoryProperty() {
		return tabbedSpaceMenuFactoryProperty;
	}

	@Nonnull
	@Override
	public DockableDestination getDockableDestination() {
		return view;
	}

	@Override
	public String toString() {
		return "Tabbed: " + getIdentifier();
	}
}
