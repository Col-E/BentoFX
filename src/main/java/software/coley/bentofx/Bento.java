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
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableDestination;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.impl.ImplBento;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.RootContentLayout;
import software.coley.bentofx.path.ContentPath;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.path.LayoutPath;
import software.coley.bentofx.util.BentoUtils;

import java.net.URL;

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
	 * @return Unmodifiable list of all tracked {@link RootContentLayout}
	 * created by this bento instance that are present in any active scenes.
	 */
	@Nonnull
	ObservableList<RootContentLayout> getRootLayouts();

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
	 * Attempts to remove a given dockable from any child {@link Content} of any depth belonging to this bento instance.
	 * Be aware, this method will bypass {@link Dockable#closableProperty()}.
	 *
	 * @param dockable
	 * 		Dockable to remove.
	 *
	 * @return {@code true} if removed. {@code false} if not removed.
	 */
	default boolean removeDockable(@Nonnull Dockable dockable) {
		for (RootContentLayout layout : getRootLayouts()) {
			if (layout.removeDockable(dockable))
				return true;
		}
		return false;
	}

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
