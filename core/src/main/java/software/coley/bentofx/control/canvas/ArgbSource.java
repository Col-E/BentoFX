package software.coley.bentofx.control.canvas;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * ARGB source to wrap arbitrary inputs representing image data.
 *
 * @author Matt Coley.
 */
public interface ArgbSource {
	/**
	 * @return Image width.
	 */
	int getWidth();

	/**
	 * @return Image height.
	 */
	int getHeight();

	/**
	 * @param x
	 * 		Image X coordinate.
	 * @param y
	 * 		Image X coordinate.
	 *
	 * @return ARGB {@code int} at coordinate.
	 * Defaults to {@code 0} for any coordinate out of the image bounds.
	 */
	int getArgb(int x, int y);

	/**
	 * @param x
	 * 		Image X coordinate.
	 * @param y
	 * 		Image X coordinate.
	 * @param width
	 * 		Width of image section to grab.
	 * @param height
	 * 		Height of image section to grab.
	 *
	 * @return ARGB {@code int[]} at coordinates for the given width/height.
	 * {@code null} when coordinates are out of the image bounds.
	 */
	@Nullable
	int[] getArgb(int x, int y, int width, int height);

	/**
	 * @return ARGB {@code int[]} for the full image.
	 */
	@Nonnull
	default int[] getArgb() {
		return Objects.requireNonNull(getArgb(0, 0, getWidth(), getHeight()),
				"Failed computing ARGB for full image dimensions");
	}
}
