package software.coley.bentofx.persistence.impl.storage.db.provider;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorageLocations;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static software.coley.bentofx.persistence.core.api.storage.LayoutStorageLocations.USER_HOME_PROPERTY;

/**
 * Exercises the persistence unit the module contains, which the storage tests
 * replace with a temporary one. Everything the packaged unit decides: where the
 * database file goes, what credentials open it, how the table comes to exist.
 */
class DatabaseLayoutStorageProviderITP {

	private static final String LAYOUT_IDENTIFIER = "provider-layout";
	private static final String CODEC_IDENTIFIER = "json";
	private static final String TEST_DATA = "A layout stored through the provider.";

	// Not @Nullable: JUnit's @TempDir extension populates this before any
	// @BeforeEach or @Test method runs, so it is never actually null at any
	// point this class's own code observes it.
	@TempDir
	private Path temporaryHome;

	private @Nullable String realUserHome;

	@BeforeEach
	void setUp() {
		realUserHome = System.getProperty(USER_HOME_PROPERTY);

		// The declared JDBC URL is relative to the user's home, and Hibernate
		// resolves that placeholder from the system property when the factory is
		// created.
		System.setProperty(USER_HOME_PROPERTY, temporaryHome.toString());
	}

	@AfterEach
	void tearDown() {
		setOrClearUserHome(realUserHome);
		System.clearProperty(LayoutStorageLocations.HOME_DIRECTORY_PROPERTY);
		System.clearProperty(LayoutStorageLocations.NAMESPACE_PROPERTY);
	}

	/**
	 * Restores {@value LayoutStorageLocations#USER_HOME_PROPERTY} to what it
	 * was before this test redirected it - cleared, not set to {@code null},
	 * on the off chance it was not set to begin with, since
	 * {@code System.setProperty} rejects a {@code null} value outright.
	 *
	 * @param value the property's original value, or {@code null} when it had
	 * none.
	 */
	private static void setOrClearUserHome(final @Nullable String value) {
		if (value == null) {
			System.clearProperty(USER_HOME_PROPERTY);
		} else {
			System.setProperty(USER_HOME_PROPERTY, value);
		}
	}

	@Test
	void storesAndReadsALayoutThroughTheProvidedPersistenceUnit() throws IOException {
		final DatabaseLayoutStorageProvider provider =
				new DatabaseLayoutStorageProvider();

		try (LayoutStorage storage =
					 provider.getLayoutStorage(LAYOUT_IDENTIFIER, CODEC_IDENTIFIER)) {

			assertThat(storage.exists())
					.describedAs("storage.exists() before anything is stored")
					.isFalse();

			try (OutputStream outputStream = storage.openOutputStream()) {
				outputStream.write(TEST_DATA.getBytes(UTF_8));
			}

			assertThat(storage.exists())
					.describedAs("storage.exists() after storing a layout")
					.isTrue();

			try (InputStream inputStream = storage.openInputStream()) {
				assertThat(new String(inputStream.readAllBytes(), UTF_8))
						.describedAs("stored layout read back")
						.isEqualTo(TEST_DATA);
			}
		}

		assertThat(Files.isDirectory(temporaryHome.resolve(LayoutStorageLocations.BENTOFX_DIRECTORY_NAME)))
				.describedAs("the database was created under the user's home")
				.isTrue();
	}

	@Test
	void listsAndDeletesStoredLayouts() throws IOException {
		final DatabaseLayoutStorageProvider provider =
				new DatabaseLayoutStorageProvider();

		storeLayout(provider, "first-layout");
		storeLayout(provider, "second-layout");

		assertThat(provider.getLayoutIdentifiers(CODEC_IDENTIFIER))
				.describedAs("layouts stored for this codec")
				.containsExactlyInAnyOrder("first-layout", "second-layout");
		assertThat(provider.getLayoutIdentifiers("none"))
				.describedAs("layouts stored for a codec nothing was written with")
				.isEmpty();

		assertThat(provider.isLayoutStored("first-layout", CODEC_IDENTIFIER))
				.describedAs("isLayoutStored before the delete")
				.isTrue();
		assertThat(provider.deleteLayout("first-layout", CODEC_IDENTIFIER))
				.describedAs("deleteLayout for a stored layout")
				.isTrue();
		assertThat(provider.deleteLayout("first-layout", CODEC_IDENTIFIER))
				.describedAs("deleteLayout for a layout that is already gone")
				.isFalse();

		assertThat(provider.getLayoutIdentifiers(CODEC_IDENTIFIER))
				.describedAs("layouts stored after the delete")
				.containsExactly("second-layout");
	}

