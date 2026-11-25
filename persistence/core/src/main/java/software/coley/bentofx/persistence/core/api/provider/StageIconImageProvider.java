package software.coley.bentofx.persistence.core.api.provider;

import javafx.scene.image.Image;

import java.util.Collection;

/**
 * Supplies the icon {@link Image}s for a restored {@code Stage}.
 *
 * <p>Implemented and supplied by the application, which passes an instance to
 * {@link DockingLayoutPersistenceProvider}'s {@code getLayoutRestorer}. It is
 * optional - passing {@code null} there leaves a restored {@code Stage}'s
 * icons unset.</p>
 *
 * @author Phil Bryant
 */
public interface StageIconImageProvider {

	/**
	 * Returns a collection of varying sizes for the {@link Image} to be used
	 * for {@code Stage} and {@code DragDropStage} instances. Assumes all
	 * {@code Stage} implementations use the same icons.
	 *
	 * @return a collection of varying sizes for the {@link Image} to be used
	 * for {@code Stage} and {@code DragDropStage} instances.
	 */
	Collection<Image> getStageIcons();
}
