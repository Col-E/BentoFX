package software.coley.bentofx.builder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.geometry.Side;
import software.coley.bentofx.space.TabbedSpaceMenuFactory;
import software.coley.bentofx.dockable.Dockable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabbedSpaceArgs extends AbstractArgs<TabbedSpaceArgs> {
	private final List<Dockable> dockables = new ArrayList<>();
	private boolean autoPruneWhenEmpty = true;
	private boolean canSplit = true;
	private Side side = Side.TOP;
	private TabbedSpaceMenuFactory menuFactory;

	@Nonnull
	public List<Dockable> getDockables() {
		return dockables;
	}

	@Nonnull
	public TabbedSpaceArgs addDockables(@Nonnull Dockable... dockables) {
		return addDockables(Arrays.asList(dockables));
	}

	@Nonnull
	public TabbedSpaceArgs addDockables(@Nonnull DockableBuilder... builders) {
		List<Dockable> dockables = Arrays.stream(builders)
				.map(DockableBuilder::build)
				.toList();
		return addDockables(dockables);
	}

	@Nonnull
	public TabbedSpaceArgs addDockables(@Nonnull List<Dockable> dockables) {
		this.dockables.addAll(dockables);
		return this;
	}

	public boolean isAutoPruneWhenEmpty() {
		return autoPruneWhenEmpty;
	}

	@Nonnull
	public TabbedSpaceArgs setAutoPruneWhenEmpty(boolean autoPruneWhenEmpty) {
		this.autoPruneWhenEmpty = autoPruneWhenEmpty;
		return this;
	}

	public boolean isCanSplit() {
		return canSplit;
	}

	@Nonnull
	public TabbedSpaceArgs setCanSplit(boolean canSplit) {
		this.canSplit = canSplit;
		return this;
	}

	@Nonnull
	public Side getSide() {
		return side;
	}

	@Nonnull
	public TabbedSpaceArgs setSide(@Nonnull Side side) {
		this.side = side;
		return this;
	}

	@Nullable
	public TabbedSpaceMenuFactory getMenuFactory() {
		return menuFactory;
	}

	@Nonnull
	public TabbedSpaceArgs setMenuFactory(@Nullable TabbedSpaceMenuFactory menuFactory) {
		this.menuFactory = menuFactory;
		return this;
	}
}
