package software.coley.bentofx.persistence.core.api.provider;

import software.coley.bentofx.layout.container.DockContainerLeafMenuFactory;

import java.util.Optional;


/**
 * Supplies the {@link DockContainerLeafMenuFactory} for a restored
 * {@code DockContainerLeaf}.
 *
 * <p>Implemented and supplied by the application, which passes an instance to
 * {@link DockingLayoutPersistenceProvider}'s {@code getLayoutRestorer}. It is
 * optional - passing {@code null} there leaves restored leaves without a
 * menu.</p>
 *
 * @author Phil Bryant
 */
public interface DockContainerLeafMenuFactoryProvider {

	/**
	 * {@return an {@link Optional} containing the
	 * {@link DockContainerLeafMenuFactory} for the identifier, an empty
	 * {@code Optional} when no {@link DockContainerLeafMenuFactory} is
	 * available for the identifier.}
	 */
	Optional<DockContainerLeafMenuFactory> getDockContainerLeafMenuFactory(
			final String dockContainerLeafIdentifier
	);
}
