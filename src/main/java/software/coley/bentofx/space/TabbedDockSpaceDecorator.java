package software.coley.bentofx.space;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.Bento;

/**
 * Decorator for created {@link TabbedDockSpace} instances.
 *
 * @author Matt Coley
 * @see Bento#setTabbedDockSpaceDecorator(TabbedDockSpaceDecorator)
 */
public interface TabbedDockSpaceDecorator {
	/**
	 * @param space
	 * 		Newly created tabbed dock space.
	 */
	void decorate(@Nonnull TabbedDockSpace space);
}
