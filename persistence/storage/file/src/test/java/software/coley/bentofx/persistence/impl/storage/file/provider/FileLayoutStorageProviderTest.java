package software.coley.bentofx.persistence.impl.storage.file.provider;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorageLocations;
import software.coley.bentofx.persistence.impl.storage.file.FileLayoutStorage;

import java.io.File;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileLayoutStorageProviderTest {

	private static final String FILE_STORAGE_IDENTIFIER = "file";
	private static final String MAIN_LAYOUT_IDENTIFIER = "main-layout";

	@Test
	void exposesFileIdentifierAndIsDefault() {
		final FileLayoutStorageProvider provider = new FileLayoutStorageProvider();

		assertThat(provider.getIdentifier())
				.describedAs("provider.getIdentifier()")
				.isEqualTo(FILE_STORAGE_IDENTIFIER);
		assertThat(provider.isDefault())
				.describedAs("provider.isDefault()")
				.isTrue();
	}

	@Test
	void createsFileStorageUsingLayoutIdentifierAndNormalizedCodecExtension() throws Exception {
		final FileLayoutStorageProvider provider = new FileLayoutStorageProvider();

		final Object storage = provider.getLayoutStorage(MAIN_LAYOUT_IDENTIFIER, ".json");

		assertThat(storage)
				.describedAs("storage")
				.isInstanceOf(FileLayoutStorage.class);
		assertThat(layoutFile((FileLayoutStorage) storage))
				.describedAs("layoutFile((FileLayoutStorage) storage)")
				.isEqualTo(new File(
						System.getProperty(LayoutStorageLocations.USER_HOME_PROPERTY),
						LayoutStorageLocations.BENTOFX_DIRECTORY_NAME + "/layouts/main-layout.json"
				));
	}

	@Test
	void createsFileStorageWithoutAddingDuplicateExtensionSeparator() throws Exception {
		final FileLayoutStorageProvider provider = new FileLayoutStorageProvider();

		final FileLayoutStorage storage = (FileLayoutStorage) provider.getLayoutStorage(
				MAIN_LAYOUT_IDENTIFIER,
				"xml"
		);

		assertThat(layoutFile(storage))
				.describedAs("layoutFile(storage)")
				.isEqualTo(new File(
						System.getProperty(LayoutStorageLocations.USER_HOME_PROPERTY),
						LayoutStorageLocations.BENTOFX_DIRECTORY_NAME + "/layouts/main-layout.xml"
				));
	}

	@Test
	void rejectsALayoutIdentifierThatLeavesTheBentoDirectory() {
		final FileLayoutStorageProvider provider = new FileLayoutStorageProvider();

		assertThatThrownBy(() -> provider.getLayoutStorage("../escaped", "json"))
				.describedAs("layout identifier with a parent segment")
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("../escaped");
	}

	@Test
	void rejectsALayoutIdentifierNamingASubdirectory() {
		final FileLayoutStorageProvider provider = new FileLayoutStorageProvider();

		assertThatThrownBy(() -> provider.getLayoutStorage("nested/layout", "json"))
				.describedAs("layout identifier with a separator")
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("nested/layout");
	}

	@Test
	void rejectsACodecIdentifierThatLeavesTheBentoDirectory() {
		final FileLayoutStorageProvider provider = new FileLayoutStorageProvider();

		assertThatThrownBy(() ->
				provider.getLayoutStorage(MAIN_LAYOUT_IDENTIFIER, "json/../../escaped")
		)
				.describedAs("codec identifier with parent segments")
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("escaped");
	}

	@Test
	void appliesTheSharedIdentifierRule() {
		final FileLayoutStorageProvider provider = new FileLayoutStorageProvider();

		// A device name is a perfectly good path component, so the containment
		// check below it cannot object: only the shared rule rejects this.
		assertThatThrownBy(() -> provider.getLayoutStorage("nul", "json"))
				.describedAs("layout identifier naming a device")
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("device");
	}

	@Test
	// Suppress warnings for passing null argument to parameter annotated as
	// non-null; that's what we're testing.
	@SuppressWarnings("NullAway")
	void rejectsAMissingLayoutIdentifier() {
		final FileLayoutStorageProvider provider = new FileLayoutStorageProvider();

		assertThatThrownBy(() -> provider.getLayoutStorage(null, "json"))
				.describedAs("null layout identifier")
				.isInstanceOf(NullPointerException.class)
				.hasMessage("layoutIdentifier");
	}

	private static File layoutFile(final FileLayoutStorage storage) throws Exception {
		final Field fileField = FileLayoutStorage.class.getDeclaredField(FILE_STORAGE_IDENTIFIER);
		fileField.setAccessible(true);
		return (File) fileField.get(storage);
	}
}
