package software.coley.bentofx.persistence.impl.storage.file.provider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorageLocations;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests listing and deleting stored layouts against a real directory.
 */
class FileLayoutStorageCatalogITP {

	private static final String CODEC_IDENTIFIER = "json";
	private static final String OTHER_CODEC_IDENTIFIER = "xml";
	private static final String LAYOUTS_DIRECTORY_PATH =
			LayoutStorageLocations.BENTOFX_DIRECTORY_NAME + "/layouts";
	private static final String LAYOUT_CONTENT = "{}";

	@TempDir
	private static Path temporaryHome;

	private static Path bentoDirectory;

	/**
	 * Because the provider resolves its directory from {@code user.home} on
	 * every call, these tests direct {@code user.home} to a temporary
	 * directory before using the provider. Then tests to determine if the
	 * redirection succeeded by writing a layout through the provider and
	 * finding it there.
	 *
	 * @throws IOException when writing the probe layout fails.
	 */
	@BeforeAll
	static void redirectUserHomeAndProveItTook() throws IOException {
		System.setProperty(LayoutStorageLocations.USER_HOME_PROPERTY, temporaryHome.toString());

		bentoDirectory = Files.createDirectories(
				temporaryHome.resolve(LAYOUTS_DIRECTORY_PATH)
		);

		final FileLayoutStorageProvider provider =
				new FileLayoutStorageProvider();

		try (final LayoutStorage storage =
					 provider.getLayoutStorage(
							 "redirect-probe",
							 CODEC_IDENTIFIER
					 );
			 final OutputStream outputStream = storage.openOutputStream()
		) {

			outputStream.write(LAYOUT_CONTENT.getBytes(UTF_8));
		}

		assertThat(bentoDirectory.resolve("redirect-probe." + CODEC_IDENTIFIER))
				.describedAs("a layout written through the provider")
				.exists();
	}

	@BeforeEach
	void emptyTheLayoutDirectory() throws IOException {
		try (final Stream<Path> entries = Files.list(bentoDirectory)) {
			for (final Path entry : entries.toList()) {
				deleteRecursively(entry);
			}
		}
	}

	/**
	 * Belt and suspenders: a test that sets these and fails before its own
	 * cleanup runs must not leave a redirected home or namespace behind for
	 * whatever test in this class runs next.
	 */
	@AfterEach
	void clearHomeAndNamespaceOverrides() {
		System.clearProperty(LayoutStorageLocations.HOME_DIRECTORY_PROPERTY);
		System.clearProperty(LayoutStorageLocations.NAMESPACE_PROPERTY);
	}

	@Test
	void listsOnlyTheLayoutsStoredForTheCodec() throws IOException {
		writeLayout("first.json");
		writeLayout("second.json");
		writeLayout("elsewhere.xml");

		final FileLayoutStorageProvider provider = new FileLayoutStorageProvider();

		assertThat(provider.getLayoutIdentifiers(CODEC_IDENTIFIER))
				.describedAs("layouts stored for the json codec")
				.containsExactlyInAnyOrder("first", "second");
		assertThat(provider.getLayoutIdentifiers(OTHER_CODEC_IDENTIFIER))
				.describedAs("layouts stored for the xml codec")
				.containsExactly("elsewhere");
	}

	@Test
	void skipsAnEmptyFileAndADirectory() throws IOException {
		writeLayout("real.json");
		Files.createFile(bentoDirectory.resolve("empty.json"));
		Files.createDirectories(bentoDirectory.resolve("directory.json"));

		assertThat(new FileLayoutStorageProvider().getLayoutIdentifiers(CODEC_IDENTIFIER))
				.describedAs("layouts stored, with an empty file and a directory present")
				.containsExactly("real");
	}

	@Test
	void reportsNoLayoutsForACodecNothingWasWrittenWith() throws IOException {
		writeLayout("only.json");

		assertThat(new FileLayoutStorageProvider().getLayoutIdentifiers("none"))
				.describedAs("layouts stored for a codec nothing was written with")
				.isEmpty();
	}

	@Test
	void deletesOneLayoutAndReportsWhetherThereWasOne() throws IOException {
		writeLayout("removable.json");

		final FileLayoutStorageProvider provider = new FileLayoutStorageProvider();

		assertThat(provider.isLayoutStored("removable", CODEC_IDENTIFIER))
				.describedAs("isLayoutStored before the delete")
				.isTrue();
		assertThat(provider.deleteLayout("removable", CODEC_IDENTIFIER))
				.describedAs("deleteLayout for a stored layout")
				.isTrue();
		assertThat(provider.isLayoutStored("removable", CODEC_IDENTIFIER))
				.describedAs("isLayoutStored after the delete")
				.isFalse();
		assertThat(provider.deleteLayout("removable", CODEC_IDENTIFIER))
				.describedAs("deleteLayout for a layout that is already gone")
				.isFalse();
	}

