package software.coley.bentofx.control.canvas;

import jakarta.annotation.Nonnull;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;

import java.nio.IntBuffer;

/**
 * Pixel painter instance backed by {@link PixelFormat#getIntArgbPreInstance()}.
 *
 * @author Matt Coley
 */
public class PixelPainterIntArgbPre extends PixelPainterIntArgb {
	@Override
	public void fillRect(int x, int y, int width, int height, int color) {
		super.fillRect(x, y, width, height, argbToArgbPre(color));
	}

	@Override
	public void setColor(int x, int y, int color) {
		super.setColor(x, y, argbToArgbPre(color));
	}

	@Nonnull
	@Override
	public PixelFormat<IntBuffer> getPixelFormat() {
		return PixelFormat.getIntArgbPreInstance();
	}

	protected static int argbToArgbPre(int color) {
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
		return ((alpha & 0xFF) << 24) |
				((red & 0xFF) << 16) |
				((green & 0xFF) << 8) |
				((blue & 0xFF));
	}
}
