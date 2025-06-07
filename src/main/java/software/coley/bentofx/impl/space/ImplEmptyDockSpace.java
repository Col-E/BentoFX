package software.coley.bentofx.impl.space;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.space.EmptyDockSpace;

public class ImplEmptyDockSpace extends BorderPane implements EmptyDockSpace {
	private final String identifier;

	public ImplEmptyDockSpace(@Nonnull String identifier, @Nonnull Node display) {
		this.identifier = identifier;

		getStyleClass().addAll("space", "space-empty");

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

	@Override
	public String toString() {
		return "Empty: " + getIdentifier();
	}
}
