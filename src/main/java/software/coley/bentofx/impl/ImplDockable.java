package software.coley.bentofx.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import software.coley.bentofx.Dockable;
import software.coley.bentofx.IconFactory;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.MenuFactory;
import software.coley.bentofx.builder.DockableBuilder;

public class ImplDockable implements Dockable {
	private final String identifier;
	private final int dragGroup;
	private final Node content;
	private final StringProperty titleProperty;
	private final ObjectProperty<Tooltip> tooltipProperty;
	private final ObjectProperty<IconFactory> iconFactoryProperty;
	private final BooleanProperty closableProperty;
	private final BooleanProperty canBeDragged;
	private final BooleanProperty canBeDroppedToNewWindow;
	private final ObjectProperty<MenuFactory> contextMenuFactoryProperty;
	private final BooleanProperty cachedContextMenuProperty;

	public ImplDockable(@Nonnull DockableBuilder builder) {
		identifier = builder.getIdentifier();
		content = builder.getContent();
		titleProperty = builder.titleProperty();
		tooltipProperty = builder.tooltipProperty();
		iconFactoryProperty = builder.iconFactoryProperty();
		closableProperty = builder.closableProperty();
		canBeDragged = builder.canBeDraggedProperty();
		canBeDroppedToNewWindow = builder.canBeDroppedToNewWindowProperty();
		contextMenuFactoryProperty = builder.contextMenuFactoryProperty();
		cachedContextMenuProperty = builder.cachedContextMenuProperty();
		dragGroup = builder.getDragGroup();
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
	public Node getNode() {
		return content;
	}

	@Override
	public int getDragGroup() {
		return dragGroup;
	}

	@Nonnull
	@Override
	public StringProperty titleProperty() {
		return titleProperty;
	}

	@Nonnull
	@Override
	public ObjectProperty<Tooltip> tooltipProperty() {
		return tooltipProperty;
	}

	@Nonnull
	@Override
	public ObjectProperty<IconFactory> iconFactoryProperty() {
		return iconFactoryProperty;
	}

	@Nonnull
	@Override
	public BooleanProperty closableProperty() {
		return closableProperty;
	}

	@Nonnull
	@Override
	public ObservableBooleanValue canBeDragged() {
		return canBeDragged;
	}

	@Nonnull
	@Override
	public ObservableBooleanValue canBeDroppedToNewWindow() {
		return canBeDroppedToNewWindow;
	}

	@Nonnull
	@Override
	public ObjectProperty<MenuFactory> contextMenuFactoryProperty() {
		return contextMenuFactoryProperty;
	}

	@Nonnull
	@Override
	public BooleanProperty cachedContextMenuProperty() {
		return cachedContextMenuProperty;
	}
}
