package software.coley.bentofx.builder;

import jakarta.annotation.Nonnull;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.control.SplitPane;
import software.coley.bentofx.Dockable;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.content.TabbedContent;
import software.coley.bentofx.impl.ImplBento;
import software.coley.bentofx.impl.content.ImplTabbedContent;
import software.coley.bentofx.impl.layout.ImplLeafContentLayout;
import software.coley.bentofx.impl.layout.ImplRootContentLayout;
import software.coley.bentofx.impl.layout.ImplSplitContentLayout;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.LeafContentLayout;
import software.coley.bentofx.layout.RootContentLayout;
import software.coley.bentofx.layout.SplitContentLayout;
import software.coley.bentofx.util.BentoUtils;

import java.util.Arrays;

public class ContentBuilder {
	private final ImplBento bento;

	public ContentBuilder(@Nonnull ImplBento bento) {
		this.bento = bento;
	}

	@Nonnull
	public DockableBuilder dockable() {
		return bento.newDockableBuilder();
	}

	@Nonnull
	public RootContentLayout root(@Nonnull ContentLayout child) {
		return root(child, BentoUtils.ID_PROVIDER.get());
	}

	@Nonnull
	public RootContentLayout root(@Nonnull ContentLayout child, @Nonnull String identifier) {
		return new ImplRootContentLayout(bento, child, identifier);
	}

	@Nonnull
	public SplitContentLayout vsplit(@Nonnull ContentLayout... children) {
		return split(Orientation.VERTICAL, children);
	}

	@Nonnull
	public SplitContentLayout vsplit(@Nonnull Content... children) {
		return split(new SplitContentArgs()
				.setOrientation(Orientation.VERTICAL)
				.addChildren(Arrays.stream(children).map(this::leaf).toList())
		);
	}

	@Nonnull
	public SplitContentLayout hsplit(@Nonnull ContentLayout... children) {
		return split(Orientation.HORIZONTAL, children);
	}

	@Nonnull
	public SplitContentLayout hsplit(@Nonnull Content... children) {
		return split(new SplitContentArgs()
				.setOrientation(Orientation.HORIZONTAL)
				.addChildren(Arrays.stream(children).map(this::leaf).toList())
		);
	}

	@Nonnull
	public SplitContentLayout split(@Nonnull Orientation orientation,
	                                @Nonnull ContentLayout... children) {
		return split(new SplitContentArgs()
				.setOrientation(orientation)
				.addChildren(children));
	}

	@Nonnull
	public SplitContentLayout split(@Nonnull SplitContentArgs args) {
		ImplSplitContentLayout layout = new ImplSplitContentLayout(bento,
				args.getOrientation(),
				args.getChildren(),
				args.getIdentifier()
		);
		if (!args.isResizeWithParent())
			SplitPane.setResizableWithParent(layout.getBackingRegion(), false);
		return layout;
	}

	@Nonnull
	public LeafContentLayout leaf(@Nonnull Content content) {
		return leaf(new ContentLayoutArgs(), content);
	}

	@Nonnull
	public LeafContentLayout leaf(@Nonnull ContentLayoutArgs args, @Nonnull Content content) {
		ImplLeafContentLayout layout = new ImplLeafContentLayout(bento, content);
		if (!args.isResizeWithParent())
			SplitPane.setResizableWithParent(layout.getBackingRegion(), false);
		return layout;
	}

	@Nonnull
	public TabbedContent tabbed(@Nonnull Side side, @Nonnull Dockable... dockables) {
		return tabbed(new TabbedContentArgs()
				.setSide(side)
				.addDockables(dockables));
	}

	@Nonnull
	public TabbedContent tabbed(@Nonnull TabbedContentArgs args) {
		return new ImplTabbedContent(bento, args.getSide(), args.getDockables(), args.isAutoPruneWhenEmpty(), args.isCanSplit(), args.getIdentifier());
	}
}
