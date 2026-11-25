package software.coley.bentofx.persistence.core.impl.provider;

import software.coley.bentofx.Bento;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link BentoProvider} that merely collects and
 * retrieves {@code Bento} instances using a weak reference their identifiers.
 *
 * @author Phil Bryant
 */
public class DefaultBentoProvider implements BentoProvider {

	private final Map<String, WeakReference<Bento>> bentoMap =
			new ConcurrentHashMap<>();

	/**
	 * Constructs a provider holding no {@code Bento}s. Add them with
	 * {@link #addBento(Bento)}.
	 */
	public DefaultBentoProvider() {
	}

	/**
	 * Constructor.
	 * @param bentos the {@code Bento}s to collect.
	 */
	public DefaultBentoProvider(final Bento... bentos) {
		for(final Bento bento : bentos) {
			addBento(bento);
		}
	}

	/**
	 * Collects a {@code Bento}, keyed on its identifier.
	 *
	 * <p>A {@code Bento} whose identifier is already held replaces the existing
	 * entry, since the identifier is what gives a {@code Bento} its identity
	 * here.</p>
	 *
	 * @param bento the {@code Bento} to collect.
	 */
	public void addBento(final Bento bento) {
		bentoMap.put(
				bento.getIdentifier(),
				new WeakReference<>(bento)
		);
	}

	@Override
	public Optional<Bento> getBento(String identifier) {
		final WeakReference<Bento> bentoReference = bentoMap.get(identifier);
		return Optional.ofNullable(
				bentoReference == null ? null : bentoReference.get()
		);
	}

	@Override
	public Collection<Bento> getAllBentos() {
		return bentoMap.values().stream()
				.map(WeakReference::get)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
