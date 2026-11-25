package software.coley.bentofx.control.canvas;

import org.jspecify.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Objects;

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
	 * Wraps the given image as an ARGB source.
	 *
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
		int[] argb = fullArgbCache;
		if (argb == null) {
			argb = ArgbSource.super.getArgb();
			fullArgbCache = argb;
		}
		return argb;
	}

	/**
	 * Two sources are equal when they have the same dimensions and the same pixels.
	 * The pixels are read once and cached, so the wrapped image must not change
	 * after this source is first compared or hashed.
	 */
	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) return true;
		if (!(o instanceof final ArgbBufferedImageSource that)) return false;

		// Matching hashes don't prove equality, but differing ones disprove it cheaply.
		return getWidth() == that.getWidth()
				&& getHeight() == that.getHeight()
				&& hashCode() == that.hashCode()
				&& Arrays.equals(getArgb(), that.getArgb());
	}

	@Override
	public int hashCode() {
		if (hash == 0)
			hash = Objects.hash(getWidth(), getHeight(), Arrays.hashCode(getArgb()));
		return hash;
	}
}
