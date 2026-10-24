package software.coley.bentofx.demo.persistence;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.Window;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutRestorable;
import software.coley.bentofx.persistence.core.ui.layoutsmenu.LayoutsMenu;

/**
 * The {@link MenuBar} for {@link BoxApp}.
 *
 * <p>Notice that all this class does is assemble menus and that exiting is only
 * action actually defined by the application. All layout management actions are
 * performed by the {@link LayoutsMenu} supplied by the persistence framework.</p>
 *
 * @author Phil Bryant
 * @see LayoutsMenu
 */
final class BoxAppMenuBar extends MenuBar {

	/**
	 * @param owner the window to which raised dialogs will belong.
	 * @param dockingLayoutRestorable the application whose docking layout the
	 * {@code Layouts} menu item switches.
	 * @param onExit what the {@code File | Exit} menu item runs.
	 */
	BoxAppMenuBar(
			final Window owner,
			final DockingLayoutRestorable dockingLayoutRestorable,
			final Runnable onExit
	) {
		// Assemble the File menu
		final MenuItem exitItem = new MenuItem("E_xit");
		exitItem.setOnAction(event -> onExit.run());

		final Menu fileMenu = new Menu("_File");
		fileMenu.getItems().add(exitItem);

		// Assemble the Window menu
		final MenuItem layoutsMenu =
				new LayoutsMenu(owner, dockingLayoutRestorable);

		final Menu windowMenu = new Menu("_Window");
		windowMenu.getItems().add(layoutsMenu);

		// Assemble the MenuBar
		getMenus().setAll(fileMenu, windowMenu);
	}
}
