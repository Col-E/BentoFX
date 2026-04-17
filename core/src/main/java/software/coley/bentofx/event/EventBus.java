package software.coley.bentofx.event;

import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableCloseListener;
import software.coley.bentofx.dockable.DockableMoveListener;
import software.coley.bentofx.dockable.DockableOpenListener;
import software.coley.bentofx.dockable.DockableSelectListener;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.path.DockablePath;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bus for handling event firing and event listeners.
 *
 * @author Matt Coley
 */
public class EventBus {
	private final List<DockEventListener> eventListeners = new CopyOnWriteArrayList<>();
	private final List<DockableOpenListener> openListeners = new CopyOnWriteArrayList<>();
	private final List<DockableMoveListener> moveListeners = new CopyOnWriteArrayList<>();
	private final List<DockableCloseListener> closeListeners = new CopyOnWriteArrayList<>();
	private final List<DockableSelectListener> selectListeners = new CopyOnWriteArrayList<>();

	/**
	 * @param event
	 * 		Event to fire.
	 */
	public void fire(DockEvent event) {
		// Fire generic event listeners
		for (DockEventListener listener : eventListeners)
			listener.onDockEvent(event);

		// Fire specific listeners
		switch (event) {
			case DockEvent.ContainerChildAdded containerChildAdded -> {}
			case DockEvent.ContainerChildRemoved containerChildRemoved -> {}
			case DockEvent.ContainerParentChanged containerParentChanged -> {}
			case DockEvent.DockableAdded dockableAdded -> {
				Dockable dockable = dockableAdded.dockable();
				DockablePath path = Objects.requireNonNull(dockable.getPath());
				for (DockableOpenListener listener : openListeners) {
					listener.onOpen(path, dockable);
				}
			}
			case DockEvent.DockableClosing dockableClosing -> {
				Dockable dockable = dockableClosing.dockable();
				DockablePath path = Objects.requireNonNull(dockable.getPath());
				for (DockableCloseListener listener : closeListeners) {
					listener.onClose(path, dockable);
				}
			}
			case DockEvent.DockableParentChanged dockableParentChanged -> {
				DockContainerLeaf priorParent = dockableParentChanged.priorParent();
				DockContainerLeaf newParent = dockableParentChanged.newParent();
				if (priorParent == null || newParent == null)
					return;
				Dockable dockable = dockableParentChanged.dockable();
				DockablePath oldPath = priorParent.getPath().withChild(dockable);
				DockablePath newPath = newParent.getPath().withChild(dockable);
				for (DockableMoveListener listener : moveListeners) {
					listener.onMove(oldPath, newPath, dockable);
				}
			}
			case DockEvent.DockableRemoved dockableRemoved -> {}
			case DockEvent.DockableSelected dockableSelected -> {
				Dockable dockable = dockableSelected.dockable();
				DockablePath path = Objects.requireNonNull(dockable.getPath());
				for (DockableSelectListener listener : selectListeners) {
					listener.onSelect(path, dockable);
				}
			}
			case DockEvent.RootContainerAdded rootContainerAdded -> {}
			case DockEvent.RootContainerRemoved rootContainerRemoved -> {}
		}
	}

	/**
	 * @return Generic event listeners.
	 */
	public List<DockEventListener> getEventListeners() {
		return Collections.unmodifiableList(eventListeners);
	}

	/**
	 * @param listener
	 * 		Generic event listener to add.
	 */
	public void addEventListener(DockEventListener listener) {
		eventListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Generic event listener to remove.
	 */
	public boolean removeEventListener(DockEventListener listener) {
		return eventListeners.remove(listener);
	}

	/**
	 * @return Dockable opening listeners.
	 */
	public List<DockableOpenListener> getDockableOpenListener() {
		return Collections.unmodifiableList(openListeners);
	}

	/**
	 * @param listener
	 * 		Dockable opening listener to add.
	 */
	public void addDockableOpenListener(DockableOpenListener listener) {
		openListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Dockable opening listener to remove.
	 */
	public boolean removeDockableOpenListener(DockableOpenListener listener) {
		return openListeners.remove(listener);
	}

	/**
	 * @return Dockable moving listeners.
	 */
	public List<DockableMoveListener> getDockableMoveListener() {
		return Collections.unmodifiableList(moveListeners);
	}

	/**
	 * @param listener
	 * 		Dockable moving listener to add.
	 */
	public void addDockableMoveListener(DockableMoveListener listener) {
		moveListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Dockable moving listener to remove.
	 */
	public boolean removeDockableMoveListener(DockableMoveListener listener) {
		return moveListeners.remove(listener);
	}

	/**
	 * @return Dockable closing listeners.
	 */
	public List<DockableCloseListener> getDockableCloseListener() {
		return Collections.unmodifiableList(closeListeners);
	}

	/**
	 * @param listener
	 * 		Dockable closing listener to add.
	 */
	public void addDockableCloseListener(DockableCloseListener listener) {
		closeListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Dockable closing listener to remove.
	 */
	public boolean removeDockableCloseListener(DockableCloseListener listener) {
		return closeListeners.remove(listener);
	}

	/**
	 * @return Dockable selecting listeners.
	 */
	public List<DockableSelectListener> getDockableSelectListener() {
		return Collections.unmodifiableList(selectListeners);
	}

	/**
	 * @param listener
	 * 		Dockable selecting listener to add.
	 */
	public void addDockableSelectListener(DockableSelectListener listener) {
		selectListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Dockable selecting listener to remove.
	 */
	public boolean removeDockableSelectListener(DockableSelectListener listener) {
		return selectListeners.remove(listener);
	}
}
