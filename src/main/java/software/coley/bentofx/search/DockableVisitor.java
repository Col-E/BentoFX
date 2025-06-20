package software.coley.bentofx.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.bentofx.dockable.Dockable;

import java.util.function.Predicate;

/**
 * Search visitor that yields the first matching {@link Dockable} of some predicate.
 *
 * @author Matt Coley
 */
public class DockableVisitor implements SearchVisitor {
	private final Predicate<Dockable> matcher;
	private Dockable result;

	/**
	 * @param matcher
	 * 		Dockable predicate.
	 */
	public DockableVisitor(@Nonnull Predicate<Dockable> matcher) {
		this.matcher = matcher;
	}

	@Override
	public boolean visitDockable(@Nonnull Dockable dockable) {
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