	/**
	 * A separate, never-redirected-to home directory, distinct from the
	 * class-level one every other test in this file shares: that one already
	 * has its layouts directory created by {@link #redirectUserHomeAndProveItTook()}.
	 */
	@Test
	void reportsNoLayoutsWhenTheLayoutsDirectoryDoesNotExistYet(
			@TempDir final Path freshHome
	) {
		final String realUserHome = System.getProperty(LayoutStorageLocations.USER_HOME_PROPERTY);
		System.setProperty(LayoutStorageLocations.USER_HOME_PROPERTY, freshHome.toString());

		try {
			assertThat(new FileLayoutStorageProvider().getLayoutIdentifiers(CODEC_IDENTIFIER))
					.describedAs("layouts stored when the layouts directory does not exist yet")
					.isEmpty();
		} finally {
			System.setProperty(LayoutStorageLocations.USER_HOME_PROPERTY, realUserHome);
		}
	}

	/**
	 * {@value LayoutStorageLocations#HOME_DIRECTORY_PROPERTY} takes priority
	 * over {@code user.home}, so an application can relocate storage without
	 * touching a property this framework does not own.
	 */
	@Test
	void homePropertyRedirectsWhereLayoutsAreStored(
			@TempDir final Path customHome
	) throws IOException {
		System.setProperty(
				LayoutStorageLocations.HOME_DIRECTORY_PROPERTY,
				customHome.toString()
		);

		writeThroughProvider(new FileLayoutStorageProvider(), "relocated");

		assertThat(customHome.resolve("layouts/relocated." + CODEC_IDENTIFIER))
				.describedAs("a layout written under the home override")
				.exists();
		assertThat(new FileLayoutStorageProvider().getLayoutIdentifiers(CODEC_IDENTIFIER))
				.describedAs("layouts visible under the home override")
				.containsExactly("relocated");
	}

	/**
	 * The concrete "two applications on one machine" scenario:
	 * {@value LayoutStorageLocations#NAMESPACE_PROPERTY} gives each one its
	 * own subdirectory of a shared home, so neither sees the other's layouts.
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
		writeThroughProvider(new FileLayoutStorageProvider(), "shared-name");

		System.setProperty(LayoutStorageLocations.NAMESPACE_PROPERTY, "app-two");
		writeThroughProvider(new FileLayoutStorageProvider(), "shared-name");

		assertThat(new FileLayoutStorageProvider().getLayoutIdentifiers(CODEC_IDENTIFIER))
				.describedAs("layouts visible to app-two")
				.containsExactly("shared-name");

		System.setProperty(LayoutStorageLocations.NAMESPACE_PROPERTY, "app-one");
		assertThat(new FileLayoutStorageProvider().getLayoutIdentifiers(CODEC_IDENTIFIER))
				.describedAs("layouts visible to app-one")
				.containsExactly("shared-name");

		assertThat(sharedHome.resolve("app-one/layouts/shared-name." + CODEC_IDENTIFIER))
				.describedAs("app-one's own copy of the identically-named layout")
				.exists();
		assertThat(sharedHome.resolve("app-two/layouts/shared-name." + CODEC_IDENTIFIER))
				.describedAs("app-two's own copy of the identically-named layout")
				.exists();
	}

	private static void writeThroughProvider(
			final FileLayoutStorageProvider provider,
			final String layoutIdentifier
	) throws IOException {
		try (final LayoutStorage storage =
					 provider.getLayoutStorage(layoutIdentifier, CODEC_IDENTIFIER);
			 final OutputStream outputStream = storage.openOutputStream()) {
			outputStream.write(LAYOUT_CONTENT.getBytes(UTF_8));
		}
	}

	private static void writeLayout(final String fileName) throws IOException {
		Files.writeString(bentoDirectory.resolve(fileName), LAYOUT_CONTENT, UTF_8);
	}

	private static void deleteRecursively(final Path path) throws IOException {
		if (Files.isDirectory(path)) {
			try (final Stream<Path> children = Files.list(path)) {
				for (final Path child : children.toList()) {
					deleteRecursively(child);
				}
			}
		}

		Files.deleteIfExists(path);
	}
}
