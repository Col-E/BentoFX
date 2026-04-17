package software.coley.bentofx.dockable;

import javafx.scene.Node;

/**
 * Factory to create a {@link Node} placeholder display for some given {@link Dockable}.
 * Implementations should create <b>NEW</b> instances for <b>EACH</b> call.
 *
 * @author Matt Coley
 */
public interface DockablePlaceholderFactory {
	/**
	 * @param dockable
	 * 		Dockable to create a placeholder display for.
	 *
	 * @return Placeholder for the dockable.
	 */
	Node build(Dockable dockable);
}
