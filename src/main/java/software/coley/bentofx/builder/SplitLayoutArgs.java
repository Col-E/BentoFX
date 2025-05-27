package software.coley.bentofx.builder;

import jakarta.annotation.Nonnull;
import javafx.geometry.Orientation;
import software.coley.bentofx.layout.DockLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SplitLayoutArgs extends AbstractLayoutArgs<SplitLayoutArgs> {
	private final List<DockLayout> children = new ArrayList<>();
	private final List<Double> childrenSizes = new ArrayList<>();
	private final List<Double> childrenPercentages = new ArrayList<>();
	private Orientation orientation = Orientation.HORIZONTAL;

	@Nonnull
	public Orientation getOrientation() {
		return orientation;
	}

	@Nonnull
	public SplitLayoutArgs setOrientation(@Nonnull Orientation orientation) {
		this.orientation = orientation;
		return this;
	}

	@Nonnull
	public List<DockLayout> getChildren() {
		return children;
	}

	@Nonnull
	public SplitLayoutArgs addChildren(@Nonnull DockLayout... children) {
		return addChildren(Arrays.asList(children));
	}

	@Nonnull
	public SplitLayoutArgs addChildren(@Nonnull List<? extends DockLayout> children) {
		this.children.addAll(children);
		return this;
	}

	@Nonnull
	public List<Double> getChildrenSizes() {
		return childrenSizes;
	}

	@Nonnull
	public SplitLayoutArgs setChildrenSizes(double... sizes) {
		childrenSizes.clear();
		for (double size : sizes)
			childrenSizes.add(size);
		return this;
	}

	@Nonnull
	public List<Double> getChildrenPercentages() {
		return childrenPercentages;
	}

	@Nonnull
	public SplitLayoutArgs setChildrenPercentages(double... percentages) {
		childrenPercentages.clear();
		for (double percentage : percentages)
			childrenPercentages.add(percentage);
		return this;
	}
}
