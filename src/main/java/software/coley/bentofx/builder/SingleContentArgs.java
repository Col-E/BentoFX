package software.coley.bentofx.builder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.geometry.Side;
import software.coley.bentofx.dockable.Dockable;

public class SingleContentArgs extends AbstractContentArgs<SingleContentArgs> {
	private Dockable dockable;
	private Side side = Side.TOP;

	@Nonnull
	public SingleContentArgs setDockable(@Nonnull Dockable dockable) {
		this.dockable = dockable;
		return this;
	}

	@Nullable
	public Dockable getDockable() {
		return dockable;
	}

	@Nullable
	public Side getSide() {
		return side;
	}

	@Nonnull
	public SingleContentArgs setSide(@Nullable Side side) {
		this.side = side;
		return this;
	}
}
