package software.coley.bentofx.search;

import org.jspecify.annotations.Nullable;
import software.coley.bentofx.dockable.Dockable;

import java.util.function.Predicate;

/**
 * Search visitor that yields the first matching {@link Dockable} of some predicate.
 *
 * @author Matt Coley
 */
public class DockableVisitor implements SearchVisitor {
	private final Predicate<Dockable> matcher;
	private @Nullable Dockable result;

	/**
	 * @param matcher
	 * 		Dockable predicate.
	 */
	public DockableVisitor(Predicate<Dockable> matcher) {
		this.matcher = matcher;
	}

	@Override
	public boolean visitDockable(Dockable dockable) {
		if (matcher.test(dockable)) {
			// Match found, stop visiting.
			result = dockable;
			return false;
		}
		return true;
	}

	/**
	 * @return Matched dockable if found.
	 */
	@Nullable
	public Dockable getMatchedDockable() {
		return result;
	}
}
