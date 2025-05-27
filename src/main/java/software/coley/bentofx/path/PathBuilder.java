package software.coley.bentofx.path;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.space.DockSpace;
import software.coley.bentofx.layout.DockLayout;
import software.coley.bentofx.layout.RootDockLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PathBuilder {
	private final RootDockLayout rootDockLayout;
	private final List<DockLayout> dockLayouts;
	private DockSpace space;
	private Dockable dockable;

	public PathBuilder(@Nonnull RootDockLayout rootDockLayout) {
		this(rootDockLayout, Collections.emptyList());
	}

	public PathBuilder(@Nonnull RootDockLayout rootDockLayout, @Nonnull List<DockLayout> dockLayouts) {
		this.rootDockLayout = rootDockLayout;
		this.dockLayouts = dockLayouts;
	}

	@Nonnull
	public PathBuilder inside(@Nonnull DockLayout layout) {
		List<DockLayout> newDockLayouts = new ArrayList<>(dockLayouts.size() + 1);
		newDockLayouts.addAll(dockLayouts);
		newDockLayouts.add(layout);
		return new PathBuilder(rootDockLayout, newDockLayouts);
	}

	@Nonnull
	public PathBuilder withSpace(@Nonnull DockSpace space) {
		this.space = space;
		return this;
	}

	@Nonnull
	public PathBuilder withDockable(@Nonnull Dockable dockable) {
		this.dockable = dockable;
		return this;
	}

	@Nonnull
	public LayoutPath buildLayoutPath() {
		return new LayoutPath(rootDockLayout, dockLayouts);
	}

	@Nonnull
	public SpacePath buildSpacePath() {
		if (space == null)
			throw new IllegalStateException("Incomplete path, missing 'Space' type");
		return new SpacePath(rootDockLayout, dockLayouts, space);
	}

	@Nonnull
	public DockablePath buildDockablePath() {
		if (space == null)
			throw new IllegalStateException("Incomplete path, missing 'Space' type");
		if (dockable == null)
			throw new IllegalStateException("Incomplete path, missing 'Dockable' type");
		return new DockablePath(rootDockLayout, dockLayouts, space, dockable);
	}
}
