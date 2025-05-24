package software.coley.bentofx.builder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.bentofx.content.Content;

public class LeafContentLayoutArgs extends AbstractContentLayoutArgs<LeafContentLayoutArgs> {
	private Content content;

	@Nullable
	public Content getContent() {
		return content;
	}

	@Nonnull
	public LeafContentLayoutArgs setContent(@Nullable Content content) {
		this.content = content;
		return this;
	}
}
