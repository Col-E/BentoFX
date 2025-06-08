package software.coley.bentofx;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.stage.Stage;
import software.coley.bentofx.builder.DockableBuilder;
import software.coley.bentofx.builder.LayoutBuilder;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableCloseListener;
import software.coley.bentofx.dockable.DockableDestination;
import software.coley.bentofx.dockable.DockableMoveListener;
import software.coley.bentofx.dockable.DockableOpenListener;
import software.coley.bentofx.dockable.DockableSelectListener;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.impl.ImplBento;
import software.coley.bentofx.layout.DockLayout;
import software.coley.bentofx.layout.LeafDockLayout;
import software.coley.bentofx.layout.RootDockLayout;
import software.coley.bentofx.layout.SplitDockLayout;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.path.LayoutPath;
import software.coley.bentofx.path.SpacePath;
import software.coley.bentofx.space.DockSpace;
import software.coley.bentofx.space.EmptyDisplayFactory;
import software.coley.bentofx.space.EmptyDockSpace;
import software.coley.bentofx.space.SingleDockSpace;
import software.coley.bentofx.space.TabbedDockSpace;
import software.coley.bentofx.space.TabbedDockSpaceDecorator;
import software.coley.bentofx.util.BentoUtils;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Supplier;

/**
 * Controller for docking supported layouts and spaces.
 *
 * @author Matt Coley
 */
public interface Bento {
	/**
	 * @return New bento instance.
	 */
	@Nonnull
	static Bento newBento() {
		return new ImplBento();
	}

	/**
	 * @return New builder to create {@link RootDockLayout}, {@link DockLayout} and {@link DockSpace} instances.
	 */
	@Nonnull
	LayoutBuilder newLayoutBuilder();

	/**
	 * @return New builder to create {@link Dockable} instances.
	 */
	@Nonnull
	DockableBuilder newDockableBuilder();

	/**
	 * Create a new empty space instance with some configurable {@link Node} content.
	 *
	 * @param parentLayout
	 * 		Parent layout to contain the new {@link EmptyDockSpace}.
	 *
	 * @return A new empty space instance.
	 *
	 * @see #setEmptyDisplayFactory(EmptyDisplayFactory)
	 */
	@Nonnull
	DockSpace newEmptySpace(@Nonnull DockLayout parentLayout);

	/**
	 * @param factory
	 * 		Factory to create a display for {@link EmptyDockSpace} when placed into the UI.
	 */
	void setEmptyDisplayFactory(@Nullable EmptyDisplayFactory factory);

	/**
	 * @param factory
	 * 		Factory to create {@link Stage} instances for new window creation.
	 */
	void setStageFactory(@Nullable StageFactory factory);

	/**
	 * @param factory
	 * 		Factory to create {@link Scene} instances for new window creation.
	 */
	void setSceneFactory(@Nullable SceneFactory factory);

	/**
	 * @param decorator
	 * 		Decorator responsible for intercepting all created {@link TabbedDockSpace}.
	 */
	void setTabbedDockSpaceDecorator(@Nullable TabbedDockSpaceDecorator decorator);

	/**
	 * @param source
	 * 		The source where the header is being dragged out of.
	 * @param header
	 * 		Header being dragged into some open space, triggering the creation of a new stage.
	 *
	 * @return The new stage for the header to be moved to.
	 */
	@Nonnull
	Stage newStageForDroppedHeader(@Nonnull DockableDestination source, @Nonnull Header header);

	/**
	 * @param sourceScene
	 * 		Optional origin scene to copy stylesheets from.
	 * @param dockable
	 * 		Dockable to place in the newly created stage.
	 * @param width
	 * 		Initial stage width.
	 * @param height
	 * 		Initial stage height.
	 *
	 * @return The new stage for the dockable.
	 */
	@Nonnull
	Stage newStageForDockable(@Nullable Scene sourceScene, @Nonnull Dockable dockable, double width, double height);

	/**
	 * @param identifier
	 * 		The identifier of some {@link DockLayout} to find and replace.
	 * @param replacementProvider
	 * 		Supplier of a {@link DockLayout} to replace the existing layout with.
	 *
	 * @return {@code true} when the existing layout was found and replaced.
	 */
	default boolean replaceLayout(@Nonnull String identifier, @Nonnull Supplier<DockLayout> replacementProvider) {
		LayoutPath path = findLayout(identifier);
		if (path == null || path.layouts().isEmpty())
			return false;

		DockLayout target = path.layouts().getLast();
		DockLayout parentLayout = target.getParentLayout();
		if (parentLayout == null)
			return false;

		parentLayout.replaceChildLayout(target, replacementProvider.get());
		return true;
	}

