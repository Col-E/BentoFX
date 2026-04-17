package software.coley.bentofx.dockable;

import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.path.DockablePath;

/**
 * Listener that is invoked when a {@link Dockable} is added to a {@link DockContainer}.
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
	void onOpen(DockablePath path, Dockable dockable);
}
