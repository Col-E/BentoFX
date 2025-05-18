package software.coley.bentofx.layout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.bentofx.Bento;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.RegionBacked;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.content.EmptyContent;
import software.coley.bentofx.path.ContentPath;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.path.LayoutPath;
import software.coley.bentofx.path.PathBuilder;

/**
 * This is the top level of any docking-enabled layout. By itself it acts very similar to {@link LeafContentLayout}
 * except that this houses a {@link ContentLayout} as its sole child.
 *
 * @author Matt Coley
 * @see Bento#newContentBuilder()
 */
public interface RootContentLayout extends Identifiable, RegionBacked {
	/**
	 * Attempts to find a given {@link ContentLayout} from any child of any depth belonging to this root.
	 * If a {@code null} is returned, then the given layout does not exist in any child belonging to this root.
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
	 * Attempts to find a given {@link ContentLayout} from any child of any depth belonging to this root.
	 * The identifier will be matched against {@link ContentLayout#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link ContentLayout} to find.
	 *
	 * @return The path to the {@link ContentLayout} if found, otherwise {@code null}.
	 */
	@Nullable
	default LayoutPath findLayout(@Nonnull String identifier) {
		return getLayout().findLayout(newPathBuilder(), identifier);
	}

	/**
	 * Attempts to find a given {@link Content} from any child {@link ContentLayout} of any depth belonging to this root.
	 * If a {@code null} is returned, then the given content does not exist in any child {@link ContentLayout}
	 * belonging to this root.
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
	 * Attempts to find a given {@link Content} from any child {@link ContentLayout} of any depth belonging to this root.
	 * The identifier will be matched against {@link Content#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link Content} to find.
	 *
	 * @return The path to the {@link Content} if found, otherwise {@code null}.
	 */
	@Nullable
	default ContentPath findContent(@Nonnull String identifier) {
		return getLayout().findContent(newPathBuilder(), identifier);
	}

	/**
	 * Attempts to find a given {@link Dockable} from any child {@link Content} of any depth belonging to this root.
	 * If a {@code null} is returned, then the given dockable does not exist in any child {@link ContentLayout}
	 * belonging to this root.
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
	 * Attempts to find a given {@link Dockable} from any child {@link Content} of any depth belonging to this root.
	 * The identifier will be matched against {@link Dockable#getIdentifier()}.
	 *
	 * @param identifier
	 * 		The identifier of some {@link Dockable} to find.
	 *
	 * @return The path to the {@link Dockable} if found, otherwise {@code null}.
	 */
	@Nullable
	default DockablePath findDockable(@Nonnull String identifier) {
		return getLayout().findDockable(newPathBuilder(), identifier);
	}

	/**
	 * Attempts to remove a given dockable from any child {@link Content} of any depth belonging to this root.
	 * Be aware, this method will bypass {@link Dockable#closableProperty()}.
	 *
	 * @param dockable
	 * 		Dockable to remove.
	 *
	 * @return {@code true} if removed. {@code false} if not removed.
	 */
	default boolean removeDockable(@Nonnull Dockable dockable) {
		return getLayout().removeDockable(dockable);
	}

	/**
	 * Updates the current child layout.
	 *
	 * @param layout
	 * 		The new child layout to assign.
	 */
	void setLayout(@Nonnull ContentLayout layout);

	/**
	 * @return The current child layout.
	 */
	@Nonnull
	ContentLayout getLayout();

	/**
	 * Removes any {@link #getLayout() child layot} and replaces it with a {@link LeafContentLayout}
	 * containing a single {@link EmptyContent}.
	 */
	void clearLayout();

	/**
	 * @return New path builder for use in {@code findX(...)} methods.
	 */
	@Nonnull
	default PathBuilder newPathBuilder() {
		return new PathBuilder(this);
	}
}
