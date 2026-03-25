package software.coley.bentofx.dockable;

import org.jspecify.annotations.NonNull;
import software.coley.bentofx.path.DockablePath;

/**
 * Listener that is invoked when a {@link Dockable} is selected.
 *
 * @author Matt Coley
 */
public interface DockableSelectListener {
	/**
	 * @param path
	 * 		Path to selected dockable.
	 * @param dockable
	 * 		Selected dockable.
	 */
	void onSelect(@NonNull DockablePath path, @NonNull Dockable dockable);
}
