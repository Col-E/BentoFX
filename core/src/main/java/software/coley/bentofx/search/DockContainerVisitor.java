package software.coley.bentofx.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
	private DockContainer result;

	public DockContainerVisitor(@Nonnull Predicate<DockContainer> matcher) {
		this.matcher = matcher;
	}

	@Override
	public boolean visitBranch(@Nonnull DockContainerBranch container) {
		return visitContainer(container);
	}

	@Override
	public boolean visitLeaf(@Nonnull DockContainerLeaf container) {
		return visitContainer(container);
	}

	private boolean visitContainer(@Nonnull DockContainer container) {
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
