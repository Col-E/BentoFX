package software.coley.bentofx.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.builder.DockableBuilder;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableCloseListener;
import software.coley.bentofx.dockable.DockableIconFactory;
import software.coley.bentofx.dockable.DockableMenuFactory;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.path.DockablePath;

import java.util.ArrayList;
import java.util.List;

public class ImplDockable implements Dockable {
	private final String identifier;
	private final int dragGroup;
	private final ObjectProperty<Node> nodeProperty;
	private final StringProperty titleProperty;
	private final ObjectProperty<Tooltip> tooltipProperty;
	private final ObjectProperty<DockableIconFactory> iconFactoryProperty;
	private final BooleanProperty closableProperty;
	private final BooleanProperty canBeDragged;
	private final BooleanProperty canBeDroppedToNewWindow;
	private final ObjectProperty<DockableMenuFactory> contextMenuFactoryProperty;
	private final BooleanProperty cachedContextMenuProperty;
	private List<DockableCloseListener> closeListeners;
	private DockablePath priorPath;

	public ImplDockable(@Nonnull DockableBuilder builder) {
		identifier = builder.getIdentifier();
		nodeProperty = builder.nodeProperty();
		titleProperty = builder.titleProperty();
		tooltipProperty = builder.tooltipProperty();
		iconFactoryProperty = builder.iconFactoryProperty();
		closableProperty = builder.closableProperty();
		canBeDragged = builder.canBeDraggedProperty();
		canBeDroppedToNewWindow = builder.canBeDroppedToNewWindowProperty();
		contextMenuFactoryProperty = builder.contextMenuFactoryProperty();
		cachedContextMenuProperty = builder.cachedContextMenuProperty();
		dragGroup = builder.getDragGroup();
		for (DockableCloseListener listener : builder.getCloseListeners())
			withCloseListener(listener);
	}

	/**
	 * Called when this dockable is removed from the scene with intent of permanent removal.
	 */
	public void onClose(@Nonnull DockablePath path) {
		if (closeListeners != null) {
			for (DockableCloseListener listener : closeListeners)
				listener.onClose(path, this);

			// Clear so that any repeated calls do not re-trigger listeners.
			closeListeners = null;
		}
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
	public ObjectProperty<Node> nodeProperty() {
		return nodeProperty;
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
	public ObjectProperty<DockableIconFactory> iconFactoryProperty() {
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
	public ObjectProperty<DockableMenuFactory> contextMenuFactoryProperty() {
		return contextMenuFactoryProperty;
	}

	@Nonnull
	@Override
	public BooleanProperty cachedContextMenuProperty() {
		return cachedContextMenuProperty;
	}

	@Nonnull
	@Override
	public Dockable withCloseListener(@Nullable DockableCloseListener listener) {
		if (closeListeners == null)
			closeListeners = new ArrayList<>(3);
		closeListeners.add(listener);
		return this;
	}

	/**
	 * When {@link Header#removeFromParent(Header.RemovalReason)} is called with {@link Header.RemovalReason#MOVING}
	 * this value gets set to the then-current path. Later when we observe this dockable being added someplace
	 * we check if this value exists. If it does, this dockable has been moved rather than being freshly opened.
	 *
	 * @return Prior path of this dockable.
	 */
	@Nullable
	public DockablePath getPriorPath() {
		return priorPath;
	}

	/**
	 * Set the last path this dockable was located at.
	 *
	 * @param path
	 * 		Prior path of this dockable.
	 *
	 * @see #getPriorPath()
	 */
	public void setPriorPath(@Nullable DockablePath path) {
		priorPath = path;
	}
}
