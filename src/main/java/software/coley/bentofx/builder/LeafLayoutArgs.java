package software.coley.bentofx.builder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.bentofx.space.DockSpace;

public class LeafLayoutArgs extends AbstractLayoutArgs<LeafLayoutArgs> {
	private DockSpace space;

	@Nullable
	public DockSpace getSpace() {
		return space;
	}

	@Nonnull
	public LeafLayoutArgs setSpace(@Nullable DockSpace space) {
		this.space = space;
		return this;
	}
}
