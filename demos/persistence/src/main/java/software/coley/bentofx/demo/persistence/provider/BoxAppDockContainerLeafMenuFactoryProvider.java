package software.coley.bentofx.demo.persistence.provider;

import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerLeafMenuFactory;
import software.coley.bentofx.persistence.core.api.provider.DockContainerLeafMenuFactoryProvider;

import java.util.Optional;

/**
 * This demo's {@link DockContainerLeafMenuFactoryProvider}.
 *
 * <p>Constructed by the application and passed to {@code getLayoutRestorer}.</p>
 *
 * @author Phil Bryant
 */
public class BoxAppDockContainerLeafMenuFactoryProvider
		implements DockContainerLeafMenuFactoryProvider {

	private static final DockContainerLeafMenuFactory factory =
			dockContainerLeaf ->
					addSideOptions(new ContextMenu(), dockContainerLeaf);

	@Override
	public Optional<DockContainerLeafMenuFactory> getDockContainerLeafMenuFactory(
			final String dockContainerLeafIdentifier
	) {
		return Optional.of(factory);
	}

	private static ContextMenu addSideOptions(ContextMenu menu, DockContainerLeaf space) {
		for (Side side : Side.values()) {
			MenuItem item = new MenuItem(side.name());
			item.setGraphic(new Label(side == space.getSide() ? "✓" : " "));
			item.setOnAction(e -> space.setSide(side));
			menu.getItems().add(item);
		}
		return menu;
	}
}
