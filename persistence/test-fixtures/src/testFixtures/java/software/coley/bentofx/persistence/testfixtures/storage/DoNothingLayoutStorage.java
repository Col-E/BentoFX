package software.coley.bentofx.persistence.testfixtures.storage;

import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Simple in-memory {@link LayoutStorage} for provider-selection tests.
 *
 * <p>It stores nothing. Discards bytes written to the provided stream,
 * {@link #exists()} always returns {@code false}, and reading from the
 * {@link InputStream} returns no bytes. This fixture is intended for use
 * with tests that only care which provider was chosen. It should not be
 * used for anything intended to that save and read back what was saved.
 *
 * @author Phil Bryant
 * @see InMemoryLayoutStorage to save and read back what was saved.
 */
public final class DoNothingLayoutStorage implements LayoutStorage {

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public OutputStream openOutputStream() {
		return new ByteArrayOutputStream();
	}

	@Override
	public InputStream openInputStream() {
		return new ByteArrayInputStream(new byte[0]);
	}
}
