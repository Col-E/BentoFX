package software.coley.bentofx.persistence.testfixtures.codec;

import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.codec.PersistableLayout;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Simple {@link LayoutCodec} implementation for provider-selection tests.
 *
 * <p>It encodes nothing and decodes to nothing, which is enough for a test that
 * only cares which codec was chosen</p>
 *
 * @param identifier the identifier this codec answers to.
 *
 * @author Phil Bryant
 * @see InMemoryLayoutCodec} for encoding and decoding what was encoded
 */
public record DoNothingLayoutCodec(String identifier) implements LayoutCodec {

	@Override
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Discards the supplied layout, writing nothing.
	 *
	 * @param layout ignored.
	 * @param outputStream ignored, and left unwritten.
	 */
	@Override
	public void encode(
			final PersistableLayout layout,
			final OutputStream outputStream
	) {
		// no-op
	}

	/**
	 * {@return an empty layout, whatever the stream holds.}
	 *
	 * @param inputStream ignored, and left unread.
	 */
	@Override
	public PersistableLayout decode(
			final InputStream inputStream
	) {
		return PersistableLayout.of(List.of());
	}
}
