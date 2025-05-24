package software.coley.bentofx;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.stage.Stage;
import software.coley.bentofx.builder.ContentBuilder;
import software.coley.bentofx.builder.DockableBuilder;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.content.EmptyContent;
import software.coley.bentofx.content.EmptyContentDisplayFactory;
import software.coley.bentofx.content.SingleContent;
import software.coley.bentofx.content.TabbedContent;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableCloseListener;
import software.coley.bentofx.dockable.DockableDestination;
import software.coley.bentofx.dockable.DockableMoveListener;
import software.coley.bentofx.dockable.DockableOpenListener;
import software.coley.bentofx.dockable.DockableSelectListener;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.impl.ImplBento;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.LeafContentLayout;
import software.coley.bentofx.layout.RootContentLayout;
import software.coley.bentofx.layout.SplitContentLayout;
import software.coley.bentofx.path.ContentPath;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.path.LayoutPath;
import software.coley.bentofx.util.BentoUtils;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Supplier;

/**
 * Controller for dockable content and layouts.
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
	 * @return New builder to create {@link RootContentLayout}, {@link ContentLayout} and {@link Content} instances.
	 */
	@Nonnull
	ContentBuilder newContentBuilder();

	/**
	 * @return New builder to create {@link Dockable} instances.
	 */
	@Nonnull
	DockableBuilder newDockableBuilder();

	/**
	 * Create a new empty content instance with some configurable {@link Node} content.
	 *
	 * @param parentLayout
	 * 		Parent layout to contain the new {@link EmptyContent}.
	 *
	 * @return A new empty content instance.
	 *
	 * @see #setEmptyDisplayFactory(EmptyContentDisplayFactory)
	 */
	@Nonnull
	Content newEmptyContent(@Nonnull ContentLayout parentLayout);

	/**
	 * @param factory
	 * 		Factory to create content for {@link EmptyContent} when placed into the UI.
	 */
	void setEmptyDisplayFactory(@Nullable EmptyContentDisplayFactory factory);

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
	 * @param identifier
	 * 		The identifier of some {@link ContentLayout} to find and replace.
	 * @param replacementProvider
	 * 		Supplier of a {@link ContentLayout} to replace the existing layout with.
	 *
	 * @return {@code true} when the existing layout was found and replaced.
	 */
	default boolean replaceContentLayout(@Nonnull String identifier, @Nonnull Supplier<ContentLayout> replacementProvider) {
		LayoutPath path = findLayout(identifier);
		if (path == null || path.layouts().isEmpty())
			return false;

		ContentLayout target = path.layouts().getLast();
		ContentLayout parentLayout = target.getParentLayout();
		if (parentLayout == null)
			return false;

		parentLayout.replaceChildLayout(target, replacementProvider.get());
		return true;
	}

	/**
	 * @param identifier
	 * 		The identifier of some {@link Content} to find and replace.
	 * @param replacementProvider
	 * 		Supplier of a {@link Content} to replace the existing content with.
	 *
	 * @return {@code true} when the existing content was found and replaced.
	 */
	default boolean replaceContent(@Nonnull String identifier, @Nonnull Supplier<Content> replacementProvider) {
		ContentPath path = findContent(identifier);
		if (path == null || path.layouts().isEmpty())
			return false;

		ContentLayout parent = path.layouts().getLast();
		switch (parent) {
			case LeafContentLayout leafContentLayout -> leafContentLayout.setContent(replacementProvider.get());
			case SplitContentLayout ignored ->
					throw new IllegalStateException(SplitContentLayout.class.getSimpleName() +
							" should never have a direct child " + Content.class.getSimpleName());
		}
		return true;
	}

	/**
	 * @return Unmodifiable list of all tracked {@link RootContentLayout}
	 * created by this bento instance that are present in any active scenes.
	 */
	@Nonnull
	ObservableList<RootContentLayout> getRootLayouts();

	/**
	 * @return List of all {@link Dockable} instanced tracked in this instance.
	 */
	@Nonnull
	default List<DockablePath> getAllDockables() {
		List<DockablePath> paths = new ArrayList<>();
		for (RootContentLayout root : getRootLayouts()) {
			Queue<ContentLayout> layouts = new ArrayDeque<>();
			layouts.add(root.getLayout());
			while (!layouts.isEmpty()) {
				ContentLayout layout = layouts.remove();
				switch (layout) {
					case LeafContentLayout leaf -> {
						switch (leaf.getContent()) {
							case EmptyContent ignored -> {}
							case SingleContent singleContent -> {
								ContentPath contentPath = singleContent.getPath();
								if (contentPath != null)
									paths.add(new DockablePath(contentPath, singleContent.getDockable()));
							}
							case TabbedContent tabbedContent -> {
								ContentPath contentPath = tabbedContent.getPath();
								if (contentPath != null)
									for (Dockable dockable : tabbedContent.getDockables())
										paths.add(new DockablePath(contentPath, dockable));
							}
						}
					}
					case SplitContentLayout ignored -> layouts.addAll(layout.getChildLayouts());
				}
			}
		}
		return paths;
	}

	/**
	 * Attempts to find a given {@link ContentLayout} from any child of any depth belonging to this bento instance.
	 * If a {@code null} is returned, then the given layout does not exist in any child belonging to this bento instance.
	 *
	 * @param layout
	 * 		Some layout to find.
	 *
	 * @return The path to the {@link ContentLayout} if found, otherwise {@code null}.
	 */
	@Nullable
	default LayoutPath findLayout(@Nonnull ContentLayout layout) {
		return findLayout(layout.getIdentifier());
	}

	/**
	 * Attempts to find a given {@link ContentLayout} from any child of any depth belonging to this bento instance.
	 * The identifier will be matched against {@link ContentLayout#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link ContentLayout} to find.
	 *
	 * @return The path to the {@link ContentLayout} if found, otherwise {@code null}.
	 */
	@Nullable
	default LayoutPath findLayout(@Nonnull String identifier) {
		for (RootContentLayout layout : getRootLayouts()) {
			LayoutPath path = layout.findLayout(identifier);
			if (path != null)
				return path;
		}
		return null;
	}

	/**
	 * Attempts to find a given {@link Content} from any child {@link ContentLayout} of any depth belonging to this bento instance.
	 * If a {@code null} is returned, then the given content does not exist in any child {@link ContentLayout}
	 * belonging to this bento instance.
	 *
	 * @param content
	 * 		Some content to find.
	 *
	 * @return The path to the {@link Dockable} if found, otherwise {@code null}.
	 */
	@Nullable
	default ContentPath findContent(@Nonnull Content content) {
		return findContent(content.getIdentifier());
	}

	/**
	 * Attempts to find a given {@link Content} from any child {@link ContentLayout} of any depth belonging to this bento instance.
	 * The identifier will be matched against {@link Content#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link Content} to find.
	 *
	 * @return The path to the {@link Content} if found, otherwise {@code null}.
	 */
	@Nullable
	default ContentPath findContent(@Nonnull String identifier) {
		for (RootContentLayout layout : getRootLayouts()) {
			ContentPath path = layout.findContent(identifier);
			if (path != null)
				return path;
		}
		return null;
	}

	/**
	 * Attempts to find a {@link Dockable}, based on the identifier present in the event's {@link Dragboard},
	 * contained within any child {@link Content} of any depth belonging to this bento instance.
	 * If a {@code null} is returned, then the given dockable does not exist in any child {@link ContentLayout}
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
	 * Attempts to find a given {@link Dockable} from any child {@link Content} of any depth belonging to this bento instance.
	 * If a {@code null} is returned, then the given dockable does not exist in any child {@link ContentLayout}
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
	 * Attempts to find a given {@link Dockable} from any child {@link Content} of any depth belonging to this bento instance.
	 * The identifier will be matched against {@link Dockable#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link Dockable} to find.
	 *
	 * @return The path to the {@link Dockable} if found, otherwise {@code null}.
	 */
	@Nullable
	default DockablePath findDockable(@Nonnull String identifier) {
		for (RootContentLayout layout : getRootLayouts()) {
			DockablePath path = layout.findDockable(identifier);
			if (path != null)
				return path;
		}
		return null;
	}

	/**
	 * Attempts to remove the given dockable from any child {@link Content} of any depth belonging to this bento instance.
	 * Be aware, this method will bypass {@link Dockable#closableProperty()}.
	 *
	 * @param dockable
	 * 		Dockable to remove.
	 *
	 * @return {@code true} if removed. {@code false} if not removed.
	 * @see #closeDockable(Dockable)
	 */
	default boolean removeDockable(@Nonnull Dockable dockable) {
		for (RootContentLayout layout : getRootLayouts()) {
			if (layout.removeDockable(dockable))
				return true;
		}
		return false;
	}

	/**
	 * Attempts to close the given dockable from any child {@link Content} of any depth belonging to this bento instance.
	 *
	 * @param dockable
	 * 		Dockable to remove.
	 *
	 * @return {@code true} if closed. {@code false} if not closed.
	 * @see #removeDockable(Dockable)
	 */
	default boolean closeDockable(@Nonnull Dockable dockable) {
		for (RootContentLayout layout : getRootLayouts()) {
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
	 * 		Listener to add that observes existing {@link Dockable} items being moved to new {@link Content} locations.
	 */
	void addDockableMoveListener(@Nonnull DockableMoveListener listener);

	/**
	 * @param listener
	 * 		Listener to remove that observes existing {@link Dockable} items being moved to new {@link Content} locations.
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
