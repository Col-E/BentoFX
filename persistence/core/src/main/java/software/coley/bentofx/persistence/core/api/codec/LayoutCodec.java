package software.coley.bentofx.persistence.core.api.codec;

import software.coley.bentofx.persistence.core.api.BentoStateException;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Codec for encoding and decoding a {@link PersistableLayout} to and from some
 * persistence format (XML, JSON, and so on).
 *
 * @author Phil Bryant
 */
public interface LayoutCodec {

	/**
	 * Returns an identifier used to differentiate this {@link LayoutCodec}
	 * implementation from other {@link LayoutCodec} implementations. Could be
	 * usable as a file extension.
	 * @return an identifier used to differentiate this {@link LayoutCodec}.
	 */
	String getIdentifier();

	/**
	 * Encode the {@link PersistableLayout} and write it to the
	 * {@link OutputStream}.
	 *
	 * <p>The layout's display name is part of what is written, so a codec that
	 * drops it does not round-trip: a layout decoded later would come back
	 * without the name it was saved under.</p>
	 *
	 * @param layout the {@link PersistableLayout} to be encoded.
	 * @param outputStream the {@link OutputStream} where the encoded
	 * {@link PersistableLayout} is to be written.
	 * @throws BentoStateException on error
	 */
	void encode(
			final PersistableLayout layout,
			final OutputStream outputStream
	) throws BentoStateException;

	/**
	 * Read the {@link PersistableLayout} from an {@link InputStream}.
	 *
	 * @param inputStream stream a {@link PersistableLayout} was written to.
	 * @return the decoded {@link PersistableLayout}.
	 * @throws BentoStateException on error
	 */
	PersistableLayout decode(
			final InputStream inputStream
	) throws BentoStateException;
}
