package software.coley.bentofx.dockable;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.space.DockSpace;
import software.coley.bentofx.path.DockablePath;

/**
 * Listener that is invoked when a {@link Dockable} is added to a {@link DockSpace}.
 *
 * @author Matt Coley
 */
public interface DockableOpenListener {
	/**
	 * @param path
	 * 		Path to opened dockable.
	 * @param dockable
	 * 		Closed dockable.
	 */
	void onOpen(@Nonnull DockablePath path, @Nonnull Dockable dockable);
}
