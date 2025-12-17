package software.coley.bentofx.dockable;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import software.coley.bentofx.Bento;
import software.coley.bentofx.BentoBacked;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.event.DockEvent;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.path.DockablePath;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Outline of some item to display.
 *
 * @author Matt Coley
 */
public class Dockable implements BentoBacked, Identifiable {
	private final Bento bento;
	private final String identifier;
	private StringProperty title;
	private ObjectProperty<Tooltip> tooltip;
	private ObjectProperty<DockableIconFactory> iconFactory;
	private ObjectProperty<DockableMenuFactory> contextMenuFactory;
	private ObjectProperty<Node> node;
	private ObjectProperty<DockContainerLeaf> container;
	private IntegerProperty dragGroupMask;
	private BooleanProperty closable;
	private BooleanProperty canBeDragged;
	private BooleanProperty canBeDroppedToNewWindow;
	private List<DockableCloseListener> closeListeners;

	/**
	 * @param bento
	 * 		Parent bento instance.
	 * @param identifier
	 * 		This dockable's identifier.
	 */
	public Dockable(@Nonnull Bento bento, @Nonnull String identifier) {
		this.bento = bento;
		this.identifier = identifier;
	}

	/**
	 * Invoke all {@link DockableCloseListener} registered against this dockable.
	 */
	public void fireCloseListeners() {
		if (closeListeners != null) {
			DockablePath path = getPath();
			if (path == null)
				return;
			for (DockableCloseListener listener : closeListeners)
				listener.onClose(path, this);

			// Clear so that any repeated calls do not re-trigger listeners.
			closeListeners = null;
		}
	}

	/**
	 * @param consumer
	 * 		Action to run in the parent container, if one exists.
	 */
	public void inContainer(@Nonnull Consumer<DockContainerLeaf> consumer) {
		DockContainerLeaf container = getContainer();
		if (container != null)
			consumer.accept(container);
	}

	/**
	 * @param consumer
	 * 		Action to run in the parent container, if one exists.
	 */
	public void inContainer(@Nonnull BiConsumer<DockContainerLeaf, Dockable> consumer) {
		DockContainerLeaf container = getContainer();
		if (container != null)
			consumer.accept(container, this);
	}

	/**
	 * @return Path to this dockable in the current bento instance.
	 * {@code null} if this dockable has no {@link #getContainer() parent container}.
	 */
	@Nullable
	public DockablePath getPath() {
		DockContainerLeaf parent = getContainer();
		if (parent == null)
			return null;
		return parent.getPath().withChild(this);
	}

	@Nonnull
	@Override
	public Bento getBento() {
		return bento;
	}

	@Nonnull
	@Override
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public boolean matchesIdentity(@Nonnull Identifiable other) {
		return getIdentifier().equals(other.getIdentifier());
	}

	/**
	 * @return Current title.
	 */
	@Nonnull
	public String getTitle() {
		if (title == null)
			return "";
		return title.get();
	}

	/**
	 * @return Title property.
	 */
	@Nonnull
	public StringProperty titleProperty() {
		if (title == null)
			title = new SimpleStringProperty("");
		return title;
	}

	/**
	 * @param title
	 * 		New title.
	 */
	public void setTitle(@Nullable String title) {
		titleProperty().set(Objects.requireNonNullElse(title, ""));
	}

	/**
	 * @return Current tooltip.
	 */
	@Nullable
	public Tooltip getTooltip() {
		if (tooltip == null)
			return null;
		return tooltip.get();
	}

	/**
	 * @return Tooltip property.
	 */
	@Nonnull
	public ObjectProperty<Tooltip> tooltipProperty() {
		if (tooltip == null)
			tooltip = new SimpleObjectProperty<>();
		return tooltip;
	}

	/**
	 * @param tooltip
	 * 		New tooltip.
	 */
	public void setTooltip(@Nullable Tooltip tooltip) {
		tooltipProperty().set(tooltip);
	}

	/**
	 * @return Current icon factory.
	 */
	@Nullable
	public DockableIconFactory getIconFactory() {
		if (iconFactory == null)
			return null;
		return iconFactory.get();
	}

	/**
	 * @return Icon factory property.
	 */
	@Nonnull
	public ObjectProperty<DockableIconFactory> iconFactoryProperty() {
		if (iconFactory == null) iconFactory = new SimpleObjectProperty<>();
		return iconFactory;
	}

	/**
	 * @param iconFactory
	 * 		New icon factory.
	 */
	public void setIconFactory(@Nullable DockableIconFactory iconFactory) {
		iconFactoryProperty().set(iconFactory);
	}

	/**
	 * @return Current context menu factory.
	 */
	@Nullable
	public DockableMenuFactory getContextMenuFactory() {
		if (contextMenuFactory == null)
			return null;
		return contextMenuFactory.get();
	}

	/**
	 * @return Context menu factory property.
	 */
	@Nonnull
	public ObjectProperty<DockableMenuFactory> contextMenuFactoryProperty() {
		if (contextMenuFactory == null)
			contextMenuFactory = new SimpleObjectProperty<>();
		return contextMenuFactory;
	}

