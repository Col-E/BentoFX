package software.coley.bentofx.persistence.core.impl.provider;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.LayoutRestorer;
import software.coley.bentofx.persistence.core.api.LayoutSaver;
import software.coley.bentofx.persistence.core.api.provider.LayoutStorageProvider;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;
import software.coley.bentofx.persistence.testfixtures.provider.ConfigurableLayoutCodecProvider;
import software.coley.bentofx.persistence.testfixtures.provider.ConfigurableLayoutStorageProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class DefaultDockingLayoutPersistenceProviderTest {

	private static final String TEST_LAYOUT_IDENTIFIER = "test-layout";
	private static final String JSON_CODEC_IDENTIFIER = "json";
	private static final String XML_CODEC_IDENTIFIER = "xml";
	private static final String FILE_STORAGE_IDENTIFIER = "file";
	private static final String DATABASE_STORAGE_IDENTIFIER = "h2";

	private static final String DATABASE_PROVIDER_GET_LAYOUT_IDENTIFIER_DESCRIPTION =
			"databaseProvider.getLayoutIdentifier()";
	private static final String EXCEPTION_THROWN_BY_PROVIDER_GET_DESCRIPTION =
			"exception thrown by () -> provider.getLayoutSaver(\"default\", new DefaultBentoProvider())";
	private static final String FILE_PROVIDER_GET_LAYOUT_IDENTIFIER_DESCRIPTION =
			"fileProvider.getLayoutIdentifier()";
	private static final String JSON_PROVIDER_GET_CREATED_CODEC_COUNT_DESCRIPTION =
			"jsonProvider.getCreatedCodecCount()";
	private static final String XML_PROVIDER_GET_CREATED_CODEC_COUNT_DESCRIPTION =
			"xmlProvider.getCreatedCodecCount()";
	private static final String STORAGE_PROVIDER_GET_CREATED_LAYOUT_STORAGES_DESCRIPTION =
			"storageProvider.getCreatedLayoutStorages()";

	@Test
	void listsStoredLayoutsFromTheSelectedStorageProvider() throws BentoStateException {
		final ConfigurableLayoutCodecProvider codecProvider =
				new ConfigurableLayoutCodecProvider(JSON_CODEC_IDENTIFIER, false);
		final ConfigurableLayoutStorageProvider storageProvider =
				new ConfigurableLayoutStorageProvider(FILE_STORAGE_IDENTIFIER, false);
		storageProvider.setStoredLayoutIdentifiers(
				List.of("compact", "multi-monitor")
		);

		final DefaultDockingLayoutPersistenceProvider provider =
				new DefaultDockingLayoutPersistenceProvider(
						List.of(codecProvider),
						List.of(storageProvider)
				);

		assertThat(provider.getStoredLayoutIdentifiers(
				LayoutPersistenceProfile.of(TEST_LAYOUT_IDENTIFIER)
		))
				.describedAs("layouts the selected storage provider holds")
				.containsExactly("compact", "multi-monitor");
		assertThat(storageProvider.getCatalogCodecIdentifier())
				.describedAs("the codec identifier the catalog was asked with")
				.isEqualTo(JSON_CODEC_IDENTIFIER);
	}

	@Test
	void listsStoredLayoutsAsProfilesCarryingTheProfilesCodecAndStorage()
			throws BentoStateException {

		final ConfigurableLayoutStorageProvider storageProvider =
				new ConfigurableLayoutStorageProvider(FILE_STORAGE_IDENTIFIER, false);
		storageProvider.setStoredLayoutIdentifiers(
				List.of("compact", "multi-monitor")
		);

		final DefaultDockingLayoutPersistenceProvider provider =
				new DefaultDockingLayoutPersistenceProvider(
						List.of(new ConfigurableLayoutCodecProvider(
								JSON_CODEC_IDENTIFIER, false
						)),
						List.of(storageProvider)
				);

		final LayoutPersistenceProfile profile = new LayoutPersistenceProfile(
				TEST_LAYOUT_IDENTIFIER,
				JSON_CODEC_IDENTIFIER,
				FILE_STORAGE_IDENTIFIER
		);

		assertThat(provider.getStoredLayouts(profile))
				.describedAs("stored layouts as profiles")
				.extracting(
						LayoutPersistenceProfile::layoutIdentifier,
						LayoutPersistenceProfile::codecIdentifier,
						LayoutPersistenceProfile::storageIdentifier
				)
				.containsExactlyInAnyOrder(
						tuple(
								"compact",
								JSON_CODEC_IDENTIFIER,
								FILE_STORAGE_IDENTIFIER
						),
						tuple(
								"multi-monitor",
								JSON_CODEC_IDENTIFIER,
								FILE_STORAGE_IDENTIFIER
						)
				);
	}

	@Test
	void reportsAndDeletesOneStoredLayout() throws BentoStateException {
		final ConfigurableLayoutStorageProvider storageProvider =
				new ConfigurableLayoutStorageProvider(FILE_STORAGE_IDENTIFIER, false);
		storageProvider.setStoredLayoutIdentifiers(List.of("compact"));

		final DefaultDockingLayoutPersistenceProvider provider =
				new DefaultDockingLayoutPersistenceProvider(
						List.of(new ConfigurableLayoutCodecProvider(JSON_CODEC_IDENTIFIER, false)),
						List.of(storageProvider)
				);

		final LayoutPersistenceProfile profile =
				LayoutPersistenceProfile.of("compact");

		assertThat(provider.isLayoutStored(profile))
				.describedAs("isLayoutStored for a layout the destination holds")
				.isTrue();
		assertThat(provider.deleteLayout(profile))
				.describedAs("deleteLayout for a layout the destination holds")
				.isTrue();
		assertThat(provider.isLayoutStored(profile))
				.describedAs("isLayoutStored after the delete")
				.isFalse();
		assertThat(provider.deleteLayout(profile))
				.describedAs("deleteLayout for a layout that is already gone")
				.isFalse();
	}

	@Test
	void catalogHonorsTheProfilesStorageIdentifier() throws BentoStateException {
		final ConfigurableLayoutStorageProvider fileProvider =
				new ConfigurableLayoutStorageProvider(FILE_STORAGE_IDENTIFIER, false);
		final ConfigurableLayoutStorageProvider databaseProvider =
				new ConfigurableLayoutStorageProvider(DATABASE_STORAGE_IDENTIFIER, false);
		databaseProvider.setStoredLayoutIdentifiers(List.of("in-the-database"));

		final DefaultDockingLayoutPersistenceProvider provider =
				new DefaultDockingLayoutPersistenceProvider(
						List.of(new ConfigurableLayoutCodecProvider(JSON_CODEC_IDENTIFIER, false)),
						List.of(fileProvider, databaseProvider)
				);

		assertThat(provider.getStoredLayoutIdentifiers(new LayoutPersistenceProfile(
				TEST_LAYOUT_IDENTIFIER,
				JSON_CODEC_IDENTIFIER,
				DATABASE_STORAGE_IDENTIFIER
		)))
				.describedAs("layouts held by the storage the profile names")
				.containsExactly("in-the-database");
		assertThat(fileProvider.getCatalogCodecIdentifier())
				.describedAs("the storage provider the profile did not name")
				.isNull();
	}

	@Test
	void usesSingleCodecAndStorageProvidersWithoutExplicitSelection() throws BentoStateException {
		final ConfigurableLayoutCodecProvider codecProvider = new ConfigurableLayoutCodecProvider(JSON_CODEC_IDENTIFIER, false);
		final ConfigurableLayoutStorageProvider storageProvider = new ConfigurableLayoutStorageProvider(FILE_STORAGE_IDENTIFIER, false);

		final DefaultDockingLayoutPersistenceProvider provider =
				new DefaultDockingLayoutPersistenceProvider(
						List.of(codecProvider),
						List.of(storageProvider)
				);

		provider.getLayoutSaver(TEST_LAYOUT_IDENTIFIER, new DefaultBentoProvider());

		assertThat(codecProvider.getCreatedCodecCount())
				.describedAs("codecProvider.getCreatedCodecCount()")
				.isEqualTo(1);
		assertThat(storageProvider.getLayoutIdentifier())
				.describedAs("storageProvider.getLayoutIdentifier()")
				.isEqualTo(TEST_LAYOUT_IDENTIFIER);
		assertThat(storageProvider.getCodecIdentifier())
				.describedAs("storageProvider.getCodecIdentifier()")
				.isEqualTo(JSON_CODEC_IDENTIFIER);
	}

	@Test
	void usesExplicitProviderIdentifiersWhenMultipleProvidersAreAvailable() throws BentoStateException {
		final ConfigurableLayoutCodecProvider jsonProvider = new ConfigurableLayoutCodecProvider(JSON_CODEC_IDENTIFIER, false);
		final ConfigurableLayoutCodecProvider xmlProvider = new ConfigurableLayoutCodecProvider(XML_CODEC_IDENTIFIER, false);
		final ConfigurableLayoutStorageProvider fileProvider = new ConfigurableLayoutStorageProvider(FILE_STORAGE_IDENTIFIER, false);
		final ConfigurableLayoutStorageProvider databaseProvider = new ConfigurableLayoutStorageProvider(DATABASE_STORAGE_IDENTIFIER, false);

		final DefaultDockingLayoutPersistenceProvider provider =
				new DefaultDockingLayoutPersistenceProvider(
						List.of(jsonProvider, xmlProvider),
						List.of(fileProvider, databaseProvider)
				);

		provider.getLayoutSaver(
				new LayoutPersistenceProfile(TEST_LAYOUT_IDENTIFIER, XML_CODEC_IDENTIFIER, DATABASE_STORAGE_IDENTIFIER),
				new DefaultBentoProvider()
		);

		assertThat(jsonProvider.getCreatedCodecCount())
				.describedAs(JSON_PROVIDER_GET_CREATED_CODEC_COUNT_DESCRIPTION)
				.isZero();
		assertThat(xmlProvider.getCreatedCodecCount())
				.describedAs(XML_PROVIDER_GET_CREATED_CODEC_COUNT_DESCRIPTION)
				.isEqualTo(1);
		assertThat(fileProvider.getLayoutIdentifier())
				.describedAs(FILE_PROVIDER_GET_LAYOUT_IDENTIFIER_DESCRIPTION)
				.isNull();
		assertThat(databaseProvider.getLayoutIdentifier())
				.describedAs(DATABASE_PROVIDER_GET_LAYOUT_IDENTIFIER_DESCRIPTION)
				.isEqualTo(TEST_LAYOUT_IDENTIFIER);
		assertThat(databaseProvider.getCodecIdentifier())
				.describedAs("databaseProvider.getCodecIdentifier()")
				.isEqualTo(XML_CODEC_IDENTIFIER);
	}

	@Test
	void usesSingleDefaultProviderWhenMultipleProvidersAreAvailableWithoutExplicitSelection() throws BentoStateException {
		final ConfigurableLayoutCodecProvider jsonProvider = new ConfigurableLayoutCodecProvider(JSON_CODEC_IDENTIFIER, false);
		final ConfigurableLayoutCodecProvider xmlProvider = new ConfigurableLayoutCodecProvider(XML_CODEC_IDENTIFIER, true);
		final ConfigurableLayoutStorageProvider fileProvider = new ConfigurableLayoutStorageProvider(FILE_STORAGE_IDENTIFIER, true);
		final ConfigurableLayoutStorageProvider databaseProvider = new ConfigurableLayoutStorageProvider(DATABASE_STORAGE_IDENTIFIER, false);

		final DefaultDockingLayoutPersistenceProvider provider =
				new DefaultDockingLayoutPersistenceProvider(
						List.of(jsonProvider, xmlProvider),
						List.of(fileProvider, databaseProvider)
				);

		provider.getLayoutSaver(TEST_LAYOUT_IDENTIFIER, new DefaultBentoProvider());

		assertThat(jsonProvider.getCreatedCodecCount())
				.describedAs(JSON_PROVIDER_GET_CREATED_CODEC_COUNT_DESCRIPTION)
				.isZero();
		assertThat(xmlProvider.getCreatedCodecCount())
				.describedAs(XML_PROVIDER_GET_CREATED_CODEC_COUNT_DESCRIPTION)
				.isEqualTo(1);
		assertThat(fileProvider.getLayoutIdentifier())
				.describedAs(FILE_PROVIDER_GET_LAYOUT_IDENTIFIER_DESCRIPTION)
				.isEqualTo(TEST_LAYOUT_IDENTIFIER);
		assertThat(fileProvider.getCodecIdentifier())
				.describedAs("fileProvider.getCodecIdentifier()")
				.isEqualTo(XML_CODEC_IDENTIFIER);
		assertThat(databaseProvider.getLayoutIdentifier())
				.describedAs(DATABASE_PROVIDER_GET_LAYOUT_IDENTIFIER_DESCRIPTION)
				.isNull();
	}


	@Test
	void providerCreatedSaverClosesLayoutStorage() throws BentoStateException {
		final ConfigurableLayoutCodecProvider codecProvider =
				new ConfigurableLayoutCodecProvider(JSON_CODEC_IDENTIFIER, false);
		final CloseTrackingLayoutStorageProvider storageProvider =
				new CloseTrackingLayoutStorageProvider(FILE_STORAGE_IDENTIFIER);

		final DefaultDockingLayoutPersistenceProvider provider =
				new DefaultDockingLayoutPersistenceProvider(
						List.of(codecProvider),
						List.of(storageProvider)
				);

		final LayoutSaver layoutSaver =
				provider.getLayoutSaver(TEST_LAYOUT_IDENTIFIER, new DefaultBentoProvider());
		layoutSaver.close();
		layoutSaver.close();

		assertThat(storageProvider.getCreatedLayoutStorages())
				.describedAs(STORAGE_PROVIDER_GET_CREATED_LAYOUT_STORAGES_DESCRIPTION)
				.singleElement()
				.extracting(CloseTrackingLayoutStorage::getCloseCount)
				.isEqualTo(1);
	}

	@Test
	void providerCreatedRestorerClosesLayoutStorage() throws BentoStateException {
		final ConfigurableLayoutCodecProvider codecProvider =
				new ConfigurableLayoutCodecProvider(JSON_CODEC_IDENTIFIER, false);
		final CloseTrackingLayoutStorageProvider storageProvider =
				new CloseTrackingLayoutStorageProvider(FILE_STORAGE_IDENTIFIER);

		final DefaultDockingLayoutPersistenceProvider provider =
				new DefaultDockingLayoutPersistenceProvider(
						List.of(codecProvider),
						List.of(storageProvider)
				);

		final LayoutRestorer layoutRestorer =
				provider.getLayoutRestorer(
						TEST_LAYOUT_IDENTIFIER,
						new DefaultBentoProvider(),
						actualId -> Optional.empty(),
						null,
						null
				);
		layoutRestorer.close();
		layoutRestorer.close();

		assertThat(storageProvider.getCreatedLayoutStorages())
				.describedAs(STORAGE_PROVIDER_GET_CREATED_LAYOUT_STORAGES_DESCRIPTION)
				.singleElement()
				.extracting(CloseTrackingLayoutStorage::getCloseCount)
				.isEqualTo(1);
	}

	@Test
	void closingAProviderCreatedSaverLeavesTheRestorersStorageOpen() throws BentoStateException {
		final ConfigurableLayoutCodecProvider codecProvider =
				new ConfigurableLayoutCodecProvider(JSON_CODEC_IDENTIFIER, false);
		final CloseTrackingLayoutStorageProvider storageProvider =
				new CloseTrackingLayoutStorageProvider(FILE_STORAGE_IDENTIFIER);

		final DefaultDockingLayoutPersistenceProvider provider =
				new DefaultDockingLayoutPersistenceProvider(
						List.of(codecProvider),
						List.of(storageProvider)
				);

		final LayoutSaver layoutSaver =
				provider.getLayoutSaver(TEST_LAYOUT_IDENTIFIER, new DefaultBentoProvider());
		provider.getLayoutRestorer(
				TEST_LAYOUT_IDENTIFIER,
				new DefaultBentoProvider(),
				actualId -> Optional.empty(),
				null,
				null
		);

		layoutSaver.close();

		// A storage each, and closing the saver must not reach the restorer's.
		// Collapsing the provider's two getLayoutStorage calls into one shared
		// instance is what this guards against: a saver that outlives nothing
		// would still shut the storage the restorer reads from.
		final List<CloseTrackingLayoutStorage> createdLayoutStorages =
				storageProvider.getCreatedLayoutStorages();
		assertThat(createdLayoutStorages)
				.describedAs(STORAGE_PROVIDER_GET_CREATED_LAYOUT_STORAGES_DESCRIPTION)
				.hasSize(2);
		assertThat(createdLayoutStorages.get(1).getCloseCount())
				.describedAs("close count of the restorer's storage")
				.isZero();
	}

	@Test
	void failsWhenMultipleProvidersAreAvailableWithoutExplicitSelectionOrDefault() {
		final DefaultDockingLayoutPersistenceProvider provider =
				new DefaultDockingLayoutPersistenceProvider(
						List.of(
								new ConfigurableLayoutCodecProvider(JSON_CODEC_IDENTIFIER, false),
								new ConfigurableLayoutCodecProvider(XML_CODEC_IDENTIFIER, false)
						),
						List.of(new ConfigurableLayoutStorageProvider(FILE_STORAGE_IDENTIFIER, false))
				);

		assertThatThrownBy(() -> provider.getLayoutSaver(TEST_LAYOUT_IDENTIFIER, new DefaultBentoProvider()))
				.describedAs(EXCEPTION_THROWN_BY_PROVIDER_GET_DESCRIPTION)
				.isInstanceOf(BentoStateException.class)
				.hasMessageContaining("Multiple LayoutCodecProvider implementations")
				.hasMessageContaining(JSON_CODEC_IDENTIFIER)
				.hasMessageContaining(XML_CODEC_IDENTIFIER);
	}

	@Test
	void failsWhenExplicitProviderIdentifierIsUnavailable() {
		final DefaultDockingLayoutPersistenceProvider provider =
				new DefaultDockingLayoutPersistenceProvider(
						List.of(new ConfigurableLayoutCodecProvider(JSON_CODEC_IDENTIFIER, false)),
						List.of(new ConfigurableLayoutStorageProvider(FILE_STORAGE_IDENTIFIER, false))
				);

		assertThatThrownBy(() -> provider.getLayoutSaver(
				new LayoutPersistenceProfile(TEST_LAYOUT_IDENTIFIER, XML_CODEC_IDENTIFIER, FILE_STORAGE_IDENTIFIER),
				new DefaultBentoProvider()
		))
				.describedAs("exception thrown by () -> provider.getLayoutSaver( new LayoutPersistenceProfile(\"defaul...")
				.isInstanceOf(BentoStateException.class)
				.hasMessageContaining(XML_CODEC_IDENTIFIER)
				.hasMessageContaining(JSON_CODEC_IDENTIFIER);
	}

	@Test
	void failsWhenNoCodecProvidersAreAvailable() {
		final DefaultDockingLayoutPersistenceProvider provider =
				new DefaultDockingLayoutPersistenceProvider(
						List.of(),
						List.of(new ConfigurableLayoutStorageProvider(FILE_STORAGE_IDENTIFIER, false))
				);

		assertThatThrownBy(() -> provider.getLayoutSaver(TEST_LAYOUT_IDENTIFIER, new DefaultBentoProvider()))
				.describedAs(EXCEPTION_THROWN_BY_PROVIDER_GET_DESCRIPTION)
				.isInstanceOf(BentoStateException.class)
				.hasMessageContaining("No LayoutCodecProvider implementation was found");
	}

	@Test
	void failsWhenMultipleDefaultCodecProvidersAreAvailable() {
		final DefaultDockingLayoutPersistenceProvider provider =
				new DefaultDockingLayoutPersistenceProvider(
						List.of(
								new ConfigurableLayoutCodecProvider(JSON_CODEC_IDENTIFIER, true),
								new ConfigurableLayoutCodecProvider(XML_CODEC_IDENTIFIER, true)
						),
						List.of(new ConfigurableLayoutStorageProvider(FILE_STORAGE_IDENTIFIER, false))
				);

		assertThatThrownBy(() -> provider.getLayoutSaver(TEST_LAYOUT_IDENTIFIER, new DefaultBentoProvider()))
				.describedAs(EXCEPTION_THROWN_BY_PROVIDER_GET_DESCRIPTION)
				.isInstanceOf(BentoStateException.class)
				.hasMessageContaining("Multiple default LayoutCodecProvider implementations")
				.hasMessageContaining(JSON_CODEC_IDENTIFIER)
				.hasMessageContaining(XML_CODEC_IDENTIFIER);
	}

	/**
	 * The no-args constructor discovers providers with {@code ServiceLoader}
	 * rather than taking them explicitly. This module declares no {@code
	 * provides} clause of its own, so discovery legitimately finds nothing -
	 * what this covers is that construction itself succeeds either way.
	 */
	@Test
	void noArgsConstructorDiscoversProvidersViaServiceLoaderWithoutThrowing() {
		assertThatCode(DefaultDockingLayoutPersistenceProvider::new)
				.describedAs("constructing DefaultDockingLayoutPersistenceProvider() with no explicit providers")
				.doesNotThrowAnyException();
	}

	/**
	 * A layout whose storage cannot be opened is still listed, without its
	 * naming, rather than failing the listing for every other layout.
	 */
	@Test
	void getStoredLayoutsListsALayoutItCannotReadWithoutItsNaming()
			throws BentoStateException {
		final String layoutIdentifier = "unreadable";
		final IOException openFailure = new IOException("could not open");
		final FailingOpenLayoutStorageProvider storageProvider =
				new FailingOpenLayoutStorageProvider(
						FILE_STORAGE_IDENTIFIER,
						layoutIdentifier,
						openFailure
				);

		final DefaultDockingLayoutPersistenceProvider provider =
				new DefaultDockingLayoutPersistenceProvider(
						List.of(new ConfigurableLayoutCodecProvider(JSON_CODEC_IDENTIFIER, false)),
						List.of(storageProvider)
				);

		final List<LayoutPersistenceProfile> listed = provider.getStoredLayouts(
				new LayoutPersistenceProfile(
						TEST_LAYOUT_IDENTIFIER,
						JSON_CODEC_IDENTIFIER,
						FILE_STORAGE_IDENTIFIER
				)
		);

		assertThat(listed)
				.describedAs("layouts listed when one cannot be read")
				.singleElement()
				.satisfies(layout -> {
					assertThat(layout.layoutIdentifier())
							.describedAs("unreadable layout's identifier")
							.isEqualTo(layoutIdentifier);
					assertThat(layout.displayName())
							.describedAs("unreadable layout's display name")
							.isNull();
				});
	}

	/**
	 * Reports one stored layout, whose storage fails to open for reading -
	 * the way an entry could disappear, or become unreadable, between being
	 * listed and being read.
	 */
	private static final class FailingOpenLayoutStorageProvider
			extends software.coley.bentofx.persistence.testfixtures.provider.AbstractConfigurableLayoutProvider
			implements LayoutStorageProvider {

		private final String layoutIdentifier;
		private final IOException openFailure;

		private FailingOpenLayoutStorageProvider(
				final String identifier,
				final String layoutIdentifier,
				final IOException openFailure
		) {
			super(identifier, false);
			this.layoutIdentifier = layoutIdentifier;
			this.openFailure = openFailure;
		}

		@Override
		public List<String> getLayoutIdentifiers(final String codecIdentifier) {
			return List.of(layoutIdentifier);
		}

		@Override
		public boolean deleteLayout(
				final String layoutIdentifier,
				final String codecIdentifier
		) {
			return false;
		}

		@Override
		public LayoutStorage getLayoutStorage(
				final String layoutIdentifier,
				final String codecIdentifier
		) {
			return new LayoutStorage() {
				@Override
				public boolean exists() {
					return true;
				}

				@Override
				public InputStream openInputStream() throws IOException {
					throw openFailure;
				}

				@Override
				public OutputStream openOutputStream() {
					return new ByteArrayOutputStream();
				}
			};
		}
	}


	/**
	 * Hands out a fresh storage per call, the way a real
	 * {@link LayoutStorageProvider} does, and keeps every instance it created so
	 * a test can tell which component closed which.
	 */
	private static final class CloseTrackingLayoutStorageProvider implements LayoutStorageProvider {
		private final String identifier;
		private final List<CloseTrackingLayoutStorage> createdLayoutStorages =
				new ArrayList<>();

		private CloseTrackingLayoutStorageProvider(final String identifier) {
			this.identifier = identifier;
		}

		@Override
		public String getIdentifier() {
			return identifier;
		}

		@Override
		public LayoutStorage getLayoutStorage(
				final String layoutIdentifier,
				final String codecIdentifier
		) {
			final CloseTrackingLayoutStorage layoutStorage =
					new CloseTrackingLayoutStorage();
			createdLayoutStorages.add(layoutStorage);
			return layoutStorage;
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

		private List<CloseTrackingLayoutStorage> getCreatedLayoutStorages() {
			return createdLayoutStorages;
		}
	}

	private static final class CloseTrackingLayoutStorage implements LayoutStorage {
		private int closeCount;

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public OutputStream openOutputStream() {
			return new ByteArrayOutputStream();
		}

		@Override
		public InputStream openInputStream() {
			return new ByteArrayInputStream(new byte[0]);
		}

		@Override
		public void close() {
			closeCount++;
		}

		private int getCloseCount() {
			return closeCount;
		}
	}
}
