package software.coley.bentofx.dockable;

import jakarta.annotation.Nonnull;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import software.coley.bentofx.control.Header;
import software.coley.bentofx.layout.container.DockContainerLeaf;

/**
 * Click operations.
 *
 * @author Matt Coley
 */
public interface DockableClickBehavior {
	/**
	 * Handles what happens when a {@link Dockable} represented by a {@link Header} is clicked.
	 *
	 * @param container
	 * 		The containing leaf that holds the dockable.
	 * @param dockable
	 * 		The clicked dockable.
	 * @param header
	 * 		The clicked header representing the dockable.
	 * @param e
	 * 		The click event.
	 */
	default void onMouseClick(@Nonnull DockContainerLeaf container,
	                          @Nonnull Dockable dockable,
	                          @Nonnull Header header,
	                          @Nonnull MouseEvent e) {
		// Primary click --> select dockable if not selected, otherwise toggle collapsed state.
		if (e.getButton() == MouseButton.PRIMARY) {
			if (container.getSelectedDockable() == dockable || container.isCollapsed()) {
				container.toggleCollapse(dockable);
			} else if (container.getSelectedDockable() != dockable) {
				container.selectDockable(dockable);
				header.requestFocus();
			}
		}

		// Secondary click --> populate context menu
		if (e.getButton() == MouseButton.SECONDARY) {
			// Show if a menu was provided
			DockableMenuFactory factory = dockable.getContextMenuFactory();
			if (factory != null) {
				ContextMenu menu = factory.build(dockable);
				if (menu != null) {
					menu.setAutoHide(true);
					menu.show(header, e.getScreenX(), e.getScreenY());
				}
			}
		}
	}
}
