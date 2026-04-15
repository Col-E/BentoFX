package software.coley.bentofx.dockable;

import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.path.DockablePath;

/**
 * Listener invoked when a {@link Dockable} is removed from a {@link DockContainer} with the intent to close it.
 *
 * @author Matt Coley
 */
public interface DockableCloseListener {
	/**
	 * @param path
	 * 		Path to dockable prior to closure.
	 * @param dockable
	 * 		Closed dockable.
	 */
	void onClose(DockablePath path, Dockable dockable);
}
