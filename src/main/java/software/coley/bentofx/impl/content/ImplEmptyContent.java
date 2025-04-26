package software.coley.bentofx.impl.content;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.content.EmptyContent;

public class ImplEmptyContent extends BorderPane implements EmptyContent {
	private final String identifier;

	public ImplEmptyContent(@Nonnull String identifier, @Nonnull Node display) {
		this.identifier = identifier;

		setCenter(display);
	}

	@Nonnull
	@Override
	public Region getBackingRegion() {
		return this;
	}

	@Nonnull
	@Override
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public boolean matchesIdentity(@Nullable Identifiable other) {
		if (other == null) return false;
		return identifier.equals(other.getIdentifier());
	}
}
