package software.coley.bentofx.dockable;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Tooltip;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.header.HeaderView;
import software.coley.bentofx.util.ConstantIcon;

/**
 * Outline of some item to display, generally tied to a {@link Header} inside a {@link HeaderView}.
 *
 * @author Matt Coley
 */
public interface Dockable extends Identifiable {
	/**
	 * @return Content to display when this dockable is active in its parent {@link Content}.
	 */
	@Nonnull
	ObjectProperty<Node> nodeProperty();

	/**
	 * Drag groups are arbitrary integers. By default, any dockable's group is {@code 0}.
	 * Any two dockables that have different groups cannot be dragged by the user to reside in the same {@link Content}.
	 *
	 * @return This dockable's group.
	 */
	int getDragGroup();

	/**
	 * @return New graphic to represent this dockable.
	 */
	@Nullable
	default Node buildIcon() {
		DockableIconFactory factory = iconFactoryProperty().get();
		return factory == null ? null : factory.build(this);
	}

	/**
	 * @return This dockable's display text when represented by a {@link Header}.
	 */
	@Nonnull
	StringProperty titleProperty();

	/**
	 * @return This dockable's tooltip when hovered as a {@link Header}.
	 */
	@Nonnull
	ObjectProperty<Tooltip> tooltipProperty();

	/**
	 * @return This dockable's display graphic when represented by a {@link Header}.
	 */
	@Nonnull
	ObjectProperty<DockableIconFactory> iconFactoryProperty();

	/**
	 * @return This dockable's closable state when represented by a {@link Header}.
	 */
	@Nonnull
	BooleanProperty closableProperty();

	/**
	 * @return This dockable's draggable state when represented by a {@link Header}.
	 */
	@Nonnull
	ObservableBooleanValue canBeDragged();

	/**
	 * @return This dockable's externalized-droppable state when represented by a {@link Header}.
	 */
	@Nonnull
	ObservableBooleanValue canBeDroppedToNewWindow();

	/**
	 * @return This dockable's context-menu factory used to populate menus in a representative {@link Header}.
	 */
	@Nonnull
	ObjectProperty<DockableMenuFactory> contextMenuFactoryProperty();

	/**
	 * @return This dockable's persistence for {@link #contextMenuFactoryProperty()} generated {@link ContextMenu}s.
	 */
	@Nonnull
	BooleanProperty cachedContextMenuProperty();

	/**
	 * @param title
	 * 		Title text to set.
	 *
	 * @return Self.
	 */
	@Nonnull
	default Dockable withTitle(@Nullable String title) {
		titleProperty().set(title);
		return this;
	}

	/**
	 * @param tooltip
	 * 		Tooltip to set.
	 *
	 * @return Self.
	 */
	@Nonnull
	default Dockable withTooltip(@Nullable Tooltip tooltip) {
		tooltipProperty().set(tooltip);
		return this;
	}

	/**
	 * @param icon
	 * 		Icon to set.
	 *
	 * @return Self.
	 */
	@Nonnull
	default Dockable withIcon(@Nullable Node icon) {
		if (icon == null)
			iconFactoryProperty().set(null);
		else
			iconFactoryProperty().set(new ConstantIcon(icon));
		return this;
	}

	/**
	 * @param factory
	 * 		Icon factory to set.
	 *
	 * @return Self.
	 */
	@Nonnull
	default Dockable withIconFactory(@Nullable DockableIconFactory factory) {
		iconFactoryProperty().set(factory);
		return this;
	}

	/**
	 * @param factory
	 * 		Context menu factory to set.
	 *
	 * @return Self.
	 */
	@Nonnull
	default Dockable withContextMenuFactory(@Nullable DockableMenuFactory factory) {
		contextMenuFactoryProperty().set(factory);
		return this;
	}

	/**
	 * @param cacheMenu
	 *        {@code true} to cache the values of {@link #contextMenuFactoryProperty() menu factory} outputs.
	 *        {@code false} to provide a new {@link ContextMenu} on each request.
	 *
	 * @return Self.
	 */
	@Nonnull
	default Dockable withCachedContextMenu(boolean cacheMenu) {
		cachedContextMenuProperty().set(cacheMenu);
		return this;
	}

	/**
	 * @param closable
	 *        {@code true} to allow this dockable to be closed when represented as a {@link Header}.
	 *        {@code false} to disallow closing.
	 *
	 * @return Self.
	 */
	@Nonnull
	default Dockable withClosable(boolean closable) {
		closableProperty().set(closable);
		return this;
	}

	/**
	 * @param listener
	 * 		Listener to call upon this dockable's closure.
	 *
	 * @return Self.
	 */
	@Nonnull
	Dockable withCloseListener(@Nullable DockableCloseListener listener);
}
