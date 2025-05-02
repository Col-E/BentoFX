package software.coley.bentofx.builder;

import jakarta.annotation.Nonnull;
import javafx.geometry.Orientation;
import software.coley.bentofx.layout.ContentLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SplitContentLayoutArgs extends AbstractContentLayoutArgs<SplitContentLayoutArgs> {
	private final List<ContentLayout> children = new ArrayList<>();
	private final List<Double> childrenSizes = new ArrayList<>();
	private final List<Double> childrenPercentages = new ArrayList<>();
	private Orientation orientation = Orientation.HORIZONTAL;

	@Nonnull
	public Orientation getOrientation() {
		return orientation;
	}

	@Nonnull
	public SplitContentLayoutArgs setOrientation(@Nonnull Orientation orientation) {
		this.orientation = orientation;
		return this;
	}

	@Nonnull
	public List<ContentLayout> getChildren() {
		return children;
	}

	@Nonnull
	public SplitContentLayoutArgs addChildren(@Nonnull ContentLayout... children) {
		return addChildren(Arrays.asList(children));
	}

	@Nonnull
	public SplitContentLayoutArgs addChildren(@Nonnull List<? extends ContentLayout> children) {
		this.children.addAll(children);
		return this;
	}

	@Nonnull
	public List<Double> getChildrenSizes() {
		return childrenSizes;
	}

	@Nonnull
	public SplitContentLayoutArgs setChildrenSizes(double... sizes) {
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
	public SplitContentLayoutArgs setChildrenPercentages(double... percentages) {
		childrenPercentages.clear();
		for (double percentage : percentages)
			childrenPercentages.add(percentage);
		return this;
	}
}
