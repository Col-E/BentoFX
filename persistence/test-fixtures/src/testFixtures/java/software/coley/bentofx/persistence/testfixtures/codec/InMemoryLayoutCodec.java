package software.coley.bentofx.persistence.testfixtures.codec;

import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.codec.PersistableLayout;
import software.coley.bentofx.persistence.core.api.state.BentoState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link LayoutCodec} implementation that encodes a layout to a small in-memory
 * token. Intended for tests that need a concrete {@link LayoutCodec} without
 * requiring JSON, XML, or another real persistence format.
 * <p>
 * The encoded bytes are meaningful to the codec instance that wrote them: the
 * token is a key into a map this codec holds, so decoding returns the very
 * layout that was encoded, display name and all. Use the same instance when
 * testing a save/restore round trip with an in-memory storage fixture.
 *
 * @author Phil Bryant
 */
public class InMemoryLayoutCodec implements LayoutCodec {

	private static final String IDENTIFIER = "memory";
	private static final String TOKEN_PREFIX = "bento-layout-";

	private final Map<String, PersistableLayout> encodedLayouts =
			new HashMap<>();
	private final List<PersistableLayout> encodeCalls = new ArrayList<>();
	private int nextToken = 0;

	@Override
	public String getIdentifier() {
		return IDENTIFIER;
	}

	@Override
	public synchronized void encode(
			final PersistableLayout layout,
			final OutputStream outputStream
	) throws BentoStateException {
		final String token = TOKEN_PREFIX + nextToken++;

		encodedLayouts.put(token, layout);
		encodeCalls.add(layout);

		try {
			outputStream.write(token.getBytes(StandardCharsets.UTF_8));
		} catch (final IOException e) {
			throw new BentoStateException(
					"Could not write encoded layout to the output stream",
					e
			);
		}
	}

	@Override
	public synchronized PersistableLayout decode(
			final InputStream inputStream
	) throws BentoStateException {
		final String token;

		try {
			token = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (final IOException e) {
			throw new BentoStateException(
					"Could not read encoded layout from the input stream",
					e
			);
		}

		if (token.isEmpty()) {
			return PersistableLayout.of(List.of());
		}

		final PersistableLayout layout = encodedLayouts.get(token);
		if (layout == null) {
			throw new BentoStateException(
					"Input stream was not encoded by this InMemoryLayoutCodec"
			);
		}

		return layout;
	}

	/**
	 * Seeds the codec with a layout and writes the matching token to the
	 * supplied output stream, so that a later {@link #decode(InputStream)} of
	 * those bytes returns it. Useful when testing restore-only code.
	 *
	 * @param layout the layout later decodes return for the written bytes.
	 * @param outputStream output stream to receive the token.
	 * @throws BentoStateException on error.
	 */
	public void writeEncoded(
			final PersistableLayout layout,
			final OutputStream outputStream
	) throws BentoStateException {
		encode(layout, outputStream);
	}

	/**
	 * Seeds the codec with states and writes the matching token, for a test
	 * that does not care about a display name.
	 *
	 * @param bentoStates states later decodes return for the written bytes.
	 * @param outputStream output stream to receive the token.
	 * @throws BentoStateException on error.
	 */
	public void writeEncoded(
			final List<BentoState> bentoStates,
			final OutputStream outputStream
	) throws BentoStateException {
		encode(PersistableLayout.of(bentoStates), outputStream);
	}

	/**
	 * @return states from the most recent encode call, or an empty list if this
	 * codec has not encoded anything.
	 */
	public synchronized List<BentoState> getEncodedStates() {
		if (encodeCalls.isEmpty()) {
			return List.of();
		}

		return encodeCalls.getLast().bentoStates();
	}

	/**
	 * @return the states handed to every encode call, in call order.
	 */
	public synchronized List<List<BentoState>> getEncodeCalls() {
		return encodeCalls.stream()
				.map(PersistableLayout::bentoStates)
				.toList();
	}

	/**
	 * @return the layouts handed to every encode call, in call order, for a
	 * test that also cares about the display name.
	 */
	public synchronized List<PersistableLayout> getEncodedLayouts() {
		return List.copyOf(encodeCalls);
	}
}
