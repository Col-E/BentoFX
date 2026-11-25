package software.coley.bentofx.persistence.impl.storage.file;

import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Implementation of the {@link LayoutStorage} interface for persisting Bento
 * layouts to a file.
 *
 * @author Phil Bryant
 */
public class FileLayoutStorage implements LayoutStorage {

	private static final String PARTIAL_FILE_SUFFIX = ".part";

	private final File file;

	/**
	 * Creates a {@code FileLayoutStorage} that can be used to persist Bento
	 * layout to a file.
	 * @param file the {@link File} to which the Bento is to be persisted.
	 */
	public FileLayoutStorage(final File file) {
		this.file = Objects.requireNonNull(file);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>An empty file and a directory are both answered {@code false}.</p>
	 */
	@Override
	public boolean exists() {
		return file.isFile() && file.length() > 0;
	}

	/**
	 * {@return a stream that writes to a file beside the target and moves it
	 * over the target when it closes cleanly.}
	 *
	 * <p>Writing straight to the target emptied it as soon as the stream
	 * opened, so a save that failed part way through left the previous layout
	 * gone and a fragment of the new one in its place.</p>
	 *
	 * @throws IOException if the directory or the file cannot be created.
	 */
	@Override
	public OutputStream openOutputStream() throws IOException {
		final Path target = file.toPath().toAbsolutePath();
		final Path directory = target.getParent();

		Files.createDirectories(directory);

		final Path partial = Files.createTempFile(
				directory,
				target.getFileName().toString(),
				PARTIAL_FILE_SUFFIX
		);

		return new StagedOutputStream(
				Files.newOutputStream(partial),
				partial,
				target
		);
	}

	@Override
	public InputStream openInputStream() throws IOException {
		return Files.newInputStream(file.toPath());
	}

	/**
	 * Writes to one file and, if every write and the close succeed, moves it
	 * over another.
	 *
	 * <p>A write or a close that fails leaves the target alone and deletes what
	 * had been written, so the caller's failure cannot promote a half-written
	 * layout. Closing is what promotes, and a caller that abandons the stream
	 * without closing it leaves the target as it was.</p>
	 */
	private static final class StagedOutputStream extends FilterOutputStream {

		private final Path partial;
		private final Path target;
		private boolean failed;
		private boolean closed;

		private StagedOutputStream(
				final OutputStream out,
				final Path partial,
				final Path target
		) {
			super(out);
			this.partial = partial;
			this.target = target;
		}

		@Override
		public void write(final int byteValue) throws IOException {
			try {
				out.write(byteValue);
			} catch (final IOException e) {
				failed = true;
				throw e;
			}
		}

		@Override
		public void write(
				final byte[] bytes,
				final int offset,
				final int length
		) throws IOException {
			try {
				out.write(bytes, offset, length);
			} catch (final IOException e) {
				failed = true;
				throw e;
			}
		}

		@Override
		public void flush() throws IOException {
			try {
				out.flush();
			} catch (final IOException e) {
				failed = true;
				throw e;
			}
		}

		@Override
		public void close() throws IOException {
			if (closed) {
				return;
			}

			closed = true;

			try {
				super.close();
			} catch (final IOException e) {
				Files.deleteIfExists(partial);
				throw e;
			}

			if (failed) {
				Files.deleteIfExists(partial);
				return;
			}

			try {
				move();
			} catch (final IOException e) {
				Files.deleteIfExists(partial);
				throw e;
			}
		}

		private void move() throws IOException {
			try {
				Files.move(partial, target, REPLACE_EXISTING, ATOMIC_MOVE);
			} catch (final AtomicMoveNotSupportedException e) {
				Files.move(partial, target, REPLACE_EXISTING);
			}
		}
	}
}
