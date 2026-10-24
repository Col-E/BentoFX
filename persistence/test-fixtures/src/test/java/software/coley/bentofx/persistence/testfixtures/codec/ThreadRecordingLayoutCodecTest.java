package software.coley.bentofx.persistence.testfixtures.codec;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.codec.PersistableLayout;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.BentoState.BentoStateBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadRecordingLayoutCodecTest {

	private static final String BENTO_IDENTIFIER = "bento:test";

	@Test
	void encodeRecordsThreadAndStates() throws BentoStateException {
		final ThreadRecordingLayoutCodec codec = new ThreadRecordingLayoutCodec();
		final BentoState state = new BentoStateBuilder(BENTO_IDENTIFIER).build();
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		final Thread currentThread = Thread.currentThread();

		codec.encode(PersistableLayout.of(List.of(state)), outputStream);

		assertThat(codec.getEncodeThread())
				.describedAs("codec.getEncodeThread()")
				.isSameAs(currentThread);
		assertThat(codec.getEncodedStates())
				.describedAs("codec.getEncodedStates()")
				.containsExactly(state);
		assertThat(outputStream.toByteArray())
				.describedAs("outputStream.toByteArray()")
				.isNotEmpty();
	}

	@Test
	void encodedStatesCanBeDecodedBack() throws BentoStateException {
		final ThreadRecordingLayoutCodec codec = new ThreadRecordingLayoutCodec();
		final BentoState state = new BentoStateBuilder(BENTO_IDENTIFIER).build();
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		codec.encode(PersistableLayout.of(List.of(state)), outputStream);

		assertThat(codec.decode(
				new ByteArrayInputStream(outputStream.toByteArray()))
				.bentoStates())
				.describedAs("states decoded after encoding them")
				.containsExactly(state);
	}

	@Test
	void decodeRecordsThreadAndReturnsSeededStates() throws BentoStateException {
		final ThreadRecordingLayoutCodec codec = new ThreadRecordingLayoutCodec();
		final BentoState state = new BentoStateBuilder(BENTO_IDENTIFIER).build();
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		final Thread currentThread = Thread.currentThread();

		codec.writeEncoded(List.of(state), outputStream);

		assertThat(codec.decode(
				new ByteArrayInputStream(outputStream.toByteArray()))
				.bentoStates())
				.describedAs("states decoded from the seeded stream")
				.containsExactly(state);
		assertThat(codec.getDecodeThread())
				.describedAs("codec.getDecodeThread()")
				.isSameAs(currentThread);
	}
}
