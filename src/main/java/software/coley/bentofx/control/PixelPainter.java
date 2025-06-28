package software.coley.bentofx.control;

import jakarta.annotation.Nonnull;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;

import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * Helper class to draw pixels into a temporary buffer.
 *
 * @author Matt Coley
 */
public final class PixelPainter {
	/** ARGB pixel buffer to draw with. */
	private int[] drawBuffer = new int[0];
	/** Current width of an image. */
	private int imageWidth;
	/** Current height of an image. */
	private int imageHeight;

	/**
	 * Initializes the painter.
	 *
	 * @param width
	 * 		Assigned width.
	 * @param height
	 * 		Assigned height.
	 */
	public void initialize(int width, int height) {
		if (imageWidth != width || imageHeight != height) {
			imageWidth = width;
			imageHeight = height;
			int drawBufferCapacity = width * height;
			if (drawBufferCapacity > drawBuffer.length) {
				drawBuffer = new int[drawBufferCapacity];
				return;
			}
		}
		clear();
	}

	/**
	 * Releases any resources held by the painter.
	 * Call {@link #initialize(int, int)} to initialize the painter again.
	 */
	public void release() {
		imageWidth = 0;
		imageHeight = 0;
		drawBuffer = new int[0];
	}

	/**
	 * Commits any pending state to the display.
	 *
	 * @param pixelWriter
	 * 		Pixel writer.
	 */
	public void commit(@Nonnull PixelWriter pixelWriter) {
		pixelWriter.setPixels(
				0,
				0,
				imageWidth,
				imageHeight,
				PixelFormat.getIntArgbInstance(),
				IntBuffer.wrap(drawBuffer, 0, drawBufferCapacity()),
				imageWidth
		);
	}

	/**
	 * Fills the given rectangle with the given color.
	 *
	 * @param x
	 * 		Rect x coordinate.
	 * @param y
	 * 		Rect y coordinate.
	 * @param width
	 * 		Rect width.
	 * @param height
	 * 		Rect height.
	 * @param borderSize
	 * 		Border size <i>(inset into the rect)</i>.
	 * @param color
	 * 		Color to fill.
	 * @param borderColor
	 * 		Color to draw as a border.
	 */
	public void fillBorderedRect(double x, double y, double width, double height, int borderSize, int color, int borderColor) {
		fillRect(x + borderSize, y + borderSize, width - borderSize * 2, height - borderSize * 2, color);
		drawRect(x, y, width, height, borderSize, borderColor);
	}

	/**
	 * Fills the given rectangle with the given color.
	 *
	 * @param x
	 * 		Rect x coordinate.
	 * @param y
	 * 		Rect y coordinate.
	 * @param width
	 * 		Rect width.
	 * @param height
	 * 		Rect height.
	 * @param color
	 * 		Color to fill.
	 */
	public void fillRect(double x, double y, double width, double height, int color) {
		fillRect((int) x, (int) y, (int) width, (int) height, color);
	}

	/**
	 * Fills the given rectangle with the given color.
	 *
	 * @param x
	 * 		Rect x coordinate.
	 * @param y
	 * 		Rect y coordinate.
	 * @param width
	 * 		Rect width.
	 * @param height
	 * 		Rect height.
	 * @param color
	 * 		Color to fill.
	 */
	public void fillRect(int x, int y, int width, int height, int color) {
		int yBound = Math.min(y + height, imageHeight);
		int xBound = Math.min(x + width, imageWidth);
		int[] drawBuffer = this.drawBuffer;
		int capacity = drawBufferCapacity();
		for (int ly = y; ly < yBound; ly++) {
			int yOffset = ly * imageWidth;
			for (int lx = x; lx < xBound; lx++) {
				int index = yOffset + lx;
				if (index < capacity) drawBuffer[index] = color;
			}
		}
	}

	/**
	 * Draws the edges of a given rectangle with the given color.
	 *
	 * @param x
	 * 		Rect x coordinate.
	 * @param y
	 * 		Rect y coordinate.
	 * @param width
	 * 		Rect width.
	 * @param height
	 * 		Rect height.
	 * @param borderSize
	 * 		Border size <i>(inset into the rect)</i>.
	 * @param color
	 * 		Color to draw.
	 */
	public void drawRect(double x, double y, double width, double height, double borderSize, int color) {
		drawRect((int) x, (int) y, (int) width, (int) height, (int) borderSize, color);
	}

