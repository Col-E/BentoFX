package software.coley.bentofx.persistence.testfixtures.codec;

import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.codec.PersistableLayout;
import software.coley.bentofx.persistence.core.api.state.BentoState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test codec that records the threads used to encode and decode persisted state.
 *
 * <p>Encoding and decoding are inverses: what {@link #encode} is given is what
 * {@link #decode} returns. Use {@link #writeEncoded} instead when a test only
 * restores, so that there is something to decode without an encode having
 * happened.</p>
 *
 * <p>{@link #decode} does not read the {@link InputStream} provided to it. This codec
 * records threads; it is not the fixture to use when a test needs the bytes a
 * storage held to reach the codec.</p>
 *
 * @author Phil Bryant
 * @see InMemoryLayoutCodec for tests needing the bytes a storage holds in order to
 * reach a codec
 */
public final class ThreadRecordingLayoutCodec implements LayoutCodec {

	private final AtomicReference<@Nullable Thread> encodeThread = new AtomicReference<>();
	private final AtomicReference<@Nullable Thread> decodeThread = new AtomicReference<>();
	private List<BentoState> encodedStates = List.of();
	private List<BentoState> decodedStates = List.of();

	@Override
	public String getIdentifier() {
		return "thread-recording";
	}

	@Override
	public synchronized void encode(
			final PersistableLayout layout,
			final OutputStream outputStream
	) throws BentoStateException {
		encodeThread.set(Thread.currentThread());
		encodedStates = layout.bentoStates();

		// Also what decode returns. A codec whose halves are not inverses hands a
		// test that saves and then restores an empty layout and no error.
		decodedStates = encodedStates;

		writeMarker(outputStream);
	}

	@Override
	public synchronized PersistableLayout decode(
			final InputStream inputStream
	) throws BentoStateException {
		decodeThread.set(Thread.currentThread());
		return PersistableLayout.of(decodedStates);
	}

	/**
	 * Seeds the states returned by {@link #decode(InputStream)} and writes a marker
	 * to storage, for a test that restores without having encoded.
	 *
	 * @param bentoStates states to return when decoding.
	 * @param outputStream output stream to mark as written.
	 *
	 * @throws BentoStateException when writing the marker fails.
	 */
	public synchronized void writeEncoded(
			final List<BentoState> bentoStates,
			final OutputStream outputStream
	) throws BentoStateException {
		decodedStates = List.copyOf(bentoStates);
		writeMarker(outputStream);
	}

	/**
	 * {@return the thread that last called {@link #encode}, or {@code null} when it
	 * has not been called.}
	 */
	public @Nullable Thread getEncodeThread() {
		return encodeThread.get();
	}

	/**
	 * {@return the thread that last called {@link #decode}, or {@code null} when it
	 * has not been called.}
	 */
	public @Nullable Thread getDecodeThread() {
		return decodeThread.get();
	}

	/**
	 * {@return the states last handed to {@link #encode}, or an empty list when it
	 * has not been called.}
	 */
	public synchronized List<BentoState> getEncodedStates() {
		return encodedStates;
	}

	/**
	 * Writes the one byte that stands in for an encoded layout.
	 *
	 * @param outputStream the stream to write to.
	 *
	 * @throws BentoStateException when writing fails.
	 */
	private static void writeMarker(final OutputStream outputStream) throws BentoStateException {
		try {
			outputStream.write(1);
		} catch (final IOException e) {
			throw new BentoStateException("Could not write test codec marker.", e);
		}
	}
}
