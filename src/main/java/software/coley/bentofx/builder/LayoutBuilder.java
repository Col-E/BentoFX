package software.coley.bentofx.builder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.control.SplitPane;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.impl.ImplBento;
import software.coley.bentofx.impl.layout.ImplLeafDockLayout;
import software.coley.bentofx.impl.layout.ImplRootDockLayout;
import software.coley.bentofx.impl.layout.ImplSplitDockLayout;
import software.coley.bentofx.impl.space.ImplSingleDockSpace;
import software.coley.bentofx.impl.space.ImplTabbedDockSpace;
import software.coley.bentofx.layout.DockLayout;
import software.coley.bentofx.layout.LeafDockLayout;
import software.coley.bentofx.layout.RootDockLayout;
import software.coley.bentofx.layout.SplitDockLayout;
import software.coley.bentofx.space.DockSpace;
import software.coley.bentofx.space.SingleDockSpace;
import software.coley.bentofx.space.TabbedDockSpace;
import software.coley.bentofx.util.BentoUtils;

import java.util.Arrays;
import java.util.List;

public class LayoutBuilder {
	private final ImplBento bento;

	public LayoutBuilder(@Nonnull ImplBento bento) {
		this.bento = bento;
	}

	@Nonnull
	public DockableBuilder dockable() {
		return bento.newDockableBuilder();
	}

	@Nonnull
	public RootDockLayout root(@Nonnull DockLayout child) {
		return root(child, BentoUtils.ID_PROVIDER.get());
	}

	@Nonnull
	public RootDockLayout root(@Nonnull DockLayout child, @Nonnull String identifier) {
		return new ImplRootDockLayout(bento, child, identifier);
	}

	@Nonnull
	public SplitDockLayout vsplit(@Nonnull DockLayout... children) {
		return split(Orientation.VERTICAL, children);
	}

	@Nonnull
	public SplitDockLayout vsplit(@Nonnull DockSpace... children) {
		return split(new SplitLayoutArgs()
				.setOrientation(Orientation.VERTICAL)
				.addChildren(Arrays.stream(children).map(this::leaf).toList())
		);
	}

	@Nonnull
	public SplitDockLayout hsplit(@Nonnull DockLayout... children) {
		return split(Orientation.HORIZONTAL, children);
	}

	@Nonnull
	public SplitDockLayout hsplit(@Nonnull DockSpace... children) {
		return split(new SplitLayoutArgs()
				.setOrientation(Orientation.HORIZONTAL)
				.addChildren(Arrays.stream(children).map(this::leaf).toList())
		);
	}

	@Nonnull
	public SplitDockLayout split(@Nonnull Orientation orientation,
	                             @Nonnull DockLayout... children) {
		return split(new SplitLayoutArgs()
				.setOrientation(orientation)
				.addChildren(children));
	}

	@Nonnull
	public SplitDockLayout split(@Nonnull SplitLayoutArgs args) {
		List<DockLayout> children = args.getChildren();
		ImplSplitDockLayout layout = new ImplSplitDockLayout(bento,
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
				DockLayout child = children.get(i);
				layout.setChildSize(child, size);
			}
		} else if (!childrenPercentages.isEmpty()) {
			for (int i = 0; i < Math.min(children.size(), childrenPercentages.size()); i++) {
				double percent = childrenPercentages.get(i);
				if (percent < 0)
					continue;
				DockLayout child = children.get(i);
				layout.setChildPercent(child, percent);
			}
		}
		return layout;
	}

	@Nonnull
	public LeafDockLayout leaf(@Nullable DockSpace space) {
		return leaf(space, true);
	}

	@Nonnull
	public LeafDockLayout fitLeaf(@Nullable DockSpace space) {
		return leaf(space, false);
	}

	@Nonnull
	public LeafDockLayout leaf(@Nullable DockSpace space, boolean resizeWithParent) {
		return leaf(new LeafLayoutArgs().setSpace(space).setResizeWithParent(resizeWithParent));
	}

	@Nonnull
	public LeafDockLayout leaf(@Nonnull LeafLayoutArgs args) {
		ImplLeafDockLayout layout = new ImplLeafDockLayout(bento, args.getSpace(), args.getIdentifier());
		if (!args.isResizeWithParent())
			SplitPane.setResizableWithParent(layout.getBackingRegion(), false);
		return layout;
	}

	@Nonnull
	public SingleDockSpace single(@Nonnull SingleSpaceArgs args) {
		Dockable dockable = args.getDockable();
		if (dockable == null)
			throw new IllegalArgumentException("Single space requires a dockable");
		return new ImplSingleDockSpace(bento, dockable, args.getSide(), args.getIdentifier());
	}

	@Nonnull
	public TabbedDockSpace tabbed(@Nonnull Side side, @Nonnull Dockable... dockables) {
		return tabbed(new TabbedSpaceArgs()
				.setSide(side)
				.addDockables(dockables));
	}

	@Nonnull
	public TabbedDockSpace tabbed(@Nonnull TabbedSpaceArgs args) {
		return new ImplTabbedDockSpace(bento, args);
	}
}
