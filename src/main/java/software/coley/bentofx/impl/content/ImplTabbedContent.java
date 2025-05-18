package software.coley.bentofx.impl.content;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Side;
import javafx.scene.layout.Region;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Dockable;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.content.TabbedContent;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.header.HeaderView;
import software.coley.bentofx.util.BentoUtils;

import java.util.List;

public class ImplTabbedContent extends ImplContentBase implements TabbedContent {
	private final BooleanProperty autoPruneWhenEmptyProperty;
	private final BooleanProperty canSplitProperty;
	private final ObjectProperty<Dockable> selectedProperty;
	private final ObjectProperty<Side> sideProperty;
	private final String identifier;
	private HeaderView view;

	public ImplTabbedContent(@Nonnull Bento bento, @Nonnull Side side, @Nonnull List<Dockable> dockables) {
		this(bento, side, dockables, true, true, BentoUtils.newIdentifier());
	}

	public ImplTabbedContent(@Nonnull Bento bento, @Nonnull Side side, @Nonnull List<Dockable> dockables, boolean autoPrune, boolean canSplit, @Nonnull String identifier) {
		this.identifier = identifier;
		this.sideProperty = new SimpleObjectProperty<>(side);
		this.selectedProperty = new SimpleObjectProperty<>();
		this.autoPruneWhenEmptyProperty = new SimpleBooleanProperty(autoPrune);
		this.canSplitProperty = new SimpleBooleanProperty(canSplit);

		// Setup initial header view
		setupHeaderView(bento, side, dockables);

		// Refresh the header view when the side property updates
		sideProperty.addListener((ob, old, cur) -> setupHeaderView(bento, cur, view.getDockables()));
	}

	private void setupHeaderView(@Nonnull Bento bento, @Nonnull Side side, @Nonnull List<Dockable> dockables) {
		// Track existing view state.
		Dockable oldSelected = null;
		boolean wasFocused = false;
		if (view != null) {
			oldSelected = view.getSelectedDockable();
			wasFocused = view.isFocusWithin();

			// If the old view is collapsed, we need to uncollapse before changing the target side.
			// This prevents soft-locking the user out of interacting with the updated layout.
			if (view.isCollapsed())
				view.toggleCollapsed();
		}

		// Create a new view for the given side.
		view = new HeaderView(bento, side);
		for (Dockable dockable : dockables)
			view.addDockable(dockable);
		layout.setCenter(view);

		// Restore state from previous instance.
		if (oldSelected != null)
			view.selectDockable(oldSelected);
		if (wasFocused)
			view.getContentWrapper().requestFocus();

		// Update selection to target the new view.
		selectedProperty.unbind();
		selectedProperty.bind(view.selectedProperty());
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
}
