package software.coley.bentofx.persistence.testfixtures.storage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadRecordingLayoutStorageTest {

	@Test
	void existsRecordsCallingThread() {
		final Thread currentThread = Thread.currentThread();

		try (final ThreadRecordingLayoutStorage storage =
					 new ThreadRecordingLayoutStorage()) {

			assertThat(storage.exists())
					.describedAs("storage.exists()")
					.isFalse();

			assertThat(storage.getExistsThread())
					.describedAs("storage.getExistsThread()")
					.isSameAs(currentThread);
		}
	}

	@Test
	void openOutputStreamRecordsCallingThreadAndStoresBytes() throws IOException {
		final Thread currentThread = Thread.currentThread();

		try (final ThreadRecordingLayoutStorage storage =
					 new ThreadRecordingLayoutStorage()) {

			try (final OutputStream outputStream = storage.openOutputStream()) {
				outputStream.write(new byte[]{1, 2, 3});
			}

			assertThat(storage.getOpenOutputStreamThread())
					.describedAs("storage.getOpenOutputStreamThread()")
					.isSameAs(currentThread);
			assertThat(storage.toByteArray())
					.describedAs("storage.toByteArray()")
					.containsExactly(1, 2, 3);
		}
	}

	@Test
	void previouslyStoredBytesSurviveAnAbandonedStream() throws IOException {
		try (final ThreadRecordingLayoutStorage storage =
					 new ThreadRecordingLayoutStorage()) {

			try (final OutputStream outputStream = storage.openOutputStream()) {
				outputStream.write(new byte[]{1, 2, 3});
			}

			// A save that dies part way through: the stream is opened, written to,
			// and never closed. Closing is what stores, so the previous bytes
			// stand. Deliberately not a try-with-resources - that is the point of
			// the test.
			storage.openOutputStream().write(new byte[]{9});

			assertThat(storage.toByteArray())
					.describedAs("storage.toByteArray() after an abandoned save")
					.containsExactly(1, 2, 3);
			assertThat(storage.exists())
					.describedAs("storage.exists() after an abandoned save")
					.isTrue();
		}
	}

	@Test
	void openInputStreamRecordsCallingThreadAndReadsStoredBytes() throws IOException {
		final Thread currentThread = Thread.currentThread();

		try (final ThreadRecordingLayoutStorage storage =
					 new ThreadRecordingLayoutStorage()) {

			try (final OutputStream outputStream = storage.openOutputStream()) {
				outputStream.write(new byte[]{1, 2, 3});
			}

			try (final InputStream inputStream = storage.openInputStream()) {
				assertThat(inputStream.readAllBytes())
						.describedAs("storage.openInputStream().readAllBytes()")
						.containsExactly(1, 2, 3);
			}

			assertThat(storage.getOpenInputStreamThread())
					.describedAs("storage.getOpenInputStreamThread()")
					.isSameAs(currentThread);
		}
	}
}
