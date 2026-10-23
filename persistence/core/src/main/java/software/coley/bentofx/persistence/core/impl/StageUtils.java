package software.coley.bentofx.persistence.core.impl;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Utility class containing methods relating to JavaFX {@link Stage}.
 *
 * @author Phil Bryant
 */
public class StageUtils {

	private StageUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Returns an immutable {@link List} containing all {@link Stage} instances.
	 *
	 * @return an immutable {@link List} containing all {@link Stage} instances.
	 */
	public static List<Stage> getAllStages() {
		return Window.getWindows()
				.stream()
				.filter(
						Stage.class::isInstance
				)
				.map(
						Stage.class::cast
				)
				.toList();
	}

	/**
	 * Calculates the horizontal position of a {@link Stage}, bounded within the
	 * horizontal boundary of all {@link Screen}.
	 * @param stage the {@link Stage} whose horizontally bounded {@link Screen}
	 *              position is to the determined.
	 * @param stageX the unbounded horizontal position of the {@link Stage}.
	 * @return the horizontal position of a {@link Stage}, bounded within the
	 * horizontal boundary of all {@link Screen}.
	 */
	public static double getXInScreenBounds(
			final Stage stage,
			final double stageX
	) {
		final Rectangle2D allScreenBounds = getAllScreenBounds();

		if (allScreenBounds == null) {
			return stageX;
		}

		double boundedX = stageX;
		if (stageX < allScreenBounds.getMinX()) {
			// Ensure the X coordinate is not left of the minimum allowed X
			boundedX = allScreenBounds.getMinX();
		} else if (stageX + stage.getWidth() > allScreenBounds.getMaxX()) {
			// Ensure that the stage doesn't extend beyond the maximum X boundary
			boundedX = allScreenBounds.getMaxX() - stage.getWidth();
		}
		return boundedX;
	}

	/**
	 * Calculates the vertical position of a {@link Stage}, bounded within the
	 * vertical boundary of all {@link Screen}.
	 * @param stage the {@link Stage} whose vertically bounded {@link Screen}
	 *              position is to be determined.
	 * @param stageY the unbounded vertical position of the {@link Stage}.
	 * @return the vertical position of a {@link Stage}, bounded within the
	 * vertical boundary of all {@link Screen}.
	 */
	public static double getYInScreenBounds(
			final Stage stage,
			final double stageY
	) {
		final Rectangle2D allScreenBounds = getAllScreenBounds();

		if (allScreenBounds == null) {
			return stageY;
		}

		double boundedY = stageY; // Start with the unbounded Y position

		if (stageY < allScreenBounds.getMinY()) {
			// Ensure the Y coordinate is not above the minimum allowed Y
			boundedY = allScreenBounds.getMinY();
		}
		else if (stageY + stage.getHeight() > allScreenBounds.getMaxY()) {
			// Ensure that the stage doesn't extend beyond the maximum Y boundary
			boundedY = allScreenBounds.getMaxY() - stage.getHeight();
		}

		return boundedY; // Return the adjusted Y position
	}

	/**
	 * Calculates the boundary enclosing every {@link Screen}.
	 *
	 * @return the boundary enclosing every {@link Screen}, or {@code null} when
	 * there are no screens, and so nothing to bound a position against.
	 */
	private static @Nullable Rectangle2D getAllScreenBounds() {
		final List<Screen> screens = Screen.getScreens();

		// Returning null rather than a rectangle built from the sentinels below.
		// Rectangle2D rejects a negative width so with no screens the subtraction
		// at the end throws IllegalArgumentException, which  would surface as a
		// lost layout rather than a crash, because
		// DockingLayoutRestorer.restoreLayout catches the failure and substitutes
		// the default layout.
		if (screens.isEmpty()) {
			return null;
		}

		// Infinities, not MIN_VALUE/MAX_VALUE. Double.MIN_VALUE is the smallest
		// *positive* double (~4.9e-324), so as a starting maximum it is beaten by
		// any positive coordinate but not by a negative one - a screen placed
		// entirely left of or above the origin, which a secondary monitor can be,
		// would have been clipped to ~0. MAX_VALUE does work as a starting
		// minimum, but pairing it with the corrected maxima reads as if the two
		// were symmetric when they are not.
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (final Screen screen : screens) {
			final Rectangle2D bounds = screen.getVisualBounds();
			minX = Math.min(bounds.getMinX(), minX);
			minY = Math.min(bounds.getMinY(), minY);
			maxX = Math.max(bounds.getMaxX(), maxX);
			maxY = Math.max(bounds.getMaxY(), maxY);
		}

		return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
	}
}
