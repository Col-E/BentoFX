package software.coley.bentofx.persistence.impl.codec.json.provider;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.impl.codec.json.JsonLayoutCodec;

import static org.assertj.core.api.Assertions.assertThat;

class JsonLayoutCodecProviderTest {

	@Test
	void exposesJsonIdentifierAndIsDefault() {
		final JsonLayoutCodecProvider provider = new JsonLayoutCodecProvider();

		assertThat(provider.getIdentifier())
				.describedAs("provider.getIdentifier()")
				.isEqualTo(JsonLayoutCodec.CODEC_IDENTIFIER);
		assertThat(provider.isDefault())
				.describedAs("provider.isDefault()")
				.isTrue();
	}

	@Test
	void getLayoutCodecReturnsJsonLayoutCodec() {
		final JsonLayoutCodecProvider provider = new JsonLayoutCodecProvider();

		final LayoutCodec codec = provider.getLayoutCodec();

		assertThat(codec)
				.describedAs("codec")
				.isNotNull()
				.isInstanceOf(JsonLayoutCodec.class);
		assertThat(codec.getIdentifier())
				.describedAs("codec.getIdentifier()")
				.isEqualTo(JsonLayoutCodec.CODEC_IDENTIFIER);
	}
}
