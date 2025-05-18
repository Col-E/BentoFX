package software.coley.bentofx.path;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.RootContentLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PathBuilder {
	private final RootContentLayout rootContentLayout;
	private final List<ContentLayout> contentLayouts;
	private Content content;
	private Dockable dockable;

	public PathBuilder(@Nonnull RootContentLayout rootContentLayout) {
		this(rootContentLayout, Collections.emptyList());
	}

	public PathBuilder(@Nonnull RootContentLayout rootContentLayout, @Nonnull List<ContentLayout> contentLayouts) {
		this.rootContentLayout = rootContentLayout;
		this.contentLayouts = contentLayouts;
	}

	@Nonnull
	public PathBuilder inside(@Nonnull ContentLayout layout) {
		List<ContentLayout> newContentLayouts = new ArrayList<>(contentLayouts.size() + 1);
		newContentLayouts.addAll(contentLayouts);
		newContentLayouts.add(layout);
		return new PathBuilder(rootContentLayout, newContentLayouts);
	}

	@Nonnull
	public PathBuilder withContent(@Nonnull Content content) {
		this.content = content;
		return this;
	}

	@Nonnull
	public PathBuilder withDockable(@Nonnull Dockable dockable) {
		this.dockable = dockable;
		return this;
	}

	@Nonnull
	public LayoutPath buildLayoutPath() {
		return new LayoutPath(rootContentLayout, contentLayouts);
	}

	@Nonnull
	public ContentPath buildContentPath() {
		if (content == null)
			throw new IllegalStateException("Incomplete path, missing 'Content' type");
		return new ContentPath(rootContentLayout, contentLayouts, content);
	}

	@Nonnull
	public DockablePath buildDockablePath() {
		if (content == null)
			throw new IllegalStateException("Incomplete path, missing 'Content' type");
		if (dockable == null)
			throw new IllegalStateException("Incomplete path, missing 'Dockable' type");
		return new DockablePath(rootContentLayout, contentLayouts, content, dockable);
	}
}
