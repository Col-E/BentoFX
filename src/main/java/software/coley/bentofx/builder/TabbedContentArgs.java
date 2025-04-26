package software.coley.bentofx.builder;

import jakarta.annotation.Nonnull;
import javafx.geometry.Side;
import software.coley.bentofx.Dockable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabbedContentArgs extends ContentArgs<TabbedContentArgs> {
	private final List<Dockable> dockables = new ArrayList<>();
	private boolean autoPruneWhenEmpty = true;
	private boolean canSplit = true;
	private Side side = Side.TOP;

	@Nonnull
	public List<Dockable> getDockables() {
		return dockables;
	}

	@Nonnull
	public TabbedContentArgs addDockables(@Nonnull Dockable... dockables) {
		return addDockables(Arrays.asList(dockables));
	}

	@Nonnull
	public TabbedContentArgs addDockables(@Nonnull DockableBuilder... builders) {
		List<Dockable> dockables = Arrays.stream(builders)
				.map(DockableBuilder::build)
				.toList();
		return addDockables(dockables);
	}

	@Nonnull
	public TabbedContentArgs addDockables(@Nonnull List<Dockable> dockables) {
		this.dockables.addAll(dockables);
		return this;
	}

	public boolean isAutoPruneWhenEmpty() {
		return autoPruneWhenEmpty;
	}

	@Nonnull
	public TabbedContentArgs setAutoPruneWhenEmpty(boolean autoPruneWhenEmpty) {
		this.autoPruneWhenEmpty = autoPruneWhenEmpty;
		return this;
	}

	public boolean isCanSplit() {
		return canSplit;
	}

	@Nonnull
	public TabbedContentArgs setCanSplit(boolean canSplit) {
		this.canSplit = canSplit;
		return this;
	}

	@Nonnull
	public Side getSide() {
		return side;
	}

	@Nonnull
	public TabbedContentArgs setSide(@Nonnull Side side) {
		this.side = side;
		return this;
	}
}
