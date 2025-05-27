package software.coley.bentofx.impl.space;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.space.SingleDockSpace;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.impl.ImplBento;

public class ImplSingleDockSpace extends ImplDockSpaceBase implements SingleDockSpace {
	private final ObjectProperty<Side> headerSideProperty = new SimpleObjectProperty<>();
	private final String identifier;
	private final Dockable dockable;

	public ImplSingleDockSpace(@Nonnull ImplBento bento, @Nonnull Dockable dockable, @Nullable Side headerSide, @Nonnull String identifier) {
		super(bento);

		this.dockable = dockable;
		this.identifier = identifier;

		headerSideProperty.set(headerSide);
		headerSideProperty.addListener((ob, old, cur) -> refreshHeader());
		layout.centerProperty().bind(dockable.nodeProperty());

		refreshHeader();
	}

	private void refreshHeader() {
		Side side = headerSideProperty.get();

		layout.setTop(null);
		layout.setBottom(null);
		layout.setLeft(null);
		layout.setRight(null);

		if (side != null) {
			Header value;
			switch (side) {
				case TOP -> {
					value = new Header(dockable, Side.TOP);
					layout.setTop(value);
				}
				case BOTTOM -> {
					value = new Header(dockable, Side.BOTTOM);
					layout.setBottom(value);
				}
				case LEFT -> {
					value = new Header(dockable, Side.LEFT);
					layout.setLeft(value);
				}
				case RIGHT -> {
					value = new Header(dockable, Side.RIGHT);
					layout.setRight(value);
				}
				default -> value = null;
			}
		}
	}

	@Nullable
	public Header getHeader() {
		Side side = headerSideProperty.get();
		if (side == null)
			return null;
		Node maybeHeader = switch (side) {
			case TOP -> layout.getTop();
			case BOTTOM -> layout.getBottom();
			case LEFT -> layout.getLeft();
			case RIGHT -> layout.getRight();
		};
		if (maybeHeader instanceof Header header)
			return header;
		return null;
	}

	@Nonnull
	@Override
	public Region getBackingRegion() {
		return this;
	}

	@Nonnull
	@Override
	public ObjectProperty<Side> headerSideProperty() {
		return headerSideProperty;
	}

	@Nonnull
	@Override
	public Dockable getDockable() {
		return dockable;
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
