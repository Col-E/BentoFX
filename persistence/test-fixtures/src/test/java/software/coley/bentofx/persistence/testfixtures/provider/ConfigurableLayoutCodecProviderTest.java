package software.coley.bentofx.persistence.testfixtures.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurableLayoutCodecProviderTest {

	private static final String JSON_CODEC_IDENTIFIER = "json";

	@Test
	void exposesIdentifierAndDefaultFlag() {
		final ConfigurableLayoutCodecProvider provider = new ConfigurableLayoutCodecProvider(JSON_CODEC_IDENTIFIER, true);

		assertThat(provider.getIdentifier())
				.describedAs("provider.getIdentifier()")
				.isEqualTo(JSON_CODEC_IDENTIFIER);
		assertThat(provider.isDefault())
				.describedAs("provider.isDefault()")
				.isTrue();
	}

	@Test
	void createsCodecWithMatchingIdentifierAndCountsCreations() {
		final ConfigurableLayoutCodecProvider provider = new ConfigurableLayoutCodecProvider(JSON_CODEC_IDENTIFIER, false);

		assertThat(provider.getLayoutCodec().getIdentifier())
				.describedAs("provider.getLayoutCodec().getIdentifier()")
				.isEqualTo(JSON_CODEC_IDENTIFIER);
		assertThat(provider.getCreatedCodecCount())
				.describedAs("provider.getCreatedCodecCount()")
				.isEqualTo(1);
	}
}
