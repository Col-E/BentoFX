package software.coley.bentofx.layout;

import jakarta.annotation.Nonnull;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;

/**
 * Outlines a middle level layout that holds a multiple child {@link ContentLayout} separated with a {@link SplitPane}.
 *
 * @author Matt Coley
 */
non-sealed public interface SplitContentLayout extends ContentLayout {
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
	void setChildSize(@Nonnull ContentLayout childLayout, double size);

	/**
	 * Disables dividers in the contained {@link SplitPane} so that the given child layout cannot be resized.
	 *
	 * @param childLayout
	 * 		Child layout to change resizing behavior of.
	 * @param resizable
	 *        {@code true} to allow the user to resize the given child.
	 *        {@code false} to prevent resizing.
	 *
	 * @see #isChildResizable(ContentLayout)
	 */
	void setChildResizable(@Nonnull ContentLayout childLayout, boolean resizable);

	/**
	 * @param childLayout
	 * 		Child layout to check.
	 *
	 * @return Current resizable state of the given child layout.
	 *
	 * @see #setChildResizable(ContentLayout, boolean)
	 */
	boolean isChildResizable(@Nonnull ContentLayout childLayout);
}
