package software.coley.bentofx.control.canvas;

import jakarta.annotation.Nonnull;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * ARGB source wrapping a {@link BufferedImage}.
 *
 * @author Matt Coley
 */
public class ArgbBufferedImageSource implements ArgbSource {
	private final BufferedImage image;
	private int[] fullArgbCache;
	private int hash;

	/**
	 * @param image
	 * 		Wrapped image.
	 */
	public ArgbBufferedImageSource(@Nonnull BufferedImage image) {
		this.image = image;
	}

	@Override
	public int getWidth() {
		return image.getWidth();
	}

	@Override
	public int getHeight() {
		return image.getHeight();
	}

	@Override
	public int getArgb(int x, int y) {
		try {
			return image.getRGB(x, y);
		} catch (Throwable t) {
			// Thrown when coordinates are out of bounds.
			// Default to transparent black.
			return 0;
		}
	}

	@Override
	public int[] getArgb(int x, int y, int width, int height) {
		try {
			return image.getRGB(x, y, width, height, null, 0, width);
		} catch (Throwable t) {
			// Thrown when coordinates are out of bounds.
			return null;
		}
	}

	@Nonnull
	@Override
	public int[] getArgb() {
		// We will likely be using this a bit, so it makes sense to cache the result.
		if (fullArgbCache == null)
			fullArgbCache = ArgbSource.super.getArgb();
		return fullArgbCache;
	}

	@Override
	public int hashCode() {
		if (hash == 0)
			hash = Arrays.hashCode(getArgb());
		return hash;
	}
}
