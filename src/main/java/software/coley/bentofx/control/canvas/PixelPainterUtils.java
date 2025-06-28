package software.coley.bentofx.control.canvas;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Common utils for {@link PixelPainter} implementations.
 *
 * @author Matt Coley
 */
public class PixelPainterUtils {
	public static final int[] EMPTY_ARRAY_I = new int[0];
	public static final byte[] EMPTY_ARRAY_B = new byte[0];
	public static final IntBuffer EMPTY_BUFFER_I = IntBuffer.wrap(EMPTY_ARRAY_I);
	public static final ByteBuffer EMPTY_BUFFER_B = ByteBuffer.wrap(EMPTY_ARRAY_B);
}
