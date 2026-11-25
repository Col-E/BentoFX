package software.coley.bentofx.persistence.testfixtures.storage;

import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test storage that records the threads used to inspect and open persisted layout streams.
 *
 * <p>Each call to {@link #openOutputStream()} gets a buffer of its own, and closing
 * that stream is what stores its bytes. A save that fails part way through, or one
 * that abandons its stream, leaves what was stored before it alone.</p>
 *
 * @author Phil Bryant
 */
public final class ThreadRecordingLayoutStorage implements LayoutStorage {
	private final AtomicReference<@Nullable Thread> existsThread = new AtomicReference<>();
	private final AtomicReference<@Nullable Thread> openInputStreamThread = new AtomicReference<>();
	private final AtomicReference<@Nullable Thread> openOutputStreamThread = new AtomicReference<>();
	private final AtomicReference<byte[]> storedBytes = new AtomicReference<>(new byte[0]);

	@Override
	public boolean exists() {
		existsThread.set(Thread.currentThread());
		return storedBytes.get().length > 0;
	}

	@Override
	public OutputStream openOutputStream() {
		openOutputStreamThread.set(Thread.currentThread());

		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		return new FilterOutputStream(buffer) {
			@Override
			public void close() throws IOException {
				super.close();
				storedBytes.set(buffer.toByteArray());
			}
		};
	}

	@Override
	public InputStream openInputStream() {
		openInputStreamThread.set(Thread.currentThread());
		return new ByteArrayInputStream(storedBytes.get());
	}

	/**
	 * {@return the thread that last called {@link #exists()}, or {@code null} when
	 * it has not been called.}
	 */
	public @Nullable Thread getExistsThread() {
		return existsThread.get();
	}

	/**
	 * {@return the thread that last called {@link #openInputStream()}, or
	 * {@code null} when it has not been called.}
	 */
	public @Nullable Thread getOpenInputStreamThread() {
		return openInputStreamThread.get();
	}

	/**
	 * {@return the thread that last called {@link #openOutputStream()}, or
	 * {@code null} when it has not been called.}
	 */
	public @Nullable Thread getOpenOutputStreamThread() {
		return openOutputStreamThread.get();
	}

	/**
	 * {@return a copy of the stored bytes, which are the bytes of the last stream
	 * that was closed.}
	 */
	public byte[] toByteArray() {
		return storedBytes.get().clone();
	}
}
