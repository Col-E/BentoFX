package software.coley.bentofx.path;

import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Path to a given container, starting from the root container <i>(Top {@link DockContainer#getParentContainer()} value)</i>
 * all the way down to the target container <i>(Assuming this is a result of a lookup for a specific container)</i>.
 *
 * @param containers
 * 		Containers up to and including some target container.
 */
public record DockContainerPath(List<DockContainer> containers) implements BentoPath {
	/**
	 * @param child
	 * 		Child to append to new path.
	 *
	 * @return New path with the given child at the end.
	 */
	public DockContainerPath withChild(DockContainer child) {
		List<DockContainer> containersWithChild = new ArrayList<>(containers.size() + 1);
		containersWithChild.addAll(containers);
		containersWithChild.add(child);
		return new DockContainerPath(containersWithChild);
	}

	/**
	 * @param child
	 * 		Child to append to new path.
	 *
	 * @return New path with the given child at the end.
	 */
	public DockablePath withChild(Dockable child) {
		return new DockablePath(containers, child);
	}

	@Override
	public DockContainer rootContainer() {
		// There must always be at least one container since we must have a result for the path.
		return containers.getFirst();
	}

	/**
	 * @return Tail container in the path / intended target of the path.
	 */
	public DockContainer tailContainer() {
		// There must always be at least one container since we must have a result for the path.
		return containers.getLast();
	}
}
