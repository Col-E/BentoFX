package software.coley.bentofx.persistence.testfixtures.provider;

import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.provider.LayoutCodecProvider;
import software.coley.bentofx.persistence.testfixtures.codec.DoNothingLayoutCodec;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configurable {@link LayoutCodecProvider} for provider-selection tests.
 *
 * <p>The creation count is atomic because the persistence API decides which thread
 * calls a provider, so the test making assertions using it is not the same thread that
 * incremented it.</p>
 *
 * @author Phil Bryant
 */
public final class ConfigurableLayoutCodecProvider extends AbstractConfigurableLayoutProvider implements LayoutCodecProvider {

	private final AtomicInteger createdCodecCount = new AtomicInteger();

	/**
	 * Creates a provider that reports the supplied identifier and default flag.
	 *
	 * @param identifier the identifier this provider answers to.
	 * @param defaultProvider whether this provider is the default one.
	 */
	public ConfigurableLayoutCodecProvider(
			final String identifier,
			final boolean defaultProvider
	) {
		super(identifier, defaultProvider);
	}

	@Override
	public LayoutCodec getLayoutCodec() {
		createdCodecCount.incrementAndGet();
		return new DoNothingLayoutCodec(getIdentifier());
	}

	/**
	 * {@return how many codecs this provider has been asked for.}
	 */
	public int getCreatedCodecCount() {
		return createdCodecCount.get();
	}
}