	/**
	 * @param identifier
	 * 		The identifier of some {@link DockSpace} to find and replace.
	 * @param replacementProvider
	 * 		Supplier of a {@link DockSpace} to replace the existing space with.
	 *
	 * @return {@code true} when the existing space was found and replaced.
	 */
	default boolean replaceSpace(@Nonnull String identifier, @Nonnull Supplier<DockSpace> replacementProvider) {
		SpacePath path = findSpace(identifier);
		if (path == null || path.layouts().isEmpty())
			return false;

		DockLayout parent = path.layouts().getLast();
		switch (parent) {
			case LeafDockLayout leaf -> leaf.setSpace(replacementProvider.get());
			case SplitDockLayout ignored -> throw new IllegalStateException(SplitDockLayout.class.getSimpleName() +
					" should never have a direct child " + DockSpace.class.getSimpleName());
		}
		return true;
	}

	/**
	 * @return Unmodifiable list of all tracked {@link RootDockLayout}
	 * created by this bento instance that are present in any active scenes.
	 */
	@Nonnull
	ObservableList<RootDockLayout> getRootLayouts();

	/**
	 * @return List of all {@link Dockable} instanced tracked in this instance.
	 */
	@Nonnull
	default List<DockablePath> getAllDockables() {
		List<DockablePath> paths = new ArrayList<>();
		for (RootDockLayout root : getRootLayouts()) {
			Queue<DockLayout> layouts = new ArrayDeque<>();
			layouts.add(root.getLayout());
			while (!layouts.isEmpty()) {
				DockLayout layout = layouts.remove();
				switch (layout) {
					case LeafDockLayout leaf -> {
						switch (leaf.getSpace()) {
							case EmptyDockSpace ignored -> {}
							case SingleDockSpace single -> {
								SpacePath spacePath = single.getPath();
								if (spacePath != null)
									paths.add(new DockablePath(spacePath, single.getDockable()));
							}
							case TabbedDockSpace tabbed -> {
								SpacePath spacePath = tabbed.getPath();
								if (spacePath != null)
									for (Dockable dockable : tabbed.getDockables())
										paths.add(new DockablePath(spacePath, dockable));
							}
						}
					}
					case SplitDockLayout ignored -> layouts.addAll(layout.getChildLayouts());
				}
			}
		}
		return paths;
	}

	/**
	 * Attempts to find a given {@link DockLayout} from any child of any depth belonging to this bento instance.
	 * If a {@code null} is returned, then the given layout does not exist in any child belonging to this bento instance.
	 *
	 * @param layout
	 * 		Some layout to find.
	 *
	 * @return The path to the {@link DockLayout} if found, otherwise {@code null}.
	 */
	@Nullable
	default LayoutPath findLayout(@Nonnull DockLayout layout) {
		return findLayout(layout.getIdentifier());
	}

	/**
	 * Attempts to find a given {@link DockLayout} from any child of any depth belonging to this bento instance.
	 * The identifier will be matched against {@link DockLayout#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link DockLayout} to find.
	 *
	 * @return The path to the {@link DockLayout} if found, otherwise {@code null}.
	 */
	@Nullable
	default LayoutPath findLayout(@Nonnull String identifier) {
		for (RootDockLayout layout : getRootLayouts()) {
			LayoutPath path = layout.findLayout(identifier);
			if (path != null)
				return path;
		}
		return null;
	}

	/**
	 * Attempts to find a given {@link DockSpace} from any child {@link DockLayout} of any depth belonging to this bento instance.
	 * If a {@code null} is returned, then the given space does not exist in any child {@link DockLayout}
	 * belonging to this bento instance.
	 *
	 * @param space
	 * 		Some dock space to find.
	 *
	 * @return The path to the {@link Dockable} if found, otherwise {@code null}.
	 */
	@Nullable
	default SpacePath findSpace(@Nonnull DockSpace space) {
		return findSpace(space.getIdentifier());
	}

	/**
	 * Attempts to find a given {@link DockSpace} from any child {@link DockLayout} of any depth belonging to this bento instance.
	 * The identifier will be matched against {@link DockSpace#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link DockSpace} to find.
	 *
	 * @return The path to the {@link DockSpace} if found, otherwise {@code null}.
	 */
	@Nullable
	default SpacePath findSpace(@Nonnull String identifier) {
		for (RootDockLayout layout : getRootLayouts()) {
			SpacePath path = layout.findSpace(identifier);
			if (path != null)
				return path;
		}
		return null;
	}

