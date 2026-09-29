package software.coley.bentofx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import software.coley.bentofx.building.ControlsBuilding;
import software.coley.bentofx.building.DockBuilding;
import software.coley.bentofx.building.PlaceholderBuilding;
import software.coley.bentofx.building.StageBuilding;
import software.coley.bentofx.control.DragDropStage;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableClickBehavior;
import software.coley.bentofx.dockable.DockableDragDropBehavior;
import software.coley.bentofx.event.DockEvent;
import software.coley.bentofx.event.EventBus;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.search.SearchHandler;

import java.util.Objects;

/**
 * Top level controller for docking operations.
 *
 * @author Matt Coley
 */
public class Bento implements Identifiable {
	private final String identifier;
	private final ObservableList<DockContainerRootBranch> rootContainers = FXCollections.observableArrayList();
	private final ObservableList<DockContainerRootBranch> rootContainersView = FXCollections.unmodifiableObservableList(rootContainers);
	private final EventBus eventBus = newEventBus();
	private final SearchHandler searchHandler = newSearchHandler();
	private final StageBuilding stageBuilding = newStageBuilding();
	private final ControlsBuilding controlsBuilding = newControlsBuilding();
	private final DockBuilding dockBuilding = newDockBuilding();
	private final PlaceholderBuilding placeholderBuilding = newPlaceholderBuilding();
	private final DockableDragDropBehavior dragDropBehavior = newDragDropBehavior();
	private final DockableClickBehavior clickBehavior = newClickBehavior();

	/**
	 * Creates a {@code Bento} with a generated identifier.
	 */
	public Bento() {
		identifier = DockBuilding.uid("cbento");
	}

	/**
	 * Creates a {@code Bento} with the given identifier.
	 *
	 * @param identifier
	 * 		This bento's identifier.
	 */
	public Bento(final String identifier) {
		Objects.requireNonNull(identifier, "identifier");
		this.identifier = identifier;
	}

	protected EventBus newEventBus() {
		return new EventBus();
	}

	protected SearchHandler newSearchHandler() {
		return new SearchHandler(this);
	}

	protected StageBuilding newStageBuilding() {
		return new StageBuilding(this);
	}

	protected ControlsBuilding newControlsBuilding() {
		return new ControlsBuilding();
	}

	protected DockBuilding newDockBuilding() {
		return new DockBuilding(this);
	}

	protected PlaceholderBuilding newPlaceholderBuilding() {
		return new PlaceholderBuilding();
	}

	protected DockableDragDropBehavior newDragDropBehavior() {
		return new DockableDragDropBehavior() {};
	}

	protected DockableClickBehavior newClickBehavior() {
		return new DockableClickBehavior() {};
	}

	/**
	 * @return Bus for handling event firing and event listeners.
	 */
	public EventBus events() {
		return eventBus;
	}

	/**
	 * @return Search operations.
	 */
	public SearchHandler search() {
		return searchHandler;
	}

	/**
	 * @return Builders for {@link DragDropStage}.
	 */
	public StageBuilding stageBuilding() {
		return stageBuilding;
	}

	/**
	 * @return Builders for various bento UI controls.
	 */
	public ControlsBuilding controlsBuilding() {
		return controlsBuilding;
	}

	/**
	 * @return Builders for {@link DockContainer} and {@link Dockable}.
	 */
	public DockBuilding dockBuilding() {
		return dockBuilding;
	}

	/**
	 * @return Builders for placeholder content.
	 */
	public PlaceholderBuilding placeholderBuilding() {
		return placeholderBuilding;
	}

	/**
	 * @return Behavior implementation for drag-drop operations.
	 */
	public DockableDragDropBehavior getDragDropBehavior() {
		return dragDropBehavior;
	}

	/**
	 * @return Behavior implementation for click operations.
	 */
	public DockableClickBehavior getClickBehavior() {
		return clickBehavior;
	}

	/**
	 * @return List of tracked root contents.
	 *
	 * @see #registerRoot(DockContainerRootBranch)
	 * @see #unregisterRoot(DockContainerRootBranch)
	 */
	public ObservableList<DockContainerRootBranch> getRootContainers() {
		return rootContainersView;
	}

	/**
	 * @param container
	 * 		Root container to register.
	 *
	 * @return {@code true} when registered.
	 */
	public boolean registerRoot(DockContainerRootBranch container) {
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
	public boolean unregisterRoot(DockContainerRootBranch container) {
		if (rootContainers.remove(container)) {
			eventBus.fire(new DockEvent.RootContainerRemoved(container));
			return true;
		}
		return false;
	}

	/**
	 * {@return the identifier specified when creating this {@code Bento}}
	 * <p>
	 * This identifier is not guaranteed to be unique.
	 */
	@Override
	public String getIdentifier() {
		return identifier;
	}
}
