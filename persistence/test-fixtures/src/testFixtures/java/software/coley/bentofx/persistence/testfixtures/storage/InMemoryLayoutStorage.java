package software.coley.bentofx.persistence.testfixtures.storage;

import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * {@link LayoutStorage} implementation that persists encoded layout bytes in
 * memory. Intended for use with tests that need a concrete
 * {@link LayoutStorage} without touching the file system or a database.
 *
 *
 * <p>Closing an output stream is what stores what was written to it, so an
 * abandoned save leaves the previously stored bytes alone. Every method holds this
 * instance's monitor, including the store the returned stream performs when it
 * closes, so a test may drive it from more than one thread.</p>
 *
 * @author Phil Bryant
 */
public class InMemoryLayoutStorage implements LayoutStorage {

	private byte[] bytes;

	/**
	 * Creates an empty storage location that does not yet exist.
	 */
	public InMemoryLayoutStorage() {
		this(new byte[0]);
	}

	/**
	 * Creates a storage location initialized with the supplied bytes.
	 *
	 * @param bytes initial stored bytes. Empty bytes leave this storage reporting
	 * that no layout exists.
	 */
	public InMemoryLayoutStorage(final byte[] bytes) {
		this.bytes = Arrays.copyOf(bytes, bytes.length);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Existence is answered considering a layout only exists once bytes have been
	 * stored for it, and empty content is no layout. There is deliberately no way to
	 * make this storage report that an empty layout exists.</p>
	 */
	@Override
	public synchronized boolean exists() {
		return bytes.length > 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The returned stream buffers what is written to it and stores that when it
	 * is closed.</p>
	 */
	@Override
	public OutputStream openOutputStream() {
		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		return new FilterOutputStream(buffer) {
			@Override
			public void close() throws IOException {
				super.close();
				storeBytes(buffer.toByteArray());
			}
		};
	}

	@Override
	public synchronized InputStream openInputStream() {
		return new ByteArrayInputStream(bytes);
	}

	/**
	 * Replaces the current stored bytes.
	 *
	 * @param bytes stored bytes.
	 */
	public void write(final byte[] bytes) {
		storeBytes(bytes);
	}

	/**
	 * Clears the stored bytes, after which this storage reports that no layout
	 * exists.
	 */
	public void delete() {
		storeBytes(new byte[0]);
	}

	/**
	 * {@return a defensive copy of the currently stored bytes.}
	 */
	public synchronized byte[] toByteArray() {
		return Arrays.copyOf(bytes, bytes.length);
	}

	/**
	 * Stores a copy of the supplied bytes.
	 *
	 * <p>Named for what it does rather than {@code write}, because the stream
	 * returned by {@link #openOutputStream()} calls this from its own
	 * {@code close()} and inherits a {@code write(byte[])} of its own - an
	 * unqualified call to that name inside the stream would write the bytes back
	 * into the buffer instead of storing them.</p>
	 *
	 * @param newBytes the bytes to store.
	 */
	private synchronized void storeBytes(final byte[] newBytes) {
		bytes = Arrays.copyOf(newBytes, newBytes.length);
	}
}
