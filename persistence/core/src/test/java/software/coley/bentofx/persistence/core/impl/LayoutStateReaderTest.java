package software.coley.bentofx.persistence.core.impl;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.codec.PersistableLayout;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.BentoState.BentoStateBuilder;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LayoutStateReaderTest {

	private static final String CODEC_IDENTIFIER = "test";
	private static final String ENCODED_LAYOUT = "encoded-layout";
	private static final String LAYOUT_IDENTIFIER = "main";
	private static final String CODEC_FAILURE_MESSAGE = "codec failed";
	private static final String STORAGE_FAILURE_MESSAGE = "storage failed";
	private static final String READ_FAILURE_MESSAGE = "Could not read persisted layout state";
	private static final String READ_EXCEPTION_DESCRIPTION =
			"exception thrown by LayoutStateReader.readLayoutState()";

	@Test
	void readsDecodedStateFromStorage() throws BentoStateException {
		final BentoState bentoState = new BentoStateBuilder(LAYOUT_IDENTIFIER)
				.build();
		final RecordingLayoutCodec codec = new RecordingLayoutCodec(List.of(bentoState));
		try (final InMemoryLayoutStorage storage = new InMemoryLayoutStorage(ENCODED_LAYOUT)) {
			final List<BentoState> states = new LayoutStateReader(codec, storage)
					.readLayoutState();

			assertThat(states)
					.describedAs("states")
					.containsExactly(bentoState);
			assertThat(codec.getDecodedInputStreamContent())
					.describedAs("codec.getDecodedInputStreamContent()")
					.isEqualTo(ENCODED_LAYOUT);
		}
	}

	@Test
	void propagatesDecodingFailures() {
		final RecordingLayoutCodec codec = new RecordingLayoutCodec(List.of());
		final BentoStateException expectedCause = new BentoStateException(CODEC_FAILURE_MESSAGE);
		codec.setDecodeException(expectedCause);

		try (final InMemoryLayoutStorage storage = new InMemoryLayoutStorage(ENCODED_LAYOUT)) {
			assertThatThrownBy(() ->
					new LayoutStateReader(codec, storage).readLayoutState()
			)
					.describedAs(READ_EXCEPTION_DESCRIPTION)
					.isSameAs(expectedCause);
		}
	}

	@Test
	void wrapsStorageFailures() {
		final RecordingLayoutCodec codec = new RecordingLayoutCodec(List.of());
		final IOException expectedCause = new IOException(STORAGE_FAILURE_MESSAGE);

		try (final InMemoryLayoutStorage storage = new InMemoryLayoutStorage(ENCODED_LAYOUT)) {
			storage.setOpenInputStreamException(expectedCause);

			assertThatThrownBy(() ->
					new LayoutStateReader(codec, storage).readLayoutState()
			)
					.describedAs(READ_EXCEPTION_DESCRIPTION + " when storage fails")
					.isInstanceOf(BentoStateException.class)
					.hasMessage(READ_FAILURE_MESSAGE)
					.hasCause(expectedCause);
		}
	}


	@Test
	void closeClosesStorageOnce() {
		final RecordingLayoutCodec codec = new RecordingLayoutCodec(List.of());
		final InMemoryLayoutStorage storage = new InMemoryLayoutStorage(ENCODED_LAYOUT);
		final LayoutStateReader reader = new LayoutStateReader(codec, storage);

		reader.close();
		reader.close();

		assertThat(storage.getCloseCount())
				.describedAs("storage.getCloseCount()")
				.isEqualTo(1);
	}

	private static final class RecordingLayoutCodec implements LayoutCodec {

		private final List<BentoState> decodedBentoStates;
		private @Nullable String decodedInputStreamContent;
		private @Nullable BentoStateException decodeException;

		RecordingLayoutCodec(final List<BentoState> decodedBentoStates) {
			this.decodedBentoStates = List.copyOf(decodedBentoStates);
		}

		@Override
		public String getIdentifier() {
			return CODEC_IDENTIFIER;
		}

		@Override
		public void encode(
				final PersistableLayout layout,
				final OutputStream outputStream
		) {
			// no-op
		}

		@Override
		public PersistableLayout decode(
				final InputStream inputStream
		) throws BentoStateException {
			if (decodeException != null) {
				throw decodeException;
			}

			try {
				decodedInputStreamContent = new String(
						inputStream.readAllBytes(),
						StandardCharsets.UTF_8
				);
			} catch (final IOException e) {
				throw new BentoStateException("Could not read test input", e);
			}
			return PersistableLayout.of(decodedBentoStates);
		}

		@Nullable
		String getDecodedInputStreamContent() {
			return decodedInputStreamContent;
		}

		void setDecodeException(final @Nullable BentoStateException decodeException) {
			this.decodeException = decodeException;
		}
	}

	private static final class InMemoryLayoutStorage implements LayoutStorage {

		private final String inputStreamContent;
		private @Nullable IOException openInputStreamException;
		private int closeCount;

		InMemoryLayoutStorage(final String inputStreamContent) {
			this.inputStreamContent = inputStreamContent;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public OutputStream openOutputStream() {
			return new ByteArrayOutputStream();
		}

		@Override
		public InputStream openInputStream() throws IOException {
			if (openInputStreamException != null) {
				throw openInputStreamException;
			}
			return new ByteArrayInputStream(
					inputStreamContent.getBytes(StandardCharsets.UTF_8)
			);
		}

		@Override
		public void close() {
			closeCount++;
		}

		int getCloseCount() {
			return closeCount;
		}

		void setOpenInputStreamException(
				final @Nullable IOException openInputStreamException
		) {
			this.openInputStreamException = openInputStreamException;
		}
	}
}
