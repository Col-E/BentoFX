package software.coley.bentofx.content;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Side;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.header.HeaderRegion;
import software.coley.bentofx.header.HeaderView;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.RootContentLayout;
import software.coley.bentofx.layout.SplitContentLayout;
import software.coley.bentofx.util.DragDropStage;

import java.util.List;

/**
 * Multi {@link Dockable} display content in a tabular representation.
 *
 * @author Matt Coley
 */
non-sealed public interface TabbedContent extends Content {
	/**
	 * The auto-prune property, when {@code true}, will automatically this content from the
	 * parent {@link ContentLayout} when becoming {@link #isEmpty() empty}.
	 * <p/>
	 * Note: This pruning will cascade up if the parents then also become effectively empty.
	 * If this results in a {@link DragDropStage} seeing its only child {@link RootContentLayout} is empty the window
	 * will also automatically close if possible.
	 *
	 * @return Auto prune property.
	 */
	@Nonnull
	BooleanProperty autoPruneWhenEmptyProperty();

	/**
	 * The can-split property determines if drag-n-drop allows halving this content into a {@link SplitContentLayout}
	 * when content is dropped on one of the sides of this content.
	 *
	 * @return Splittable property.
	 */
	@Nonnull
	BooleanProperty canSplitProperty();

	/**
	 * The side property controls where the {@link HeaderRegion} is located in this content's display.
	 * Assigning this value to {@code null} is unsupported, don't do that.
	 *
	 * @return Header side property.
	 */
	@Nonnull
	ObjectProperty<Side> sideProperty();

	/**
	 * This property tracks what {@link Dockable} in {@link #getDockables()} is currently selected.
	 *
	 * @return Current selected dockable.
	 */
	@Nonnull
	ReadOnlyObjectProperty<Dockable> selectedDockableProperty();

	/**
	 * The menu factory on for tabbed content is what appears in the corner of the {@link HeaderView} display.
	 * Generally, you would put things in the menu like controls to modify properties of this instance.
	 *
	 * @return Current menu factory.
	 */
	@Nonnull
	ObjectProperty<TabbedContentMenuFactory> menuFactoryProperty();
}
