package software.coley.bentofx.control.canvas;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage for {@link ArgbBufferedImageSource#equals(Object)} and its
 * agreement with {@link ArgbBufferedImageSource#hashCode()}.
 *
 * @author Phil Bryant
 */
class ArgbBufferedImageSourceTest {

    private static final int OPAQUE_RED = 0xFFFF0000;
    private static final int OPAQUE_BLUE = 0xFF0000FF;

    /**
     * Red without the override: {@link Object#equals(Object)} compares
     * identity, so two wrappers of identical pixels are unequal.
     */
    @Test
    void equalsIsTrueForDistinctSourcesWithTheSamePixels() {
        final ArgbBufferedImageSource subject = newSource(2, 2, OPAQUE_RED);
        final ArgbBufferedImageSource other = newSource(2, 2, OPAQUE_RED);

        assertThat(subject)
                .describedAs("equals() for two sources wrapping identical pixels")
                .isEqualTo(other);
    }

    @Test
    void hashCodeAgreesForSourcesThatAreEqual() {
        final ArgbBufferedImageSource subject = newSource(2, 2, OPAQUE_RED);
        final ArgbBufferedImageSource other = newSource(2, 2, OPAQUE_RED);

        assertThat(subject.hashCode())
                .describedAs("hashCode() for two equal sources")
                .hasSameHashCodeAs(other);
    }

    @Test
    void equalsIsFalseWhenAPixelDiffers() {
        final ArgbBufferedImageSource subject = newSource(2, 2, OPAQUE_RED);
        final BufferedImage otherImage = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        fill(otherImage, OPAQUE_RED);
        otherImage.setRGB(1, 1, OPAQUE_BLUE);

        assertThat(subject)
                .describedAs("equals() for sources differing in one pixel")
                .isNotEqualTo(new ArgbBufferedImageSource(otherImage));
    }

    /**
     * Red when only the pixel arrays are compared: a 2x3 and a 3x2 image of one
     * color produce identical six-element arrays.
     */
    @Test
    void equalsIsFalseForTheSamePixelsInDifferentDimensions() {
        final ArgbBufferedImageSource subject = newSource(2, 3, OPAQUE_RED);
        final ArgbBufferedImageSource other = newSource(3, 2, OPAQUE_RED);

        assertThat(subject)
                .describedAs("equals() for 2x3 and 3x2 sources of the same color")
                .isNotEqualTo(other);
    }

    /**
     * Red when only the pixel array is hashed: a 2x3 and a 3x2 image of one
     * color hash identically. Distinct hashes are not required by the
     * contract, but these two are known to differ once the dimensions are
     * included.
     */
    @Test
    void hashCodeDiffersForTheSamePixelsInDifferentDimensions() {
        final ArgbBufferedImageSource subject = newSource(2, 3, OPAQUE_RED);
        final ArgbBufferedImageSource other = newSource(3, 2, OPAQUE_RED);

        assertThat(subject.hashCode())
                .describedAs("hashCode() for 2x3 and 3x2 sources of the same color")
                .isNotEqualTo(other.hashCode());
    }

    @Test
    void hashCodeIsStableAcrossCalls() {
        final ArgbBufferedImageSource subject = newSource(2, 2, OPAQUE_RED);

        assertThat(subject.hashCode())
                .describedAs("second hashCode() call")
                .hasSameHashCodeAs(subject);
    }

    /**
     * Red when the cache is written but never read: each call rebuilds the
     * array, so equals() and hashCode() reread every pixel.
     */
    @Test
    void getArgbReturnsTheCachedArrayOnLaterCalls() {
        final ArgbBufferedImageSource subject = newSource(2, 2, OPAQUE_RED);

        assertThat(subject.getArgb())
                .describedAs("second getArgb() call")
                .isSameAs(subject.getArgb());
    }

    /**
     * Red when every call rereads the image: the second call would see the
     * pixel changed after the first.
     */
    @Test
    void getArgbDoesNotRereadTheImageAfterTheFirstCall() {
        final BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        fill(image, OPAQUE_RED);
        final ArgbBufferedImageSource subject = new ArgbBufferedImageSource(image);
        subject.getArgb();

        image.setRGB(0, 0, OPAQUE_BLUE);

        assertThat(subject.getArgb())
                .describedAs("getArgb() after the wrapped image changed")
                .containsOnly(OPAQUE_RED);
    }

    /**
     * Guards the cache against returning the wrong pixels: green before and
     * after the change.
     */
    @Test
    void getArgbReturnsThePixelsInRowMajorOrder() {
        final BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        fill(image, OPAQUE_RED);
        image.setRGB(1, 0, OPAQUE_BLUE);
        final ArgbBufferedImageSource subject = new ArgbBufferedImageSource(image);

        assertThat(subject.getArgb())
                .describedAs("getArgb() for a 2x2 image with a blue pixel at (1, 0)")
                .containsExactly(OPAQUE_RED, OPAQUE_BLUE, OPAQUE_RED, OPAQUE_RED);
    }

    @Test
    void equalsIsTrueForItself() {
        final ArgbBufferedImageSource subject = newSource(2, 2, OPAQUE_RED);

        assertThat(subject.equals(subject))
                .describedAs("equals() for a source compared with itself")
                .isTrue();
    }

    @Test
    void equalsIsFalseForNull() {
        final ArgbBufferedImageSource subject = newSource(2, 2, OPAQUE_RED);

        assertThat(subject.equals(null))
                .describedAs("equals(null)")
                .isFalse();
    }

    @Test
    void equalsIsFalseForAnotherArgbSourceType() {
        final ArgbBufferedImageSource subject = newSource(1, 1, OPAQUE_RED);
        final ArgbSource other = new ArgbSource() {
            @Override
            public int getWidth() {
                return 1;
            }

            @Override
            public int getHeight() {
                return 1;
            }

            @Override
            public int getArgb(final int x, final int y) {
                return OPAQUE_RED;
            }

            @Override
            public int[] getArgb(final int x, final int y, final int width, final int height) {
                return new int[]{OPAQUE_RED};
            }
        };

        assertThat(subject.equals(other))
                .describedAs("equals() against a different ArgbSource with the same pixels")
                .isFalse();
    }

    private static ArgbBufferedImageSource newSource(final int width, final int height, final int argb) {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        fill(image, argb);
        return new ArgbBufferedImageSource(image);
    }

    private static void fill(final BufferedImage image, final int argb) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, argb);
            }
        }
    }
}
