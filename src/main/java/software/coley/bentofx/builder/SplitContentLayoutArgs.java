package software.coley.bentofx.builder;

import jakarta.annotation.Nonnull;
import javafx.geometry.Orientation;
import software.coley.bentofx.layout.ContentLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SplitContentLayoutArgs extends AbstractContentLayoutArgs<SplitContentLayoutArgs> {
	private final List<ContentLayout> children = new ArrayList<>();
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


}
