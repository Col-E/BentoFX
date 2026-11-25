package software.coley.bentofx.persistence.core.api.provider;

import software.coley.bentofx.Bento;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;

import java.util.Collection;
import java.util.Optional;

/**
 * Resolves the {@link Bento} instances whose layouts are saved and restored.
 *
 * <p>Supplied by the application, which passes an instance to
 * {@link DockingLayoutPersistenceProvider}'s {@code getLayoutSaver} and
 * {@code getLayoutRestorer}. This is a registry of live objects, so the
 * application has to hand over the instance it populated - only the application
 * knows how long its {@link Bento}s live and how they are keyed, which is why the
 * framework asks for one rather than supplying one.</p>
 *
 * <p>When the set of {@link Bento}s is known up front, {@link #of(Bento...)} saves
 * writing an implementation. Implement this interface directly when they are
 * created, replaced or discarded while the application runs.</p>
 *
 * @author Phil Bryant
 */
public interface BentoProvider {

	/**
	 * {@return a {@link BentoProvider} holding the given {@link Bento}s, keyed on
	 * their identifiers.}
	 *
	 * <p>For the common case of a fixed set of {@link Bento}s known at start-up.
	 * The result is not mutable through this interface, so an application that adds
	 * or removes {@link Bento}s later should implement {@link BentoProvider} itself
	 * rather than call this.</p>
	 *
	 * <p><b>The caller must keep its own reference to each {@link Bento}.</b> They
	 * are held weakly here, so a {@link Bento} the application no longer references
	 * becomes collectable and drops out of the saved layout without warning.
	 * Passing a freshly constructed {@link Bento} straight into this method and
	 * keeping no field for it is therefore a mistake.</p>
	 *
	 * @param bentos the {@link Bento}s whose layouts are to be saved and restored.
	 */
	static BentoProvider of(final Bento... bentos) {
		return new DefaultBentoProvider(bentos);
	}

	/**
	 * {@return an {@link Optional<Bento>} with the given identifier.}
	 * Implementations should return an empty {@link Optional<Bento>} when a
	 * {@link Bento} with the specified identifier cannot be found.
	 *
	 * @param identifier the identifier of the {@link Bento} to be returned.
	 */
	Optional<Bento> getBento(String identifier);

	/**
	 * {@return a {@link Collection} of all {@link Bento} whose layouts are to be
	 * saved and restored.}
	 */
	Collection<Bento> getAllBentos();
}
