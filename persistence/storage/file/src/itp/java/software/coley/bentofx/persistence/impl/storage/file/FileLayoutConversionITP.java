package software.coley.bentofx.persistence.impl.storage.file;

import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.codec.PersistableLayout;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorageLocations;
import software.coley.bentofx.persistence.impl.codec.json.JsonLayoutCodec;
import software.coley.bentofx.persistence.impl.codec.xml.XmlLayoutCodec;
import software.coley.bentofx.persistence.impl.storage.file.provider.FileLayoutStorageProvider;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers.GROUP_CATALOG_LAYOUT_IDENTIFIER;
import static software.coley.bentofx.persistence.testfixtures.codec.state.SampleBentoStateFactory.createBentoStates;

/**
 * Pins the path taken to store a layout built from something other than a
 * running Bento - one imported from another docking framework, for instance.
 *
 * <p>Code doing that holds no {@code Bento} and runs outside a JavaFX
 * application, so it cannot go through {@code LayoutSaver}. It composes
 * exported types instead: hand-built {@code *State} objects, a
 * {@link LayoutCodec}, and a {@link LayoutStorage} from
 * {@link FileLayoutStorageProvider}. Each of those is covered on its own; the
 * composition is not, and it is what would quietly break if storing a layout
 * ever came to need a live {@code Bento} or a started JavaFX runtime.</p>
 *
 * @author Phil Bryant
 */
class FileLayoutConversionITP {

	private static final String LAYOUT_IDENTIFIER = "converted-layout";
	private static final String DISPLAY_NAME = "Converted Layout";

	// Not @Nullable: @TempDir populates this before any @BeforeEach or @Test
	// method runs, so this class's own code never observes it null.
	@TempDir
	private Path temporaryDirectory;

	/**
	 * {@return every bundled codec, since one is chosen per layout and both
	 * have to work.}
	 */
	static Stream<LayoutCodec> bundledCodecs() {
		return Stream.of(new JsonLayoutCodec(), new XmlLayoutCodec());
	}

	@BeforeEach
	void redirectStorageToATemporaryDirectory() {
		System.setProperty(
				LayoutStorageLocations.HOME_DIRECTORY_PROPERTY,
				temporaryDirectory.toString()
		);
	}

	@AfterEach
	void stopRedirectingStorage() {
		System.clearProperty(LayoutStorageLocations.HOME_DIRECTORY_PROPERTY);
	}

	@ParameterizedTest
	@MethodSource("bundledCodecs")
	void aLayoutIsStoredAndReadBackWithNoBentoAndNoJavaFxRuntime(
			final LayoutCodec layoutCodec
	) throws Exception {
		// Storing a layout cannot require a started JavaFX runtime: an external
		// tool converting layouts from another docking framework has none.
		assertThatThrownBy(() -> Platform.runLater(() -> { }))
				.describedAs("JavaFX runtime state for this suite")
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Toolkit not initialized");

		final List<BentoState> original = createBentoStates();
		final FileLayoutStorageProvider storageProvider =
				new FileLayoutStorageProvider();
		final String codecIdentifier = layoutCodec.getIdentifier();

		try (
				final LayoutStorage layoutStorage =
						storageProvider.getLayoutStorage(
								LAYOUT_IDENTIFIER,
								codecIdentifier
						);
				final OutputStream outputStream =
						layoutStorage.openOutputStream()
		) {
			layoutCodec.encode(
					new PersistableLayout(DISPLAY_NAME, original),
					outputStream
			);
		}

		assertThat(storageProvider.isLayoutStored(
				LAYOUT_IDENTIFIER,
				codecIdentifier
		))
				.describedAs("layout reported as stored after conversion")
				.isTrue();

		final PersistableLayout restored;

		try (
				final LayoutStorage layoutStorage =
						storageProvider.getLayoutStorage(
								LAYOUT_IDENTIFIER,
								codecIdentifier
						);
				final InputStream inputStream =
						layoutStorage.openInputStream()
		) {
			restored = layoutCodec.decode(inputStream);
		}

		assertThat(restored.displayName())
				.describedAs("display name restored from converted layout")
				.isEqualTo(DISPLAY_NAME);

		assertThat(restored.bentoStates())
				.describedAs("Bento states restored from converted layout")
				.usingRecursiveComparison()
				.isEqualTo(original);
	}

