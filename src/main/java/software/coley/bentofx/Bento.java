package software.coley.bentofx;

import jakarta.annotation.Nonnull;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import software.coley.bentofx.building.ControlsBuilding;
import software.coley.bentofx.building.DockBuilding;
import software.coley.bentofx.building.PlaceholderBuilding;
import software.coley.bentofx.building.StageBuilding;
import software.coley.bentofx.control.DragDropStage;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DragDropBehavior;
import software.coley.bentofx.event.DockEvent;
import software.coley.bentofx.event.EventBus;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.search.SearchHandler;

/**
 * Top level controller for docking operations.
 *
 * @author Matt Coley
 */
public class Bento {
	private final ObservableList<DockContainerRootBranch> rootContainers = FXCollections.observableArrayList();
	private final ObservableList<DockContainerRootBranch> rootContainersView = FXCollections.unmodifiableObservableList(rootContainers);
	private final EventBus eventBus = newEventBus();
	private final SearchHandler searchHandler = newSearchHandler();
	private final StageBuilding stageBuilding = newStageBuilding();
	private final ControlsBuilding controlsBuilding = newControlsBuilding();
	private final DockBuilding dockBuilding = newDockBuilding();
	private final PlaceholderBuilding placeholderBuilding = newPlaceholderBuilding();
	private final DragDropBehavior dragDropBehavior = newDragDropBehavior();

	@Nonnull
	protected EventBus newEventBus() {
		return new EventBus();
	}

	@Nonnull
	protected SearchHandler newSearchHandler() {
		return new SearchHandler(this);
	}

	@Nonnull
	protected StageBuilding newStageBuilding() {
		return new StageBuilding(this);
	}

	@Nonnull
	protected ControlsBuilding newControlsBuilding() {
		return new ControlsBuilding();
	}

	@Nonnull
	protected DockBuilding newDockBuilding() {
		return new DockBuilding(this);
	}

	@Nonnull
	protected PlaceholderBuilding newPlaceholderBuilding() {
		return new PlaceholderBuilding();
	}

	@Nonnull
	protected DragDropBehavior newDragDropBehavior() {
		return new DragDropBehavior();
	}

	/**
	 * @return Bus for handling event firing and event listeners.
	 */
	@Nonnull
	public EventBus events() {
		return eventBus;
	}

	/**
	 * @return Search operations.
	 */
	@Nonnull
	public SearchHandler search() {
		return searchHandler;
	}

	/**
	 * @return Builders for {@link DragDropStage}.
	 */
	@Nonnull
	public StageBuilding stageBuilding() {
		return stageBuilding;
	}

	/**
	 * @return Builders for various bento UI controls.
	 */
	@Nonnull
	public ControlsBuilding controlsBuilding() {
		return controlsBuilding;
	}

	/**
	 * @return Builders for {@link DockContainer} and {@link Dockable}.
	 */
	@Nonnull
	public DockBuilding dockBuilding() {
		return dockBuilding;
	}

	/**
	 * @return Builders for placeholder content.
	 */
	@Nonnull
	public PlaceholderBuilding placeholderBuilding() {
		return placeholderBuilding;
	}

	/**
	 * @return Behavior implementation for drag-drop.
	 */
	@Nonnull
	public DragDropBehavior getDragDropBehavior() {
		return dragDropBehavior;
	}

	/**
	 * @return List of tracked root contents.
	 *
	 * @see #registerRoot(DockContainerRootBranch)
	 * @see #unregisterRoot(DockContainerRootBranch)
	 */
	@Nonnull
	public ObservableList<DockContainerRootBranch> getRootContainers() {
		return rootContainersView;
	}

	/**
	 * @param container
	 * 		Root container to register.
	 *
	 * @return {@code true} when registered.
	 */
	public boolean registerRoot(@Nonnull DockContainerRootBranch container) {
		if (!rootContainers.contains(container)) {
			rootContainers.add(container);
			eventBus.fire(new DockEvent.RootContainerAdded(container));
			return true;
		}
		return false;
	}

	/**
	 * @param container
	 * 		Root container to unregister.
	 *
	 * @return {@code true} when unregistered.
	 */
	public boolean unregisterRoot(@Nonnull DockContainerRootBranch container) {
		if (rootContainers.remove(container)) {
			eventBus.fire(new DockEvent.RootContainerRemoved(container));
			return true;
		}
		return false;
	}
}