	/**
	 * Writes a layout through the provider, so that the catalog reads rows the
	 * provider's own storage produced.
	 *
	 * @param provider the provider to store through.
	 * @param layoutIdentifier identifies the layout to store.
	 * @throws IOException when the write fails.
	 */
	private static void storeLayout(
			final DatabaseLayoutStorageProvider provider,
			final String layoutIdentifier
	) throws IOException {
		try (final LayoutStorage storage =
					 provider.getLayoutStorage(layoutIdentifier, CODEC_IDENTIFIER);
			 final OutputStream outputStream = storage.openOutputStream()) {

			outputStream.write(TEST_DATA.getBytes(UTF_8));
		}
	}

	@Test
	void handsOutAFreshStorageEachTime() {
		final DatabaseLayoutStorageProvider provider =
				new DatabaseLayoutStorageProvider();

		try (LayoutStorage first =
					 provider.getLayoutStorage(LAYOUT_IDENTIFIER, CODEC_IDENTIFIER);
			 LayoutStorage second =
					 provider.getLayoutStorage(LAYOUT_IDENTIFIER, CODEC_IDENTIFIER)) {

			assertThat(first)
					.describedAs("second storage for the same layout")
					.isNotSameAs(second);
		}
	}

	/**
	 * {@value LayoutStorageLocations#HOME_DIRECTORY_PROPERTY} takes priority
	 * over {@code user.home}, so an application can relocate the database
	 * without touching a property this framework does not own.
	 */
	@Test
	void homePropertyRedirectsWhereTheDatabaseIsStored(
			@TempDir final Path customHome
	) throws IOException {
		System.setProperty(
				LayoutStorageLocations.HOME_DIRECTORY_PROPERTY,
				customHome.toString()
		);

		storeLayout(new DatabaseLayoutStorageProvider(), "relocated");

		try (final Stream<Path> entries = Files.list(customHome)) {
			assertThat(entries.toList())
					.describedAs("entries directly under the home override")
					.isNotEmpty();
		}
		assertThat(Files.isDirectory(temporaryHome.resolve(LayoutStorageLocations.BENTOFX_DIRECTORY_NAME)))
				.describedAs("a database created under user.home despite the override")
				.isFalse();
	}

	/**
	 * One provider instance follows a home directory changed after it was
	 * first used, as the file-backed provider does, rather than staying on the
	 * database it opened first.
	 */
	@Test
	void aProviderAlreadyInUseFollowsAChangedHome(
			@TempDir final Path firstHome,
			@TempDir final Path secondHome
	) throws IOException {
		final DatabaseLayoutStorageProvider provider =
				new DatabaseLayoutStorageProvider();

		LayoutStorageLocations.configureHome(firstHome);
		storeLayout(provider, "first-home-layout");

		LayoutStorageLocations.configureHome(secondHome);

		assertThat(provider.getLayoutIdentifiers(CODEC_IDENTIFIER))
				.describedAs("layouts visible after the home changed")
				.isEmpty();

		LayoutStorageLocations.configureHome(firstHome);

		assertThat(provider.getLayoutIdentifiers(CODEC_IDENTIFIER))
				.describedAs("layouts visible after changing the home back")
				.containsExactly("first-home-layout");
	}

	/**
	 * The concrete "two applications on one machine" scenario:
	 * {@value LayoutStorageLocations#NAMESPACE_PROPERTY} gives each one its
	 * own database file under a shared home, so neither sees the other's
	 * layouts even when they use the same layout identifier.
	 */
	@Test
	void namespacePropertyIsolatesTwoApplicationsSharingTheSameHome(
			@TempDir final Path sharedHome
	) throws IOException {
		System.setProperty(
				LayoutStorageLocations.HOME_DIRECTORY_PROPERTY,
				sharedHome.toString()
		);

		System.setProperty(LayoutStorageLocations.NAMESPACE_PROPERTY, "app-one");
		storeLayout(new DatabaseLayoutStorageProvider(), "shared-name");

		System.setProperty(LayoutStorageLocations.NAMESPACE_PROPERTY, "app-two");
		final DatabaseLayoutStorageProvider appTwoProvider =
				new DatabaseLayoutStorageProvider();
		storeLayout(appTwoProvider, "shared-name");

		assertThat(appTwoProvider.getLayoutIdentifiers(CODEC_IDENTIFIER))
				.describedAs("layouts visible to app-two")
				.containsExactly("shared-name");

		System.setProperty(LayoutStorageLocations.NAMESPACE_PROPERTY, "app-one");
		assertThat(new DatabaseLayoutStorageProvider().getLayoutIdentifiers(CODEC_IDENTIFIER))
				.describedAs("layouts visible to app-one")
				.containsExactly("shared-name");

		assertThat(Files.isDirectory(sharedHome.resolve("app-one")))
				.describedAs("app-one's own subdirectory")
				.isTrue();
		assertThat(Files.isDirectory(sharedHome.resolve("app-two")))
				.describedAs("app-two's own subdirectory")
				.isTrue();
	}
}
