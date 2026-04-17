package software.coley.bentofx.control.canvas;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import org.jspecify.annotations.Nullable;

import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * ARGB source wrapping an {@link Image}.
 *
 * @author Matt Coley
 */
public class ArgbImageSource implements ArgbSource {
	private final Image image;
	private int @Nullable[] fullArgbCache;
	private int hash;

	/**
	 * @param image
	 * 		Wrapped image.
	 */
	public ArgbImageSource(Image image) {
		this.image = image;
	}

	@Override
	public int getWidth() {
		return (int) image.getWidth();
	}

	@Override
	public int getHeight() {
		return (int) image.getHeight();
	}

	@Override
	public int getArgb(int x, int y) {
		try {
			return image.getPixelReader().getArgb(x, y);
		} catch (Throwable t) {
			// Thrown when coordinates are out of bounds.
			// Default to transparent black.
			return 0;
		}
	}

	@Override
	public int @Nullable[] getArgb(int x, int y, int width, int height) {
		try {
			IntBuffer buffer = IntBuffer.allocate(width * height);
			image.getPixelReader().getPixels(x, y, width, height, PixelFormat.getIntArgbInstance(), buffer, width);
			return buffer.array();
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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ArgbImageSource that)) return false;

		if (!image.equals(that.image)) return false;
		return Arrays.equals(fullArgbCache, that.fullArgbCache);
	}

	@Override
	public int hashCode() {
		if (hash == 0)
			hash = Arrays.hashCode(getArgb());
		return hash;
	}
}
