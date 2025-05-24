package software.coley.bentofx.builder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.control.SplitPane;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.content.SingleContent;
import software.coley.bentofx.content.TabbedContent;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.impl.ImplBento;
import software.coley.bentofx.impl.content.ImplSingleContent;
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
import java.util.List;

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
		return split(new SplitContentLayoutArgs()
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
		return split(new SplitContentLayoutArgs()
				.setOrientation(Orientation.HORIZONTAL)
				.addChildren(Arrays.stream(children).map(this::leaf).toList())
		);
	}

	@Nonnull
	public SplitContentLayout split(@Nonnull Orientation orientation,
	                                @Nonnull ContentLayout... children) {
		return split(new SplitContentLayoutArgs()
				.setOrientation(orientation)
				.addChildren(children));
	}

	@Nonnull
	public SplitContentLayout split(@Nonnull SplitContentLayoutArgs args) {
		List<ContentLayout> children = args.getChildren();
		ImplSplitContentLayout layout = new ImplSplitContentLayout(bento,
				args.getOrientation(),
				children,
				args.getIdentifier()
		);
		if (!args.isResizeWithParent())
			SplitPane.setResizableWithParent(layout.getBackingRegion(), false);
		List<Double> childrenSizes = args.getChildrenSizes();
		List<Double> childrenPercentages = args.getChildrenPercentages();
		if (!childrenSizes.isEmpty()) {
			for (int i = 0; i < Math.min(children.size(), childrenSizes.size()); i++) {
				double size = childrenSizes.get(i);
				if (size < 0)
					continue;
				ContentLayout child = children.get(i);
				layout.setChildSize(child, size);
			}
		} else if (!childrenPercentages.isEmpty()) {
			for (int i = 0; i < Math.min(children.size(), childrenPercentages.size()); i++) {
				double percent = childrenPercentages.get(i);
				if (percent < 0)
					continue;
				ContentLayout child = children.get(i);
				layout.setChildPercent(child, percent);
			}
		}
		return layout;
	}

	@Nonnull
	public LeafContentLayout leaf(@Nullable Content content) {
		return leaf(content, true);
	}

	@Nonnull
	public LeafContentLayout fitLeaf(@Nullable Content content) {
		return leaf(content, false);
	}

	@Nonnull
	public LeafContentLayout leaf(@Nullable Content content, boolean resizeWithParent) {
		return leaf(new LeafContentLayoutArgs().setContent(content).setResizeWithParent(resizeWithParent));
	}

	@Nonnull
	public LeafContentLayout leaf(@Nonnull LeafContentLayoutArgs args) {
		ImplLeafContentLayout layout = new ImplLeafContentLayout(bento, args.getContent());
		if (!args.isResizeWithParent())
			SplitPane.setResizableWithParent(layout.getBackingRegion(), false);
		return layout;
	}

	@Nonnull
	public SingleContent single(@Nonnull SingleContentArgs args) {
		Dockable dockable = args.getDockable();
		if (dockable == null)
			throw new IllegalArgumentException("Single content requires a dockable");
		return new ImplSingleContent(bento, dockable, args.getSide(), args.getIdentifier());
	}

	@Nonnull
	public TabbedContent tabbed(@Nonnull Side side, @Nonnull Dockable... dockables) {
		return tabbed(new TabbedContentArgs()
				.setSide(side)
				.addDockables(dockables));
	}

	@Nonnull
	public TabbedContent tabbed(@Nonnull TabbedContentArgs args) {
		return new ImplTabbedContent(bento, args);
	}
}
