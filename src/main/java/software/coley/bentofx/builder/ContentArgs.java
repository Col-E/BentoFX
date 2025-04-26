package software.coley.bentofx.builder;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.util.BentoUtils;

public abstract class ContentArgs<T extends ContentArgs<T>> {
	private String identifier = BentoUtils.newIdentifier();
	private boolean resizeWithParent = true;

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

	public boolean isResizeWithParent() {
		return resizeWithParent;
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	public T setResizeWithParent(boolean resizeWithParent) {
		this.resizeWithParent = resizeWithParent;
		return (T) this;
	}
}
