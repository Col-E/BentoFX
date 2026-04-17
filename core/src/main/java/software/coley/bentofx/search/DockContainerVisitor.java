package software.coley.bentofx.search;

import org.jspecify.annotations.Nullable;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;

import java.util.function.Predicate;

/**
 * Search visitor that yields the first matching {@link DockContainer} of some predicate.
 *
 * @author Matt Coley
 */
public class DockContainerVisitor implements SearchVisitor {
	private final Predicate<DockContainer> matcher;
	private @Nullable DockContainer result;

	public DockContainerVisitor(Predicate<DockContainer> matcher) {
		this.matcher = matcher;
	}

	@Override
	public boolean visitBranch(DockContainerBranch container) {
		return visitContainer(container);
	}

	@Override
	public boolean visitLeaf(DockContainerLeaf container) {
		return visitContainer(container);
	}

	private boolean visitContainer(DockContainer container) {
		if (matcher.test(container)) {
			// Match found, stop visiting.
			result = container;
			return false;
		}
		return true;
	}


	/**
	 * @return Matched container if found.
	 */
	@Nullable
	public DockContainer getMatchedContainer() {
		return result;
	}
}
