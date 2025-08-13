package software.coley.bentofx.control.canvas;

import jakarta.annotation.Nonnull;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;

import java.nio.IntBuffer;

/**
 * ARGB source wrapping an {@link Image}.
 *
 * @author Matt Coley
 */
public class ArgbImageSource implements ArgbSource {
	private final Image image;
	private int[] fullArgbCache;

	/**
	 * @param image
	 * 		Wrapped image.
	 */
	public ArgbImageSource(@Nonnull Image image) {
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
	public int[] getArgb(int x, int y, int width, int height) {
		try {
			IntBuffer buffer = IntBuffer.allocate(width * height);
			image.getPixelReader().getPixels(x, y, width, height, PixelFormat.getIntArgbInstance(), buffer, width);
			return buffer.array();
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
}
