package software.coley.bentofx.impl.layout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.impl.ImplBento;
import software.coley.bentofx.layout.DockLayout;
import software.coley.bentofx.layout.RootDockLayout;

public class ImplRootDockLayout extends BorderPane implements RootDockLayout {
	private final ImplBento bento;
	private final String identifier;
	private DockLayout layout;

	public ImplRootDockLayout(@Nonnull ImplBento bento, @Nonnull DockLayout layout, @Nonnull String identifier) {
		this.bento = bento;
		this.identifier = identifier;

		getStyleClass().add("bento");

		// Register/unregister this root based on when it belongs to the scene graph.
		sceneProperty().addListener((on, old, cur) -> {
			if (cur != null) {
				bento.registerRoot(this);
			} else {
				bento.unregisterRoot(this);
			}
		});

		setLayout(layout);
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

	@Nonnull
	@Override
	public Region getBackingRegion() {
		return this;
	}

	@Override
	public void setLayout(@Nonnull DockLayout layout) {
		this.layout = layout;
		setCenter(layout.getBackingRegion());
	}

	@Nonnull
	@Override
	public DockLayout getLayout() {
		return layout;
	}

	@Override
	public void clearLayout() {
		setLayout(new ImplLeafDockLayout(bento, null));
	}
}
