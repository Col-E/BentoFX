package software.coley.bentofx.persistence.testfixtures.provider;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.testfixtures.storage.DoNothingLayoutStorage;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurableLayoutStorageProviderTest {

	private static final String FILE_STORAGE_IDENTIFIER = "file";
	private static final String JSON_CODEC_IDENTIFIER = "json";
	private static final String LAYOUT_IDENTIFIER = "layout";

	@Test
	void exposesIdentifierAndDefaultFlag() {
		final ConfigurableLayoutStorageProvider provider = new ConfigurableLayoutStorageProvider(FILE_STORAGE_IDENTIFIER, true);

		assertThat(provider.getIdentifier())
				.describedAs("provider.getIdentifier()")
				.isEqualTo(FILE_STORAGE_IDENTIFIER);
		assertThat(provider.isDefault())
				.describedAs("provider.isDefault()")
				.isTrue();
	}

	@Test
	void createsStorageAndRecordsIdentifiers() {
		final ConfigurableLayoutStorageProvider provider = new ConfigurableLayoutStorageProvider(FILE_STORAGE_IDENTIFIER, false);

		assertThat(provider.getLayoutStorage(LAYOUT_IDENTIFIER, JSON_CODEC_IDENTIFIER))
				.describedAs("provider.getLayoutStorage(\"layout\", \"json\")")
				.isInstanceOf(DoNothingLayoutStorage.class);
		assertThat(provider.getLayoutIdentifier())
				.describedAs("provider.getLayoutIdentifier()")
				.isEqualTo(LAYOUT_IDENTIFIER);
		assertThat(provider.getCodecIdentifier())
				.describedAs("provider.getCodecIdentifier()")
				.isEqualTo(JSON_CODEC_IDENTIFIER);
	}
}
