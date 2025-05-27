package software.coley.bentofx.builder;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.util.BentoUtils;

public abstract class AbstractArgs<T extends AbstractArgs<T>> {
	private String identifier = BentoUtils.newIdentifier();

	@Nonnull
	public String getIdentifier() {
		return identifier;
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	public T setIdentifier(@Nonnull String identifier) {
		this.identifier = identifier;
		return (T) this;
	}
}
