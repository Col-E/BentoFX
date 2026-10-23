package software.coley.bentofx.persistence.testfixtures.provider;

import software.coley.bentofx.persistence.core.api.provider.LayoutPersistenceComponentProvider;

/**
 * Shared configurable provider metadata for persistence provider tests.
 *
 * <p>Declares {@link LayoutPersistenceComponentProvider} rather than supplying its
 * two methods by coincidence of name, so that a change to that interface fails here
 * instead of in whichever subclass was relying on these signatures.</p>
 *
 * @author Phil Bryant
 */
public abstract class AbstractConfigurableLayoutProvider
		implements LayoutPersistenceComponentProvider {

	private final String identifier;
	private final boolean defaultProvider;

	/**
	 * Creates a provider that reports the supplied identifier and default flag.
	 *
	 * @param identifier the identifier this provider answers to.
	 * @param defaultProvider whether this provider is the default one.
	 */
	protected AbstractConfigurableLayoutProvider(
			final String identifier,
			final boolean defaultProvider
	) {
		this.identifier = identifier;
		this.defaultProvider = defaultProvider;
	}

	@Override
	public final String getIdentifier() {
		return identifier;
	}

	@Override
	public final boolean isDefault() {
		return defaultProvider;
	}
}
