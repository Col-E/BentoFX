package software.coley.bentofx.builder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import software.coley.bentofx.Dockable;
import software.coley.bentofx.DockableCloseListener;
import software.coley.bentofx.IconFactory;
import software.coley.bentofx.MenuFactory;
import software.coley.bentofx.impl.ImplBento;
import software.coley.bentofx.impl.ImplDockable;
import software.coley.bentofx.util.BentoUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DockableBuilder {
	private final ImplBento bento;
	private final StringProperty titleProperty = new SimpleStringProperty();
	private final ObjectProperty<Tooltip> tooltipProperty = new SimpleObjectProperty<>();
	private final ObjectProperty<IconFactory> iconFactoryProperty = new SimpleObjectProperty<>();
	private final BooleanProperty closableProperty = new SimpleBooleanProperty(true);
	private final BooleanProperty canBeDraggedProperty = new SimpleBooleanProperty(true);
	private final BooleanProperty canBeDroppedToNewWindowProperty = new SimpleBooleanProperty(true);
	private final ObjectProperty<MenuFactory> contextMenuFactoryProperty = new SimpleObjectProperty<>();
	private final BooleanProperty cachedContextMenuProperty = new SimpleBooleanProperty();
	private List<DockableCloseListener> closeListeners;
	private String identifier = BentoUtils.newIdentifier();
	private int dragGroup;
	private Node content;

	public DockableBuilder(@Nonnull ImplBento bento) {
		this.bento = bento;
	}

	@Nonnull
	public DockableBuilder withIdentifier(@Nullable String identifier) {
		this.identifier = identifier;
		return this;
	}

	@Nonnull
	public DockableBuilder withDragGroup(@Nullable int dragGroup) {
		this.dragGroup = dragGroup;
		return this;
	}

	@Nonnull
	public DockableBuilder withContent(@Nullable Node content) {
		this.content = content;
		return this;
	}

	@Nonnull
	public DockableBuilder withTitle(@Nullable String title) {
		titleProperty.set(title);
		return this;
	}

	@Nonnull
	public DockableBuilder withTooltip(@Nullable Tooltip tooltip) {
		tooltipProperty.set(tooltip);
		return this;
	}

	@Nonnull
	public DockableBuilder withIconFactory(@Nullable IconFactory factory) {
		iconFactoryProperty.set(factory);
		return this;
	}

	@Nonnull
	public DockableBuilder withContextMenuFactory(@Nullable MenuFactory factory) {
		contextMenuFactoryProperty.set(factory);
		return this;
	}

	@Nonnull
	public DockableBuilder withCachedContextMenu(boolean cached) {
		cachedContextMenuProperty.set(cached);
		return this;
	}

	@Nonnull
	public DockableBuilder withClosable(boolean closable) {
		closableProperty.set(closable);
		return this;
	}

	@Nonnull
	public DockableBuilder withCloseListener(@Nullable DockableCloseListener listener) {
		if (closeListeners == null)
			closeListeners = new ArrayList<>(3);
		closeListeners.add(listener);
		return this;
	}

	@Nonnull
	public DockableBuilder withCanBeDragged(boolean draggable) {
		canBeDraggedProperty.set(draggable);
		return this;
	}

	@Nonnull
	public DockableBuilder withCanBeDroppedToNewWindow(boolean droppable) {
		canBeDroppedToNewWindowProperty.set(droppable);
		return this;
	}

	@Nonnull
	public StringProperty titleProperty() {
		return titleProperty;
	}

	@Nonnull
	public ObjectProperty<Tooltip> tooltipProperty() {
		return tooltipProperty;
	}

	@Nonnull
	public ObjectProperty<IconFactory> iconFactoryProperty() {
		return iconFactoryProperty;
	}

	@Nonnull
	public BooleanProperty closableProperty() {
		return closableProperty;
	}

	@Nonnull
	public BooleanProperty canBeDraggedProperty() {
		return canBeDraggedProperty;
	}

	@Nonnull
	public BooleanProperty canBeDroppedToNewWindowProperty() {
		return canBeDroppedToNewWindowProperty;
	}

	@Nonnull
	public ObjectProperty<MenuFactory> contextMenuFactoryProperty() {
		return contextMenuFactoryProperty;
	}

	@Nonnull
	public BooleanProperty cachedContextMenuProperty() {
		return cachedContextMenuProperty;
	}

	@Nonnull
	public List<DockableCloseListener> getCloseListeners() {
		if (closeListeners == null)
			return Collections.emptyList();
		return closeListeners;
	}

	public String getIdentifier() {
		// Effectively non-null but only enforced in 'build()'
		return identifier;
	}

	public Node getContent() {
		// Effectively non-null but only enforced in 'build()'
		return content;
	}

	public int getDragGroup() {
		return dragGroup;
	}

	@Nonnull
	public Dockable build() {
		if (identifier == null)
			throw new IllegalArgumentException("Missing identifier");
		if (content == null)
			throw new IllegalArgumentException("Missing content");
		return new ImplDockable(this);
	}
}
