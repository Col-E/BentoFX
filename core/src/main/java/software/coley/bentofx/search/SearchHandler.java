package software.coley.bentofx.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.input.DragEvent;
import software.coley.bentofx.Bento;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.path.DockContainerPath;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.util.DragUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Search operations within a bento instance.
 *
 * @author Matt Coley
 */
public class SearchHandler {
	private final Bento bento;

	/**
	 * @param bento
	 * 		Parent bento instance.
	 */
	public SearchHandler(@Nonnull Bento bento) {
		this.bento = bento;
	}

	/**
	 * @param identifier
	 * 		Some {@link DockContainer#getIdentifier()}.
	 * @param replacement
	 * 		Content to replace the matched container with.
	 *
	 * @return {@code true} when replacement was completed.
	 */
	public boolean replaceContainer(@Nonnull String identifier, @Nonnull DockContainer replacement) {
		return replaceContainer(identifier, () -> replacement);
	}

	/**
	 * @param identifier
	 * 		Some {@link DockContainer#getIdentifier()}.
	 * @param replacement
	 * 		Supplier of content to replace the matched container with.
	 *
	 * @return {@code true} when replacement was completed.
	 */
	public boolean replaceContainer(@Nonnull String identifier, @Nonnull Supplier<DockContainer> replacement) {
		DockContainerPath container = container(identifier);
		if (container == null)
			return false;

		DockContainer original = container.tailContainer();
		DockContainerBranch parent = original.getParentContainer();
		if (parent == null)
			return false;

		return parent.replaceContainer(original, replacement.get());
	}

	/**
	 * @param identifier
	 * 		Some {@link DockContainer#getIdentifier()}.
	 *
	 * @return Path to the matched container, if found.
	 */
	@Nullable
	public DockContainerPath container(@Nonnull String identifier) {
		return container(c -> c.getIdentifier().equals(identifier));
	}

	/**
	 * @param predicate
	 * 		Predicate to match against some container.
	 *
	 * @return Path to the first matched container, if found.
	 */
	@Nullable
	public DockContainerPath container(@Nonnull Predicate<DockContainer> predicate) {
		DockContainerVisitor visitor = new DockContainerVisitor(predicate);
		for (DockContainer container : bento.getRootContainers()) {
			if (!container.visit(visitor))
				break;
		}
		DockContainer result = visitor.getMatchedContainer();
		return result == null ? null : result.getPath();
	}

	/**
	 * @param event
	 * 		A drag event that may have a {@link Dockable} associated with it.
	 *
	 * @return Path to the associated {@link Dockable} if found.
	 */
	@Nullable
	public DockablePath dockable(@Nonnull DragEvent event) {
		String identifier = DragUtils.extractIdentifier(event.getDragboard());
		return identifier == null ? null : dockable(identifier);
	}

	/**
	 * @param identifier
	 * 		Some {@link Dockable#getIdentifier()}.
	 *
	 * @return Path to the matched container, if found.
	 */
	@Nullable
	public DockablePath dockable(@Nonnull String identifier) {
		return dockable(d -> d.getIdentifier().equals(identifier));
	}

	/**
	 * @param predicate
	 * 		Predicate to match against some dockable.
	 *
	 * @return Path to the first matched dockable, if found.
	 */
	@Nullable
	public DockablePath dockable(@Nonnull Predicate<Dockable> predicate) {
		DockableVisitor visitor = new DockableVisitor(predicate);
		for (DockContainer container : bento.getRootContainers()) {
			if (!container.visit(visitor))
				break;
		}
		Dockable result = visitor.getMatchedDockable();
		return result == null ? null : result.getPath();
	}

	/**
	 * @return All found dockable paths in the current bento instance.
	 */
	@Nonnull
	public List<DockablePath> allDockables() {
		List<DockablePath> paths = new ArrayList<>();
		for (DockContainerRootBranch root : bento.getRootContainers()) {
			paths.addAll(root.getDockables().stream()
					.map(Dockable::getPath)
					.filter(Objects::nonNull) // Sanity check
					.toList());
		}
		return paths;
	}
}
