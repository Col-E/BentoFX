package software.coley.bentofx.control.canvas;

import jakarta.annotation.Nonnull;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Pixel painter instance backed by {@link PixelFormat#getByteBgraInstance()}.
 *
 * @author Matt Coley
 */
public class PixelPainterByteBgra implements PixelPainter<ByteBuffer> {
	protected static final int DATA_SIZE = 4;
	/** ARGB pixel buffer to draw with. */
	protected ByteBuffer drawBuffer = PixelPainterUtils.EMPTY_BUFFER_B;
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
				drawBuffer = ByteBuffer.wrap(new byte[drawBufferCapacity]);
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
		drawBuffer = PixelPainterUtils.EMPTY_BUFFER_B;
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
				imageWidth * DATA_SIZE
		);
	}

	@Override
	public void fillRect(int x, int y, int width, int height, int color) {
		byte alpha = (byte) ((color >> 24) & 0xFF);
		byte red = (byte) ((color >> 16) & 0xFF);
		byte green = (byte) ((color >> 8) & 0xFF);
		byte blue = (byte) (color & 0xFF);
		int yBound = Math.min(y + height, imageHeight);
		int xBound = Math.min(x + width, imageWidth);
		ByteBuffer drawBuffer = this.drawBuffer;
		int capacity = drawBufferCapacity();
		for (int ly = y; ly < yBound; ly++) {
			int yOffset = ly * imageWidth;
			for (int lx = x; lx < xBound; lx++) {
				int index = (yOffset + lx) * DATA_SIZE;
				if (index < capacity) {
					drawBuffer.put(index, blue);
					drawBuffer.put(index + 1, green);
					drawBuffer.put(index + 2, red);
					drawBuffer.put(index + 3, alpha);
				}
			}
		}
	}

	@Override
	public void drawImage(int x, int y, @Nonnull ArgbSource source) {
		int sourceWidth = source.getWidth();
		int sourceHeight = source.getHeight();
		int[] argb = source.getArgb(0, 0, sourceWidth, sourceHeight);
		if (argb == null)
			return;
		int yBound = Math.min(y + sourceHeight, imageHeight);
		int xBound = Math.min(x + sourceWidth, imageWidth);
		for (int ly = y; ly < yBound; ly++) {
			int yOffsetSource = (ly - y) * sourceWidth;
			for (int lx = x; lx < xBound; lx++) {
				int sourceIndex = yOffsetSource + (lx - x);
				if (sourceIndex < argb.length)
					setColor(lx, ly, argb[sourceIndex]);
			}
		}
	}

	@Override
	public void drawImage(int x, int y, int sx, int sy, int sw, int sh, @Nonnull ArgbSource source) {
		int[] argb = source.getArgb(sx, sy, sw, sh);
		if (argb == null)
			return;
		int yBound = Math.min(y + sh, imageHeight);
		int xBound = Math.min(x + sw, imageWidth);
		for (int ly = y; ly < yBound; ly++) {
			int yOffsetSource = (ly - y) * sw;
			for (int lx = x; lx < xBound; lx++) {
				int sourceIndex = yOffsetSource + (lx - x);
				if (sourceIndex < argb.length)
					setColor(lx, ly, argb[sourceIndex]);
			}
		}
	}

	@Override
	public void setColor(int x, int y, int color) {
		int i = ((y * imageWidth) + x) * DATA_SIZE;
		if (i >= 0 && i < drawBufferCapacity()) {
			byte alpha = (byte) ((color >> 24) & 0xFF);
			byte red = (byte) ((color >> 16) & 0xFF);
			byte green = (byte) ((color >> 8) & 0xFF);
			byte blue = (byte) (color & 0xFF);
			drawBuffer.put(i, blue);
			drawBuffer.put(i + 1, green);
			drawBuffer.put(i + 2, red);
			drawBuffer.put(i + 3, alpha);
		}
	}

	@Override
	public void clear() {
		Arrays.fill(drawBuffer.array(), 0, drawBufferCapacity(), (byte) 0);
	}

	@Nonnull
	@Override
	public ByteBuffer getBuffer() {
		return drawBuffer;
	}

	@Nonnull
	@Override
	public PixelFormat<ByteBuffer> getPixelFormat() {
		return PixelFormat.getByteBgraInstance();
	}

	protected int drawBufferCapacity() {
		return (imageWidth * imageHeight) * DATA_SIZE;
	}
}
