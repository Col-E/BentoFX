package software.coley.bentofx.persistence.core.api.provider;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;
import software.coley.bentofx.persistence.testfixtures.storage.InMemoryLayoutStorage;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class LayoutStorageProviderTest {

	private static final String LAYOUT_IDENTIFIER = "layout-1";
	private static final String CODEC_IDENTIFIER = "json";

	/**
	 * A provider that implements only the required methods, so the surviving
	 * {@link LayoutStorageProvider#isLayoutStored} default can be exercised. The
	 * storage it hands back is supplied per instance, so a test controls whether
	 * a layout appears to exist.
	 */
	private static LayoutStorageProvider providerOver(final LayoutStorage storage) {
		return new LayoutStorageProvider() {
			@Override
			public String getIdentifier() {
				return "minimal";
			}

			@Override
			public LayoutStorage getLayoutStorage(
					final String layoutIdentifier,
					final String codecIdentifier
			) {
				return storage;
			}

			@Override
			public List<String> getLayoutIdentifiers(final String codecIdentifier) {
				return List.of();
			}

			@Override
			public boolean deleteLayout(
					final String layoutIdentifier,
					final String codecIdentifier
			) {
				return false;
			}
		};
	}

	@Test
	void isLayoutStoredDefaultReportsFalseWhenTheStorageIsEmpty() {
		final LayoutStorageProvider provider = providerOver(new InMemoryLayoutStorage());

		assertThat(provider.isLayoutStored(LAYOUT_IDENTIFIER, CODEC_IDENTIFIER))
				.describedAs("isLayoutStored() over empty storage")
				.isFalse();
	}

	@Test
	void isLayoutStoredDefaultReportsTrueWhenTheStorageHoldsALayout() {
		final LayoutStorageProvider provider =
				providerOver(new InMemoryLayoutStorage("stored".getBytes(UTF_8)));

		assertThat(provider.isLayoutStored(LAYOUT_IDENTIFIER, CODEC_IDENTIFIER))
				.describedAs("isLayoutStored() over storage holding a layout")
				.isTrue();
	}
}
