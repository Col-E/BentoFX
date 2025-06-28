package software.coley.bentofx.control;

import jakarta.annotation.Nonnull;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;

import java.util.Arrays;

/**
 * This is a very simple alternative to {@link Canvas} that <i>does not</i> keep track of draw operations.
 * In some super niche cases, the default canvas is a memory hog, where this operates on a flat ARGB {@code int[]}.
 *
 * @author Matt Coley
 */
public class PixelCanvas extends Region {
	private static final int OP_RECT_FILL = 100;
	private static final int OP_PX_SET = 200;
	/** Wrapped display. */
	private final ImageView view = new ImageView();
	/** The image to draw with. */
	private WritableImage image;
	/** ARGB pixel buffer to draw with. */
	private int[] drawBuffer;
	/** Current width of {@link #image}. */
	private int imageWidth;
	/** Current height of {@link #image}. */
	private int imageHeight;
	/** Last committed draw hash. */
	private int lastDrawHash;
	/** Current draw hash. */
	private int currentDrawHash;

	/**
	 * New pixel canvas.
	 */
	public PixelCanvas() {
		getChildren().add(view);

		view.fitWidthProperty().bind(widthProperty());
		view.fitHeightProperty().bind(heightProperty());

		widthProperty().addListener((ob, old, cur) -> markDirty());
		heightProperty().addListener((ob, old, cur) -> markDirty());
	}

	/**
	 * New pixel canvas.
	 *
	 * @param width
	 * 		Assigned width.
	 * @param height
	 * 		Assigned height.
	 */
	public PixelCanvas(int width, int height) {
		getChildren().add(view);

		setMinSize(width, height);
		setMaxSize(width, height);
		setPrefSize(width, height);

		view.setFitWidth(width);
		view.setFitHeight(height);
	}

	/**
	 * Commits any pending state in the canvas buffer to the display.
	 */
	public void commit() {
		if (lastDrawHash == currentDrawHash) return;
		lastDrawHash = currentDrawHash;

		checkDirty();
		image.getPixelWriter().setPixels(0, 0, imageWidth, imageHeight, PixelFormat.getIntArgbInstance(), drawBuffer, 0, imageWidth);
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
		updateDrawHash(hash(OP_RECT_FILL, x, y, width, height));
		adaptEach(x, y, width, height, (lx, ly, i) -> {
			if (i >= 0 && i < drawBuffer.length)
				drawBuffer[i] = color;
		});
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
		updateDrawHash(hash(OP_PX_SET, x, y, color));
		int i = adapt(x, y);
		if (i >= 0 && i < drawBuffer.length)
			drawBuffer[i] = color;
	}

	/**
	 * Clears the canvas buffer. Call {@link #commit()} to update the display.
	 */
	public void clear() {
		currentDrawHash = 0;
		checkDirty();
		Arrays.fill(drawBuffer, 0);
	}

	/**
	 * Update the draw hash.
	 *
	 * @param hash
	 * 		Next operation hash.
	 */
	protected void updateDrawHash(int hash) {
		if (currentDrawHash == 0) currentDrawHash = hash;
		else currentDrawHash = currentDrawHash * 31 + hash;
		checkDirty();
	}

	/**
	 * @param values
	 * 		Values to hash.
	 *
	 * @return Generated hash.
	 */
	protected static int hash(int... values) {
		int hash = values[0];
		for (int i = 1; i < values.length; i++)
			hash = 31 * hash + values[i];
		return hash;
	}

	/**
	 * A dirty canvas means the buffer state is outdated and needs to be {@link #reallocate() reallocated}.
	 * This will be done automatically when calling drawing methods.
	 *
	 * @return {@code true} when this canvas is {@code dirty}.
	 */
	public boolean isDirty() {
		return image == null;
	}

	/**
	 * Marks the canvas as dirty.
	 */
	public void markDirty() {
		image = null;
	}

	/**
	 * Check if {@code dirty} and allocates a new buffer when dirty.
	 */
	protected void checkDirty() {
		if (isDirty())
			reallocate();
	}

	/**
	 * Allocate the image and associated values.
	 */
	protected void reallocate() {
		imageWidth = (int) Math.max(1, getWidth());
		imageHeight = (int) Math.max(1, getHeight());
		image = new WritableImage(imageWidth, imageHeight);
		drawBuffer = new int[imageWidth * imageHeight];
		view.setImage(image);
	}

	/**
	 * @param x
	 * 		Canvas x coordinate.
	 * @param y
	 * 		Canvas y coordinate.
	 *
	 * @return Offset into {@link #drawBuffer}.
	 */
	protected int adapt(int x, int y) {
		return (y * imageWidth) + x;
	}

	/**
	 * @param x
	 * 		Rect x coordinate.
	 * @param y
	 * 		Rect y coordinate.
	 * @param width
	 * 		Rect width.
	 * @param height
	 * 		Rect height.
	 * @param consumer
	 * 		Consumer to operate on all locations in the given rectangle.
	 */
	protected void adaptEach(int x, int y, int width, int height, @Nonnull XYConsumer consumer) {
		int yBound = Math.min(y + height, imageHeight);
		int xBound = Math.min(x + width, imageWidth);
		for (int ly = y; ly < yBound; ly++) {
			int yOffset = ly * imageWidth;
			for (int lx = x; lx < xBound; lx++) {
				consumer.consume(lx, ly, yOffset + lx);
			}
		}
	}

	/**
	 * @see #adaptEach(int, int, int, int, XYConsumer)
	 */
	protected interface XYConsumer {
		/**
		 * @param x
		 * 		Canvas x coordinate.
		 * @param y
		 * 		Canvas y coordinate.
		 * @param xy
		 * 		Offset into {@link #drawBuffer}.
		 */
		void consume(int x, int y, int xy);
	}
}
