package software.coley.bentofx.builder;

import jakarta.annotation.Nonnull;

public class AbstractLayoutArgs<T extends AbstractLayoutArgs<T>> extends AbstractArgs<T> {
	private boolean resizeWithParent = true;

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
