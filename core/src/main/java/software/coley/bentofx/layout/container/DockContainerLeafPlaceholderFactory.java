package software.coley.bentofx.layout.container;

import javafx.scene.Node;
import org.jspecify.annotations.NonNull;

/**
 * Factory to create a {@link Node} placeholder display for some given {@link DockContainerLeaf}.
 * Implementations should create <b>NEW</b> instances for <b>EACH</b> call.
 *
 * @author Matt Coley
 */
public interface DockContainerLeafPlaceholderFactory {
	/**
	 * @param container
	 * 		Container to create a placeholder display for.
	 *
	 * @return Placeholder for the container.
	 */
	@NonNull
	Node build(@NonNull DockContainerLeaf container);
}
