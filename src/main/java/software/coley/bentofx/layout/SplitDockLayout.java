package software.coley.bentofx.layout;

import jakarta.annotation.Nonnull;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;

/**
 * Outlines a middle level layout that holds a multiple child {@link DockLayout} separated with a {@link SplitPane}.
 *
 * @author Matt Coley
 */
non-sealed public interface SplitDockLayout extends DockLayout {
	/**
	 * @return Split orientation property.
	 */
	@Nonnull
	ObjectProperty<Orientation> orientationProperty();

	/**
	 * Reallocates the size of a given child in the containing {@link SplitPane}.
	 *
	 * @param childLayout
	 * 		Child layout to change size of.
	 * @param size
	 * 		New size <i>(In pixels)</i> to grant the given child in the containing {@link SplitPane}.
	 */
	void setChildSize(@Nonnull DockLayout childLayout, double size);

	/**
	 * Reallocates the size of a given child in the containing {@link SplitPane}.
	 *
	 * @param childLayout
	 * 		Child layout to change size of.
	 * @param percent
	 * 		New size <i>(In percent)</i> to grant the given child in the containing {@link SplitPane}.
	 */
	void setChildPercent(@Nonnull DockLayout childLayout, double percent);

	/**
	 * Disables dividers in the contained {@link SplitPane} so that the given child layout cannot be resized.
	 *
	 * @param childLayout
	 * 		Child layout to change resizing behavior of.
	 * @param resizable
	 *        {@code true} to allow the user to resize the given child.
	 *        {@code false} to prevent resizing.
	 *
	 * @see #isChildResizable(DockLayout)
	 */
	void setChildResizable(@Nonnull DockLayout childLayout, boolean resizable);

	/**
	 * @param childLayout
	 * 		Child layout to check.
	 *
	 * @return Current resizable state of the given child layout.
	 *
	 * @see #setChildResizable(DockLayout, boolean)
	 */
	boolean isChildResizable(@Nonnull DockLayout childLayout);

	/**
	 * @param childLayout
	 * 		Child layout to change collapsed state of.
	 * @param collapsed
	 * 		New collapsed state.
	 *
	 * @return {@code true} when the state has changed to the requested state or already is in the given state.
	 * {@code false} when the state couldn't be changed.
	 */
	boolean setChildCollapsed(@Nonnull DockLayout childLayout, boolean collapsed);

	/**
	 * @param childLayout
	 * 		Child layout to get the collapsed state of.
	 *
	 * @return Collapsed state.
	 */
	boolean isChildCollapsed(@Nonnull DockLayout childLayout);
}
