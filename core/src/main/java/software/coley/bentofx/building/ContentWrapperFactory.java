package software.coley.bentofx.building;

import org.jspecify.annotations.NonNull;
import software.coley.bentofx.control.ContentWrapper;
import software.coley.bentofx.control.HeaderPane;
import software.coley.bentofx.layout.container.DockContainerLeaf;

/**
 * Factory for building {@link ContentWrapper} in a parent {@link HeaderPane}.
 *
 * @author Matt Coley
 */
public interface ContentWrapperFactory {
	/**
	 * @param container
	 * 		Parent container.
	 *
	 * @return Newly created content wrapper.
	 */
	@NonNull
	ContentWrapper newContentWrapper(@NonNull DockContainerLeaf container);
}
