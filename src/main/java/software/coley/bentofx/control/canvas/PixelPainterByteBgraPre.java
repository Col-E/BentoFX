package software.coley.bentofx.control.canvas;

import jakarta.annotation.Nonnull;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;

import java.nio.ByteBuffer;

/**
 * Pixel painter instance backed by {@link PixelFormat#getByteBgraPreInstance()}.
 *
 * @author Matt Coley
 */
public class PixelPainterByteBgraPre extends PixelPainterByteBgra {
	@Override
	public void fillRect(int x, int y, int width, int height, int color) {
		int alpha = (color >>> 24);
		int red, green, blue;
		if (alpha > 0x00) {
			red = (color >> 16) & 0xFF;
			green = (color >> 8) & 0xFF;
			blue = (color) & 0xFF;
			if (alpha < 0xFF) {
				red = (red * alpha + 127) / 0xFF;
				green = (green * alpha + 127) / 0xFF;
				blue = (blue * alpha + 127) / 0xFF;
			}
		} else {
			red = green = blue = 0;
		}
		int yBound = Math.min(y + height, imageHeight);
		int xBound = Math.min(x + width, imageWidth);
		ByteBuffer drawBuffer = this.drawBuffer;
		int capacity = drawBufferCapacity();
		for (int ly = y; ly < yBound; ly++) {
			int yOffset = ly * imageWidth;
			for (int lx = x; lx < xBound; lx++) {
				int index = (yOffset + lx) * DATA_SIZE;
				if (index < capacity) {
					drawBuffer.put(index, (byte) blue);
					drawBuffer.put(index + 1, (byte) green);
					drawBuffer.put(index + 2, (byte) red);
					drawBuffer.put(index + 3, (byte) alpha);
				}
			}
		}
	}

	@Override
	public void setColor(int x, int y, int color) {
		int i = ((y * imageWidth) + x) * DATA_SIZE;
		if (i >= 0 && i < drawBufferCapacity()) {
			int alpha = (color >>> 24);
			int red, green, blue;
			if (alpha > 0x00) {
				red = (color >> 16) & 0xFF;
				green = (color >> 8) & 0xFF;
				blue = (color) & 0xFF;
				if (alpha < 0xFF) {
					red = (red * alpha + 127) / 0xFF;
					green = (green * alpha + 127) / 0xFF;
					blue = (blue * alpha + 127) / 0xFF;
				}
			} else {
				red = green = blue = 0;
			}
			drawBuffer.put(i, (byte) blue);
			drawBuffer.put(i + 1, (byte) green);
			drawBuffer.put(i + 2, (byte) red);
			drawBuffer.put(i + 3, (byte) alpha);
		}
	}

	@Nonnull
	@Override
	public PixelFormat<ByteBuffer> getPixelFormat() {
		return PixelFormat.getByteBgraPreInstance();
	}
}
