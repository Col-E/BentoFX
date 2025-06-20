package software.coley.bentofx.building;

import jakarta.annotation.Nonnull;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockablePlaceholderFactory;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerLeafPlaceholderFactory;

/**
 * Builders for placeholder content when:
 * <ul>
 *     <li>A {@link Dockable} is selected in a {@link DockContainerLeaf} but has no content to show</li>
 *     <li>A {@link DockContainerLeaf} has no selected dockable</li>
 * </ul>
 *
 * @author Matt Coley
 */
public class PlaceholderBuilding implements DockablePlaceholderFactory, DockContainerLeafPlaceholderFactory {
	private DockablePlaceholderFactory dockablePlaceholderFactory = dockable -> new Pane();
	private DockContainerLeafPlaceholderFactory containerPlaceholderFactory = container -> new Pane();

	/**
	 * @return Current placeholder factory for dockables with no content to show.
	 */
	@Nonnull
	public DockablePlaceholderFactory getDockablePlaceholderFactory() {
		return dockablePlaceholderFactory;
	}

	/**
	 * @param dockablePlaceholderFactory
	 * 		Placeholder factory for dockables with no content to show.
	 */
	public void setDockablePlaceholderFactory(@Nonnull DockablePlaceholderFactory dockablePlaceholderFactory) {
		this.dockablePlaceholderFactory = dockablePlaceholderFactory;
	}

	/**
	 * @return Current placeholder factory for containers with no content to show.
	 */
	@Nonnull
	public DockContainerLeafPlaceholderFactory getContainerPlaceholderFactory() {
		return containerPlaceholderFactory;
	}

	/**
	 * @param containerPlaceholderFactory
	 * 		Placeholder factory for containers with no content to show.
	 */
	public void setContainerPlaceholderFactory(@Nonnull DockContainerLeafPlaceholderFactory containerPlaceholderFactory) {
		this.containerPlaceholderFactory = containerPlaceholderFactory;
	}

	@Nonnull
	@Override
	public Node build(@Nonnull Dockable dockable) {
		return getDockablePlaceholderFactory().build(dockable);
	}

	@Nonnull
	@Override
	public Node build(@Nonnull DockContainerLeaf container) {
		return getContainerPlaceholderFactory().build(container);
	}
}
