package software.coley.bentofx.demo.persistence.provider;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableMenuFactory;
import software.coley.bentofx.persistence.core.api.provider.DockableMenuFactoryProvider;

import java.util.Optional;

/**
 * This demo's {@link DockableMenuFactoryProvider}, returning the same sample menu
 * for every {@link Dockable}.
 *
 * <p>Constructed by, and called only from, {@link BoxAppDockableStateProvider}.</p>
 *
 * @author Phil Bryant
 */
public class BoxAppDockableMenuFactoryProvider implements DockableMenuFactoryProvider {

	private static final DockableMenuFactory factory = dockable ->
			new ContextMenu(
					new MenuItem("Menu for : " + dockable.getTitle()),
					new SeparatorMenuItem(),
					new MenuItem("Stuff")
			);

	@Override
	public Optional<DockableMenuFactory> getDockableMenuFactory(
			final String dockableIdentifier
	) {
		return Optional.of(factory);
	}
}