	/**
	 * A layout's group is written and read back through a real file, and the
	 * display name beside it is left alone even when it holds the character that
	 * once separated the two.
	 */
	@ParameterizedTest
	@MethodSource("bundledCodecs")
	void aLayoutsGroupIsStoredAndReadBackApartFromItsName(
			final LayoutCodec layoutCodec
	) throws Exception {
		final FileLayoutStorageProvider storageProvider =
				new FileLayoutStorageProvider();

		writeLayout(
				storageProvider,
				layoutCodec,
				LAYOUT_IDENTIFIER,
				new PersistableLayout(
						"TCP/IP Debug",
						createBentoStates(),
						"Debugging",
						List.of()
				)
		);

		final PersistableLayout restored =
				readLayout(storageProvider, layoutCodec, LAYOUT_IDENTIFIER);

		assertThat(restored.displayName())
				.describedAs("display name restored from a grouped layout")
				.isEqualTo("TCP/IP Debug");
		assertThat(restored.group())
				.describedAs("group restored from a grouped layout")
				.isEqualTo("Debugging");
	}

	/**
	 * The group catalog is an ordinary layout entry under a reserved identifier,
	 * which is what keeps it working on every storage implementation without one
	 * of them adding anything. It holds no docking state.
	 */
	@ParameterizedTest
	@MethodSource("bundledCodecs")
	void theGroupCatalogIsStoredAsAnOrdinaryEntry(
			final LayoutCodec layoutCodec
	) throws Exception {
		final FileLayoutStorageProvider storageProvider =
				new FileLayoutStorageProvider();
		final String codecIdentifier = layoutCodec.getIdentifier();

		writeLayout(
				storageProvider,
				layoutCodec,
				GROUP_CATALOG_LAYOUT_IDENTIFIER,
				PersistableLayout.ofGroups(List.of("Debugging", "Presentation"))
		);

		assertThat(storageProvider.getLayoutIdentifiers(codecIdentifier))
				.describedAs("layouts the destination reports")
				.contains(GROUP_CATALOG_LAYOUT_IDENTIFIER);

		final PersistableLayout restored = readLayout(
				storageProvider,
				layoutCodec,
				GROUP_CATALOG_LAYOUT_IDENTIFIER
		);

		assertThat(restored.groups())
				.describedAs("group catalog restored from a real file")
				.containsExactly("Debugging", "Presentation");
		assertThat(restored.bentoStates())
				.describedAs("docking state in the group catalog")
				.isEmpty();
	}

	/**
	 * Writes a layout through real file storage.
	 *
	 * @param storageProvider the storage to write to.
	 * @param layoutCodec the codec to write with.
	 * @param layoutIdentifier addresses the layout.
	 * @param layout what to write.
	 */
	private static void writeLayout(
			final FileLayoutStorageProvider storageProvider,
			final LayoutCodec layoutCodec,
			final String layoutIdentifier,
			final PersistableLayout layout
	) throws Exception {
		try (
				final LayoutStorage layoutStorage =
						storageProvider.getLayoutStorage(
								layoutIdentifier,
								layoutCodec.getIdentifier()
						);
				final OutputStream outputStream =
						layoutStorage.openOutputStream()
		) {
			layoutCodec.encode(layout, outputStream);
		}
	}

	/**
	 * {@return a layout read back through real file storage.}
	 *
	 * @param storageProvider the storage to read from.
	 * @param layoutCodec the codec to read with.
	 * @param layoutIdentifier addresses the layout.
	 */
	private static PersistableLayout readLayout(
			final FileLayoutStorageProvider storageProvider,
			final LayoutCodec layoutCodec,
			final String layoutIdentifier
	) throws Exception {
		try (
				final LayoutStorage layoutStorage =
						storageProvider.getLayoutStorage(
								layoutIdentifier,
								layoutCodec.getIdentifier()
						);
				final InputStream inputStream =
						layoutStorage.openInputStream()
		) {
			return layoutCodec.decode(inputStream);
		}
	}
}
