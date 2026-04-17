package software.coley.bentofx.control.canvas;

import org.jspecify.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * ARGB source wrapping a {@link BufferedImage}.
 *
 * @author Matt Coley
 */
public class ArgbBufferedImageSource implements ArgbSource {
	private final BufferedImage image;
	private int @Nullable[] fullArgbCache;
	private int hash;

	/**
	 * @param image
	 * 		Wrapped image.
	 */
	public ArgbBufferedImageSource(BufferedImage image) {
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
	public int @Nullable [] getArgb(int x, int y, int width, int height) {
		try {
			return image.getRGB(x, y, width, height, null, 0, width);
		} catch (Throwable t) {
			// Thrown when coordinates are out of bounds.
			return null;
		}
	}

	@Override
	public int[] getArgb() {
		// We will likely be using this a bit, so it makes sense to cache the result.
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