	/**
	 * @param contextMenuFactory
	 * 		New context menu factory.
	 */
	public void setContextMenuFactory(@Nullable DockableMenuFactory contextMenuFactory) {
		contextMenuFactoryProperty().set(contextMenuFactory);
	}

	/**
	 * @return Current node to display when this dockable is selected.
	 */
	@Nullable
	public Node getNode() {
		if (node == null)
			return null;
		return node.get();
	}

	/**
	 * @return Node to display when this dockable is selected.
	 */
	@Nonnull
	public ObjectProperty<Node> nodeProperty() {
		if (node == null)
			node = new SimpleObjectProperty<>();
		return node;
	}

	/**
	 * @param node
	 * 		New node to display when this dockable is selected.
	 */
	public void setNode(@Nullable Node node) {
		nodeProperty().set(node);
	}

	/**
	 * @return Current parent container.
	 */
	@Nullable
	public DockContainerLeaf getContainer() {
		if (container == null)
			return null;
		return container.get();
	}

	/**
	 * @return Parent container property.
	 */
	@Nonnull
	public ObjectProperty<DockContainerLeaf> containerProperty() {
		if (container == null)
			container = new SimpleObjectProperty<>();
		return container;
	}

	/**
	 * @param container
	 * 		New parent container.
	 */
	public void setContainer(@Nullable DockContainerLeaf container) {
		DockContainerLeaf priorParent = getContainer();
		containerProperty().set(container);
		bento.events().fire(new DockEvent.DockableParentChanged(this, priorParent, container));
	}

	/**
	 * @return Current drag group mask.
	 */
	public int getDragGroupMask() {
		if (dragGroupMask == null)
			return 0;
		return dragGroupMask.get();
	}

	/**
	 * @return Drag group mask property.
	 */
	@Nonnull
	public IntegerProperty dragGroupMaskProperty() {
		if (dragGroupMask == null)
			dragGroupMask = new SimpleIntegerProperty();
		return dragGroupMask;
	}

	/**
	 * @param dragGroupMask
	 * 		New drag group mask.
	 */
	public void setDragGroupMask(int dragGroupMask) {
		dragGroupMaskProperty().set(dragGroupMask);
	}

	/**
	 * @return {@code true} if this dockable is closable. {@code false} if not closable.
	 */
	public boolean isClosable() {
		if (closable == null)
			return true;
		return closable.get();
	}

	/**
	 * @return Closable property.
	 */
	@Nonnull
	public BooleanProperty closableProperty() {
		if (closable == null)
			closable = new SimpleBooleanProperty(true);
		return closable;
	}

	/**
	 * @param closable
	 *        {@code true} to make this dockable closable. {@code false} to disable closure.
	 */
	public void setClosable(boolean closable) {
		closableProperty().set(closable);
	}

	/**
	 * @return {@code true} if this dockable can be dragged. {@code false} if not draggable.
	 */
	public boolean isCanBeDragged() {
		if (canBeDragged == null)
			return true;
		return canBeDragged.get();
	}

	/**
	 * @return Draggable property.
	 */
	@Nonnull
	public BooleanProperty canBeDraggedProperty() {
		if (canBeDragged == null)
			canBeDragged = new SimpleBooleanProperty();
		return canBeDragged;
	}

	/**
	 * @param canBeDragged
	 *        {@code true} to make this dockable draggable. {@code false} to make it not draggable.
	 */
	public void setCanBeDragged(boolean canBeDragged) {
		canBeDraggedProperty().set(canBeDragged);
	}

	/**
	 * @return {@code true} if this dockable can be drag-dropped to a new window. {@code false} to limit to cross-container dragging.
	 */
	public boolean isCanBeDroppedToNewWindow() {
		if (canBeDroppedToNewWindow == null)
			return true;
		return canBeDroppedToNewWindow.get();
	}

	/**
	 * @return Window droppable property.
	 */
	@Nonnull
	public BooleanProperty canBeDroppedToNewWindowProperty() {
		if (canBeDroppedToNewWindow == null)
			canBeDroppedToNewWindow = new SimpleBooleanProperty();
		return canBeDroppedToNewWindow;
	}

	/**
	 * @param canBeDroppedToNewWindow
	 *        {@code true} to allow this dockable to be drag-dropped to a new window. {@code false} to limit to cross-container dragging.
	 */
	public void setCanBeDroppedToNewWindow(boolean canBeDroppedToNewWindow) {
		canBeDroppedToNewWindowProperty().set(canBeDroppedToNewWindow);
	}

	/**
	 * Adds a listener that fires when this dockable is closed.
	 *
	 * @param listener
	 * 		Listener to add.
	 */
	public void addCloseListener(@Nonnull DockableCloseListener listener) {
		if (closeListeners == null)
			closeListeners = new ArrayList<>();
		closeListeners.add(listener);
	}

	/**
	 * Removes an existing close listener.
	 *
	 * @param listener
	 * 		Listener to remove.
	 */
	public void removeCloseListener(@Nonnull DockableCloseListener listener) {
		if (closeListeners != null)
			closeListeners.remove(listener);
	}

	@Override
	public String toString() {
		return title != null ? getTitle() : getIdentifier();
	}
}
