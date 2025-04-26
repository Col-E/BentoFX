package software.coley.bentofx;

import jakarta.annotation.Nonnull;
import javafx.scene.layout.Region;

/**
 * Outline of an object that has an associated {@link Region}.
 *
 * @author Matt Coley
 */
public interface RegionBacked {
	/**
	 * @return Associated JavaFX region.
	 */
	@Nonnull
	Region getBackingRegion();
}
