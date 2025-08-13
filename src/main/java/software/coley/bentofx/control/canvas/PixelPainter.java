package software.coley.bentofx.control.canvas;

import jakarta.annotation.Nonnull;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;

import java.nio.Buffer;

/**
 * Outline of a helper for drawing pixels into a temporary buffer.
 * <br>
 * Implementations may change how the buffer is constructed and interpreted.
 *
 * @param <B>
 * 		Backing buffer type.
 *
 * @author Matt Coley
 * @author xxDark
 * @see PixelCanvas
 */
public interface PixelPainter<B extends Buffer> {
	/**
	 * Initializes the painter.
	 *
	 * @param width
	 * 		Assigned width.
	 * @param height
	 * 		Assigned height.
	 *
	 * @return {@code true} when the backing buffer was modified as a result of this operation.
	 * {@code false} when the backing buffer has already been initialized to the given dimensions.
	 */
	boolean initialize(int width, int height);

	/**
	 * Releases any resources held by the painter.
	 * Call {@link #initialize(int, int)} to initialize the painter again.
	 */
	void release();

	/**
	 * Commits any pending state to the display.
	 *
	 * @param pixelWriter
	 * 		Pixel writer.
	 */
	void commit(@Nonnull PixelWriter pixelWriter);

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
	 * 		ARGB Color to fill.
	 * @param borderColor
	 * 		ARGB Color to draw as a border.
	 */
	default void fillBorderedRect(double x, double y, double width, double height, int borderSize, int color, int borderColor) {
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
	 * 		ARGB Color to fill.
	 */
	default void fillRect(double x, double y, double width, double height, int color) {
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
	 * 		ARGB Color to fill.
	 */
	void fillRect(int x, int y, int width, int height, int color);

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
	 * 		ARGB Color to draw.
	 */
	default void drawRect(double x, double y, double width, double height, double borderSize, int color) {
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
	 * 		ARGB Color to draw.
	 */
	default void drawRect(int x, int y, int width, int height, int borderSize, int color) {
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
	 * 		ARGB Color to draw.
	 */
	default void drawHorizontalLine(double x, double y, double lineLength, double lineWidth, int color) {
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
	 * 		ARGB Color to draw.
	 */
	default void drawHorizontalLine(int x, int y, int lineLength, int lineWidth, int color) {
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
	 * 		ARGB Color to draw.
	 */
	default void drawVerticalLine(double x, double y, double lineLength, double lineWidth, int color) {
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
	 * 		ARGB Color to draw.
	 */
	default void drawVerticalLine(int x, int y, int lineLength, int lineWidth, int color) {
		fillRect(x - Math.max(1, lineWidth / 2), y, lineWidth, lineLength, color);
	}

	/**
	 * Draws an image at the given coordinates.
	 *
	 * @param x
	 * 		X coordinate to draw image at.
	 * @param y
	 * 		Y coordinate to draw image at.
	 * @param image
	 * 		Image to draw.
	 */
	void drawImage(int x, int y, @Nonnull ArgbSource image);

	/**
	 * Draws an image at the given coordinates.
	 *
	 * @param x
	 * 		X coordinate to draw image at.
	 * @param y
	 * 		Y coordinate to draw image at.
	 * @param sx
	 * 		X coordinate offset into the image.
	 * @param sy
	 * 		Y coordinate offset into the image.
	 * @param sw
	 * 		Width of the image to draw.
	 * @param sh
	 * 		Height of the image to draw.
	 * @param image
	 * 		Image to draw.
	 */
	void drawImage(int x, int y, int sx, int sy, int sw, int sh, @Nonnull ArgbSource image);

	/**
	 * Set a given pixel to the given color.
	 *
	 * @param x
	 * 		X coordinate.
	 * @param y
	 * 		Y coordinate.
	 * @param color
	 * 		ARGB Color to set.
	 */
	void setColor(int x, int y, int color);

	/**
	 * Clears the buffer.
	 */
	void clear();

	/**
	 * @return Backing buffer.
	 */
	@Nonnull
	B getBuffer();

	/**
	 * @return Pixel format for contents in this painter's buffer.
	 */
	@Nonnull
	PixelFormat<B> getPixelFormat();
}