	/**
	 * Draws the edges of a given rectangle with the given color.
	 *
	 * @param x
	 * 		Rect x coordinate.
	 * @param y
	 * 		Rect y coordinate.
	 * @param width
	 * 		Rect width.
	 * @param height
	 * 		Rect height.
	 * @param borderSize
	 * 		Border size <i>(inset into the rect)</i>.
	 * @param color
	 * 		Color to draw.
	 */
	public void drawRect(int x, int y, int width, int height, int borderSize, int color) {
		fillRect(x, y, width, borderSize, color);
		fillRect(x, y + height - borderSize, width, borderSize, color);
		fillRect(x, y + borderSize, borderSize, height - borderSize, color);
		fillRect(x + width - borderSize, y + borderSize, borderSize, height - borderSize, color);
	}

	/**
	 * Draws a horizontal line from the given point/width with the given color.
	 *
	 * @param x
	 * 		Line x coordinate.
	 * @param y
	 * 		Line y coordinate.
	 * @param lineWidth
	 * 		Width of the line <i>(Centered around y)</i>.
	 * @param lineLength
	 * 		Line width.
	 * @param color
	 * 		Color to draw.
	 */
	public void drawHorizontalLine(double x, double y, double lineLength, double lineWidth, int color) {
		drawHorizontalLine((int) x, (int) y, (int) lineLength, (int) lineWidth, color);
	}

	/**
	 * Draws a horizontal line from the given point/width with the given color.
	 *
	 * @param x
	 * 		Line x coordinate.
	 * @param y
	 * 		Line y coordinate.
	 * @param lineWidth
	 * 		Width of the line <i>(Centered around y)</i>.
	 * @param lineLength
	 * 		Line length.
	 * @param color
	 * 		Color to draw.
	 */
	public void drawHorizontalLine(int x, int y, int lineLength, int lineWidth, int color) {
		fillRect(x, y - Math.max(1, lineWidth / 2), lineLength, lineWidth, color);
	}

	/**
	 * Draws a vertical line from the given point/height with the given color.
	 *
	 * @param x
	 * 		Line x coordinate.
	 * @param y
	 * 		Line y coordinate.
	 * @param lineWidth
	 * 		Width of the line <i>(Centered around x)</i>.
	 * @param lineLength
	 * 		Line height.
	 * @param color
	 * 		Color to draw.
	 */
	public void drawVerticalLine(double x, double y, double lineLength, double lineWidth, int color) {
		drawVerticalLine((int) x, (int) y, (int) lineLength, (int) lineWidth, color);
	}

	/**
	 * Draws a vertical line from the given point/height with the given color.
	 *
	 * @param x
	 * 		Line x coordinate.
	 * @param y
	 * 		Line y coordinate.
	 * @param lineWidth
	 * 		Width of the line <i>(Centered around x)</i>.
	 * @param lineLength
	 * 		Line height.
	 * @param color
	 * 		Color to draw.
	 */
	public void drawVerticalLine(int x, int y, int lineLength, int lineWidth, int color) {
		fillRect(x - Math.max(1, lineWidth / 2), y, lineWidth, lineLength, color);
	}

	/**
	 * Set a given pixel to the given color.
	 *
	 * @param x
	 * 		X coordinate.
	 * @param y
	 * 		Y coordinate.
	 * @param color
	 * 		Color to set.
	 */
	public void setColor(int x, int y, int color) {
		int i = adapt(x, y);
		if (i >= 0 && i < drawBufferCapacity())
			drawBuffer[i] = color;
	}

	/**
	 * Clears the buffer.
	 */
	public void clear() {
		Arrays.fill(drawBuffer, 0, drawBufferCapacity(), 0);
	}

	private int adapt(int x, int y) {
		return (y * imageWidth) + x;
	}

	private int drawBufferCapacity() {
		return imageWidth * imageHeight;
	}
}