	/**
	 * Attempts to find a {@link Dockable}, based on the identifier present in the event's {@link Dragboard},
	 * contained within any child {@link DockSpace} of any depth belonging to this bento instance.
	 * If a {@code null} is returned, then the given dockable does not exist in any child {@link DockLayout}
	 * belonging to this bento instance.
	 *
	 * @param event
	 * 		Event to extract {@link Dockable#getIdentifier()} from.
	 *
	 * @return The path to the {@link Dockable} if found, otherwise {@code null}.
	 */
	@Nullable
	default DockablePath findDockable(@Nonnull DragEvent event) {
		String identifier = BentoUtils.extractIdentifier(event.getDragboard());
		return identifier == null ? null : findDockable(identifier);
	}

	/**
	 * Attempts to find a given {@link Dockable} from any child {@link DockSpace} of any depth belonging to this bento instance.
	 * If a {@code null} is returned, then the given dockable does not exist in any child {@link DockLayout}
	 * belonging to this bento instance.
	 *
	 * @param dockable
	 * 		Some dockable to find.
	 *
	 * @return The path to the {@link Dockable} if found, otherwise {@code null}.
	 */
	@Nullable
	default DockablePath findDockable(@Nonnull Dockable dockable) {
		return findDockable(dockable.getIdentifier());
	}

	/**
	 * Attempts to find a given {@link Dockable} from any child {@link DockSpace} of any depth belonging to this bento instance.
	 * The identifier will be matched against {@link Dockable#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link Dockable} to find.
	 *
	 * @return The path to the {@link Dockable} if found, otherwise {@code null}.
	 */
	@Nullable
	default DockablePath findDockable(@Nonnull String identifier) {
		for (RootDockLayout layout : getRootLayouts()) {
			DockablePath path = layout.findDockable(identifier);
			if (path != null)
				return path;
		}
		return null;
	}

	/**
	 * Attempts to remove the given dockable from any child {@link DockSpace} of any depth belonging to this bento instance.
	 * Be aware, this method will bypass {@link Dockable#closableProperty()}.
	 *
	 * @param dockable
	 * 		Dockable to remove.
	 *
	 * @return {@code true} if removed. {@code false} if not removed.
	 *
	 * @see #closeDockable(Dockable)
	 */
	default boolean removeDockable(@Nonnull Dockable dockable) {
		for (RootDockLayout layout : getRootLayouts()) {
			if (layout.removeDockable(dockable))
				return true;
		}
		return false;
	}

	/**
	 * Attempts to close the given dockable from any child {@link DockSpace} of any depth belonging to this bento instance.
	 *
	 * @param dockable
	 * 		Dockable to remove.
	 *
	 * @return {@code true} if closed. {@code false} if not closed.
	 *
	 * @see #removeDockable(Dockable)
	 */
	default boolean closeDockable(@Nonnull Dockable dockable) {
		for (RootDockLayout layout : getRootLayouts()) {
			if (layout.closeDockable(dockable))
				return true;
		}
		return false;
	}

	/**
	 * @param listener
	 * 		Listener to add that observes new {@link Dockable} items being opened.
	 */
	void addDockableOpenListener(@Nonnull DockableOpenListener listener);

	/**
	 * @param listener
	 * 		Listener to remove that observes new {@link Dockable} items being opened.
	 */
	boolean removeDockableOpenListener(@Nonnull DockableOpenListener listener);

	/**
	 * @param listener
	 * 		Listener to add that observes existing {@link Dockable} items being moved to new {@link DockSpace} locations.
	 */
	void addDockableMoveListener(@Nonnull DockableMoveListener listener);

	/**
	 * @param listener
	 * 		Listener to remove that observes existing {@link Dockable} items being moved to new {@link DockSpace} locations.
	 */
	boolean removeDockableMoveListener(@Nonnull DockableMoveListener listener);

	/**
	 * @param listener
	 * 		Listener to add that observes existing {@link Dockable} items being closed.
	 */
	void addDockableCloseListener(@Nonnull DockableCloseListener listener);

	/**
	 * @param listener
	 * 		Listener to remove that observes existing {@link Dockable} items being closed.
	 */
	boolean removeDockableCloseListener(@Nonnull DockableCloseListener listener);

	/**
	 * @param listener
	 * 		Listener to add that observes existing {@link Dockable} items being selected.
	 */
	void addDockableSelectListener(@Nonnull DockableSelectListener listener);

	/**
	 * @param listener
	 * 		Listener to remove that observes existing {@link Dockable} items being selected.
	 */
	boolean removeDockableSelectListener(@Nonnull DockableSelectListener listener);

	/**
	 * @return Path to the {@code bento.css} stylesheet.
	 */
	@Nullable
	static String getCssPath() {
		URL resource = Bento.class.getClassLoader().getResource("bento.css");
		if (resource != null)
			return resource.toExternalForm();
		return null;
	}
}
