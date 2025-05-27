package software.coley.bentofx.space;

import jakarta.annotation.Nonnull;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Side;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.header.HeaderRegion;
import software.coley.bentofx.header.HeaderView;
import software.coley.bentofx.layout.DockLayout;
import software.coley.bentofx.layout.RootDockLayout;
import software.coley.bentofx.layout.SplitDockLayout;
import software.coley.bentofx.util.DragDropStage;

/**
 * Multi {@link Dockable} display content in a tabular representation.
 *
 * @author Matt Coley
 */
non-sealed public interface TabbedDockSpace extends DockSpace {
	/**
	 * The auto-prune property, when {@code true}, will automatically this space from the
	 * parent {@link DockLayout} when becoming {@link #isEmpty() empty}.
	 * <p/>
	 * Note: This pruning will cascade up if the parents then also become effectively empty.
	 * If this results in a {@link DragDropStage} seeing its only child {@link RootDockLayout} is empty the window
	 * will also automatically close if possible.
	 *
	 * @return Auto prune property.
	 */
	@Nonnull
	BooleanProperty autoPruneWhenEmptyProperty();

	/**
	 * The can-split property determines if drag-n-drop allows halving this space into a {@link SplitDockLayout}
	 * when content is dropped on one of the sides of this space.
	 *
	 * @return Splittable property.
	 */
	@Nonnull
	BooleanProperty canSplitProperty();

	/**
	 * The side property controls where the {@link HeaderRegion} is located in this space's display.
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
	 * The menu factory for tabbed spaces is what appears in the corner of the {@link HeaderView} display.
	 * Generally, you would put things in the menu like controls to modify properties of this instance.
	 *
	 * @return Current menu factory.
	 */
	@Nonnull
	ObjectProperty<TabbedSpaceMenuFactory> menuFactoryProperty();
}
