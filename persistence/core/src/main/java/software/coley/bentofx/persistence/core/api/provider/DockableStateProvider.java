package software.coley.bentofx.persistence.core.api.provider;

import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.persistence.core.api.state.DockableState;

import java.util.Optional;

/**
 * Gets or creates the {@link DockableState} instances, and the user interface
 * components they carry, for a layout being restored.
 *
 * <p>Implemented and supplied by the application, which passes an instance to
 * {@link DockingLayoutPersistenceProvider}'s {@code getLayoutRestorer}. A
 * persisted layout records which {@link Dockable}s were open, not what was inside
 * them, so rebuilding their content is necessarily the application's job.</p>
 *
 * @author Phil Bryant
 */
public interface DockableStateProvider {

	/**
	 * {@return an {@link Optional} containing the {@link DockableState} with the
	 * given identifier, {@code null} when no {@link DockableState} with the
	 * identifier can be found.}
	 * @param id the identifier of the {@link Dockable} to be returned.
	 */
	Optional<DockableState> resolveDockableState(String id);
}
