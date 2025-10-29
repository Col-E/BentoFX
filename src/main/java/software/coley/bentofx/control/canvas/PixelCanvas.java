package software.coley.bentofx.control.canvas;

import jakarta.annotation.Nonnull;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;

/**
 * This is a very simple alternative to {@link Canvas} that <i>does not</i> keep track of draw operations.
 * In some super niche cases, the default canvas is a memory hog, where this operates on a flat ARGB {@code int[]}.
 *
 * @author Matt Coley
 * @see PixelPainter
 */
public class PixelCanvas extends Region {
	private static final int OP_FILL_RECT = 100;
	private static final int OP_FILL_IMG = 101;
	private static final int OP_DRAW_RECT = 200;
	private static final int OP_DRAW_LINE_H = 201;
	private static final int OP_DRAW_LINE_V = 202;
	private static final int OP_DRAW_PX = 203;
	/** Pixel painter. */
	private final PixelPainter<?> pixelPainter;
	/** Wrapped display. */
	private final ImageView view = new ImageView();
	/** The image to display. */
	private WritableImage image;
	/** Last committed draw hash. */
	private int lastDrawHash;
	/** Current draw hash. */
	private int currentDrawHash;

	/**
	 * New pixel canvas.
	 *
	 * @param pixelPainter
	 * 		Painter to draw pixels.
	 */
	public PixelCanvas(@Nonnull PixelPainter<?> pixelPainter) {
		this.pixelPainter = pixelPainter;
		getChildren().add(view);

		view.fitWidthProperty().bind(widthProperty());
		view.fitHeightProperty().bind(heightProperty());

		widthProperty().addListener((ob, old, cur) -> markDirty());
		heightProperty().addListener((ob, old, cur) -> markDirty());
	}

	/**
	 * New pixel canvas.
	 */
	public PixelCanvas() {
		this(new PixelPainterIntArgb());
	}

	/**
	 * New pixel canvas.
	 *
	 * @param pixelPainter
	 * 		Painter to draw pixels.
	 * @param width
	 * 		Assigned width.
	 * @param height
	 * 		Assigned height.
	 */
	public PixelCanvas(@Nonnull PixelPainter<?> pixelPainter, int width, int height) {
		this.pixelPainter = pixelPainter;
		getChildren().add(view);

		setMinSize(width, height);
		setMaxSize(width, height);
		setPrefSize(width, height);

		view.setFitWidth(width);
		view.setFitHeight(height);

		reallocate();
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
		this(new PixelPainterIntArgb(), width, height);
	}

	/**
	 * Commits any pending state in the canvas buffer to the display.
	 */
	public void commit() {
		if (lastDrawHash == currentDrawHash) return;
		lastDrawHash = currentDrawHash;

		checkDirty();

		pixelPainter.commit(image.getPixelWriter());
		view.setImage(image);
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
		updateDrawHash(hash(OP_FILL_RECT, x, y, width, height));
		pixelPainter.fillRect(x, y, width, height, color);
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
		updateDrawHash(hash(OP_DRAW_RECT, x, y, width, height, borderSize, color));
		pixelPainter.drawRect(x, y, width, height, borderSize, color);
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
		updateDrawHash(hash(OP_DRAW_LINE_H, x, y, lineLength, lineWidth, color));
		pixelPainter.drawHorizontalLine(x, y, lineLength, lineWidth, color);
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
		updateDrawHash(hash(OP_DRAW_LINE_V, x, y, lineLength, lineWidth, color));
		pixelPainter.drawVerticalLine(x, y, lineLength, lineWidth, color);
	}

	/**
	 * Draws an image at the given coordinates.
	 *
	 * @param x
	 * 		X coordinate.
	 * @param y
	 * 		Y coordinate.
	 * @param image
	 * 		Image to draw.
	 */
	public void drawImage(int x, int y, @Nonnull ArgbSource image) {
		updateDrawHash(hash(OP_FILL_IMG, x, y, image.hashCode()));
		pixelPainter.drawImage(x, y, image);
	}

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
	public void drawImage(int x, int y, int sx, int sy, int sw, int sh, @Nonnull ArgbSource image) {
		updateDrawHash(hash(OP_FILL_IMG, x, y, sx, sy, sw, sh, image.hashCode()));
		pixelPainter.drawImage(x, y, sx, sy, sw, sh, image);
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
		updateDrawHash(hash(OP_DRAW_PX, x, y, color));
		pixelPainter.setColor(x, y, color);
	}

	/**
	 * Clears the canvas buffer. Call {@link #commit()} to update the display.
	 */
	public void clear() {
		currentDrawHash = 0;
		checkDirty();
		pixelPainter.clear();
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
		currentDrawHash = ~lastDrawHash;
	}

	/**
	 * Check if {@code dirty} and allocates a new buffer when dirty.
	 */
	public void checkDirty() {
		if (isDirty())
			reallocate();
	}

	/**
	 * Allocate the image and associated values.
	 */
	protected void reallocate() {
		int imageWidth = (int) Math.max(1, view.getFitWidth());
		int imageHeight = (int) Math.max(1, view.getFitHeight());
		if (pixelPainter.initialize(imageWidth, imageHeight) || image == null)
			image = newImage(imageWidth, imageHeight);
	}

	/**
	 * Called by {@link #reallocate()} when a new image is necessary.
	 *
	 * @param width
	 * 		Image width.
	 * @param height
	 * 		Image height.
	 *
	 * @return New writable image of the given dimensions.
	 */
	@Nonnull
	protected WritableImage newImage(int width, int height) {
		return new WritableImage(width, height);
	}
}
