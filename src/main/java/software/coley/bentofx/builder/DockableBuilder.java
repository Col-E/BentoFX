package software.coley.bentofx.builder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableCloseListener;
import software.coley.bentofx.dockable.DockableIconFactory;
import software.coley.bentofx.dockable.DockableMenuFactory;
import software.coley.bentofx.impl.ImplBento;
import software.coley.bentofx.impl.ImplDockable;
import software.coley.bentofx.util.BentoUtils;
import software.coley.bentofx.util.ConstantIcon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DockableBuilder {
	private final ImplBento bento;
	private final ObjectProperty<Node> nodeProperty = new SimpleObjectProperty<>();
	private final StringProperty titleProperty = new SimpleStringProperty();
	private final ObjectProperty<Tooltip> tooltipProperty = new SimpleObjectProperty<>();
	private final ObjectProperty<DockableIconFactory> iconFactoryProperty = new SimpleObjectProperty<>();
	private final BooleanProperty closableProperty = new SimpleBooleanProperty(true);
	private final BooleanProperty canBeDraggedProperty = new SimpleBooleanProperty(true);
	private final BooleanProperty canBeDroppedToNewWindowProperty = new SimpleBooleanProperty(true);
	private final ObjectProperty<DockableMenuFactory> contextMenuFactoryProperty = new SimpleObjectProperty<>();
	private final BooleanProperty cachedContextMenuProperty = new SimpleBooleanProperty();
	private List<DockableCloseListener> closeListeners;
	private String identifier = BentoUtils.newIdentifier();
	private int dragGroup;

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
	public DockableBuilder withNode(@Nullable Node node) {
		this.nodeProperty.set(node);
		return this;
	}

	@Nonnull
	public DockableBuilder withNode(@Nonnull ObservableValue<Node> nodeValue) {
		this.nodeProperty.bind(nodeValue);
		return this;
	}

	@Nonnull
	public DockableBuilder withTitle(@Nullable String title) {
		titleProperty.set(title);
		return this;
	}

	@Nonnull
	public DockableBuilder withTitle(@Nonnull ObservableValue<String> title) {
		titleProperty.bind(title);
		return this;
	}

	@Nonnull
	public DockableBuilder withTooltip(@Nullable Tooltip tooltip) {
		tooltipProperty.set(tooltip);
		return this;
	}

	@Nonnull
	public DockableBuilder withTooltip(@Nonnull ObservableValue<Tooltip> tooltip) {
		tooltipProperty.bind(tooltip);
		return this;
	}

	@Nonnull
	public DockableBuilder withIcon(@Nullable Node icon) {
		if (icon == null)
			iconFactoryProperty.set(null);
		else
			iconFactoryProperty.set(new ConstantIcon(icon));
		return this;
	}

	@Nonnull
	public DockableBuilder withIconFactory(@Nullable DockableIconFactory factory) {
		iconFactoryProperty.set(factory);
		return this;
	}

	@Nonnull
	public DockableBuilder withIconFactory(@Nonnull ObservableValue<DockableIconFactory> factory) {
		iconFactoryProperty.bind(factory);
		return this;
	}

	@Nonnull
	public DockableBuilder withContextMenuFactory(@Nullable DockableMenuFactory factory) {
		contextMenuFactoryProperty.set(factory);
		return this;
	}

	@Nonnull
	public DockableBuilder withContextMenuFactory(@Nonnull ObservableValue<DockableMenuFactory> factory) {
		contextMenuFactoryProperty.bind(factory);
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
	public DockableBuilder withClosable(@Nonnull ObservableValue<Boolean> closable) {
		closableProperty.bind(closable);
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
	public DockableBuilder withCanBeDragged(@Nonnull ObservableValue<Boolean> draggable) {
		canBeDraggedProperty.bind(draggable);
		return this;
	}

	@Nonnull
	public DockableBuilder withCanBeDroppedToNewWindow(boolean droppable) {
		canBeDroppedToNewWindowProperty.set(droppable);
		return this;
	}

	@Nonnull
	public DockableBuilder withCanBeDroppedToNewWindow(@Nonnull ObservableValue<Boolean> droppable) {
		canBeDroppedToNewWindowProperty.bind(droppable);
		return this;
	}

	@Nonnull
	public ObjectProperty<Node> nodeProperty() {
		return nodeProperty;
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
	public ObjectProperty<DockableIconFactory> iconFactoryProperty() {
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
	public ObjectProperty<DockableMenuFactory> contextMenuFactoryProperty() {
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

	public int getDragGroup() {
		return dragGroup;
	}

	@Nonnull
	public Dockable build() {
		if (identifier == null)
			throw new IllegalArgumentException("Missing identifier");
		return new ImplDockable(this);
	}
}
