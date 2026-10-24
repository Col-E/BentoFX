package software.coley.bentofx.persistence.core.impl;

import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reads persisted Bento layout state from configured storage.
 *
 * @author Phil Bryant
 */
final class LayoutStateReader implements AutoCloseable {

	private final LayoutCodec layoutCodec;
	private final LayoutStorage layoutStorage;
	private final AtomicBoolean closed = new AtomicBoolean();

	LayoutStateReader(
			final LayoutCodec layoutCodec,
			final LayoutStorage layoutStorage
	) {
		this.layoutCodec = Objects.requireNonNull(layoutCodec);
		this.layoutStorage = Objects.requireNonNull(layoutStorage);
	}

	/**
	 * Returns {@code true} if a persisted layout exists in storage; otherwise,
	 * returns {@code false}. Exposed here rather than letting callers hold the
	 * {@link LayoutStorage} themselves, so that the reader is the only owner of
	 * the instance it closes.
	 *
	 * @return {@code true} if a persisted layout exists in storage; otherwise,
	 * {@code false}.
	 */
	boolean layoutExists() {
		return layoutStorage.exists();
	}

	/**
	 * Reads and decodes persisted layout state. This method should run away
	 * from the JavaFX application thread because it performs storage and codec
	 * operations.
	 *
	 * @return decoded Bento states.
	 * @throws BentoStateException when storage or decoding fails.
	 */
	List<BentoState> readLayoutState() throws BentoStateException {
		try (final InputStream in = layoutStorage.openInputStream()) {

			return layoutCodec.decode(in).bentoStates();
		} catch (final IOException e) {

			throw new BentoStateException(
					"Could not read persisted layout state",
					e
			);
		}
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			layoutStorage.close();
		}
	}
}
