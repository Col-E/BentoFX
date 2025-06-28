package software.coley.bentofx.control.canvas;

import jakarta.annotation.Nonnull;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;

import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * Pixel painter instance backed by {@link PixelFormat#getIntArgbInstance()}.
 *
 * @author Matt Coley
 */
public class PixelPainterIntArgb implements PixelPainter<IntBuffer> {
	/** ARGB pixel buffer to draw with. */
	protected IntBuffer drawBuffer = PixelPainterUtils.EMPTY_BUFFER_I;
	/** Current width of an image. */
	protected int imageWidth;
	/** Current height of an image. */
	protected int imageHeight;

	@Override
	public boolean initialize(int width, int height) {
		if (imageWidth != width || imageHeight != height) {
			imageWidth = width;
			imageHeight = height;
			int drawBufferCapacity = drawBufferCapacity();
			if (drawBufferCapacity > drawBuffer.limit()) {
				drawBuffer = IntBuffer.wrap(new int[drawBufferCapacity]);
				return true;
			}
		}
		clear();
		return false;
	}

	@Override
	public void release() {
		imageWidth = 0;
		imageHeight = 0;
		drawBuffer = PixelPainterUtils.EMPTY_BUFFER_I;
	}

	@Override
	public void commit(@Nonnull PixelWriter pixelWriter) {
		pixelWriter.setPixels(
				0,
				0,
				imageWidth,
				imageHeight,
				getPixelFormat(),
				drawBuffer,
				imageWidth
		);
	}

	@Override
	public void fillRect(int x, int y, int width, int height, int color) {
		int yBound = Math.min(y + height, imageHeight);
		int xBound = Math.min(x + width, imageWidth);
		IntBuffer drawBuffer = this.drawBuffer;
		int capacity = drawBufferCapacity();
		for (int ly = y; ly < yBound; ly++) {
			int yOffset = ly * imageWidth;
			for (int lx = x; lx < xBound; lx++) {
				int index = yOffset + lx;
				if (index < capacity)
					drawBuffer.put(index, color);
			}
		}
	}

	@Override
	public void setColor(int x, int y, int color) {
		int i = adapt(x, y);
		if (i >= 0 && i < drawBufferCapacity())
			drawBuffer.put(i, color);
	}

	@Override
	public void clear() {
		Arrays.fill(drawBuffer.array(), 0, drawBufferCapacity(), 0);
	}

	@Nonnull
	@Override
	public IntBuffer getBuffer() {
		return drawBuffer;
	}

	@Nonnull
	@Override
	public PixelFormat<IntBuffer> getPixelFormat() {
		return PixelFormat.getIntArgbInstance();
	}

	protected int adapt(int x, int y) {
		return (y * imageWidth) + x;
	}

	protected int drawBufferCapacity() {
		return imageWidth * imageHeight;
	}
}
