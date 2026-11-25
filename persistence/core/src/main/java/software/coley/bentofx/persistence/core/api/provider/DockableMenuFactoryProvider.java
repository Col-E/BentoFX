package software.coley.bentofx.persistence.core.api.provider;

import software.coley.bentofx.dockable.DockableMenuFactory;

import java.util.Optional;

/**
 * Resolves {@link DockableMenuFactory} instances by identifier.
 *
 * <p>Offered for applications to implement; nothing in the persistence framework
 * consumes it. Unlike its siblings here it is not even a parameter - no
 * {@link DockingLayoutPersistenceProvider} method accepts one. It exists only so
 * that a {@link DockableStateProvider} implementation has somewhere to look up the
 * menu factory it attaches to a {@code Dockable} it rebuilds. If the application
 * does not call it, nothing will.</p>
 *
 * <p>A restored dockable takes its context menu from the factory carried on the
 * {@link software.coley.bentofx.persistence.core.api.state.DockableState}, so an
 * application is free to set that factory directly and never implement this
 * interface at all.</p>
 *
 * @author Phil Bryant
 */
public interface DockableMenuFactoryProvider {

	/**
	 * {@return an {@code Optional} holding the {@link DockableMenuFactory} for the
	 * given identifier.} Implementations should return an empty {@code Optional}
	 * when no context menu factory is available for the identifier.
	 *
	 * @param identifier the identifier of the {@link DockableMenuFactory}
	 * to be returned.
	 */
	Optional<DockableMenuFactory> getDockableMenuFactory(String identifier);
}
