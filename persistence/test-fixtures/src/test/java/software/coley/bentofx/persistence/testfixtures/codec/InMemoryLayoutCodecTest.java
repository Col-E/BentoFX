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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryLayoutCodecTest {

	private static final String BENTO_IDENTIFIER = "bento:test";
	private static final String DISPLAY_NAME = "Multi-Monitor";

	@Test
	void encodedLayoutCanBeDecodedBySameCodecInstance()
			throws BentoStateException {

		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final BentoState state = new BentoStateBuilder(BENTO_IDENTIFIER).build();
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		codec.encode(
				new PersistableLayout(DISPLAY_NAME, List.of(state)),
				outputStream
		);

		final PersistableLayout decoded = codec.decode(
				new ByteArrayInputStream(outputStream.toByteArray())
		);

		assertThat(decoded.bentoStates())
				.describedAs("decoded states")
				.containsExactly(state);
		assertThat(decoded.displayName())
				.describedAs("decoded display name")
				.isEqualTo(DISPLAY_NAME);
		assertThat(codec.getEncodedStates())
				.describedAs("codec.getEncodedStates()")
				.containsExactly(state);
		assertThat(codec.getEncodeCalls())
				.describedAs("codec.getEncodeCalls()")
				.containsExactly(List.of(state));
	}

	@Test
	void emptyInputDecodesToEmptyLayout() throws BentoStateException {
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();

		assertThat(codec.decode(new ByteArrayInputStream(new byte[0]))
				.bentoStates())
				.describedAs("states decoded from empty input")
				.isEmpty();
	}

	@Test
	void tokenFromDifferentCodecInstanceCannotBeDecoded() throws BentoStateException {
		final InMemoryLayoutCodec writer = new InMemoryLayoutCodec();
		final InMemoryLayoutCodec reader = new InMemoryLayoutCodec();
		final BentoState state = new BentoStateBuilder(BENTO_IDENTIFIER).build();
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		writer.encode(PersistableLayout.of(List.of(state)), outputStream);

		assertThatThrownBy(() -> reader.decode(new ByteArrayInputStream(outputStream.toByteArray())))
				.describedAs("decoding a token another codec instance wrote")
				.isInstanceOf(BentoStateException.class)
				.hasMessageContaining("not encoded by this InMemoryLayoutCodec");
	}
}
