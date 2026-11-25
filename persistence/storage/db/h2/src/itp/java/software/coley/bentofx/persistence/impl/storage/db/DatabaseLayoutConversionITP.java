package software.coley.bentofx.persistence.impl.storage.db;

import javafx.application.Platform;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import software.coley.bentofx.persistence.impl.storage.db.provider.DatabaseLayoutStorageProvider;

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
 * {@link DatabaseLayoutStorageProvider}. That provider starts Hibernate and a
 * connection pool lazily, inside
 * {@link DatabaseLayoutStorageProvider#getLayoutStorage}, on whatever thread
 * calls it, so this is where that bootstrap is shown to need no started JavaFX
 * runtime.</p>
 *
 * <p>The provider also resolves the database location once, when the first
 * storage is requested, which is why
 * {@link LayoutStorageLocations#HOME_DIRECTORY_PROPERTY} is set before the
 * provider is built rather than per test. Code that decides where to write
 * after it has already requested a storage would write to the wrong
 * database.</p>
 *
 * @author Phil Bryant
 */
class DatabaseLayoutConversionITP {

	private static final String LAYOUT_IDENTIFIER = "converted-layout";
	private static final String DISPLAY_NAME = "Converted Layout";

	@TempDir
	private static Path temporaryDirectory;

	private static DatabaseLayoutStorageProvider storageProvider;

	/**
	 * {@return every bundled codec, since one is chosen per layout and both
	 * have to work.}
	 *
	 * <p>Both share one database: a row is keyed by layout identifier and codec
	 * identifier together, so the two cannot collide.</p>
	 */
	static Stream<LayoutCodec> bundledCodecs() {
		return Stream.of(new JsonLayoutCodec(), new XmlLayoutCodec());
	}

	@BeforeAll
	static void redirectStorageToATemporaryDirectory() {
		System.setProperty(
				LayoutStorageLocations.HOME_DIRECTORY_PROPERTY,
				temporaryDirectory.toString()
		);

		storageProvider = new DatabaseLayoutStorageProvider();
	}

	@AfterAll
	static void stopRedirectingStorage() {
		// The factory this provider opened stays open: it is created lazily and
		// LayoutStorageProvider has no shutdown to hook into, which the
		// provider's own documentation calls out. The embedded database goes
		// when the JVM does, and it lives under a temporary directory.
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
	 * A layout's group and its display name survive a real row, apart from each
	 * other, and the group needs no column of its own: it rides inside the
	 * payload the way the display name already does.
	 */
	@ParameterizedTest
	@MethodSource("bundledCodecs")
	void aLayoutsGroupIsStoredAndReadBackApartFromItsName(
			final LayoutCodec layoutCodec
	) throws Exception {
		final String layoutIdentifier =
				"grouped-" + layoutCodec.getIdentifier();

		writeLayout(
				layoutCodec,
				layoutIdentifier,
				new PersistableLayout(
						"TCP/IP Debug",
						createBentoStates(),
						"Debugging",
						List.of()
				)
		);

		final PersistableLayout restored =
				readLayout(layoutCodec, layoutIdentifier);

		assertThat(restored.displayName())
				.describedAs("display name restored from a grouped layout")
				.isEqualTo("TCP/IP Debug");
		assertThat(restored.group())
				.describedAs("group restored from a grouped layout")
				.isEqualTo("Debugging");
	}

	/**
	 * The group catalog is an ordinary row under a reserved identifier, so the
	 * schema is untouched by groups existing.
	 */
	@ParameterizedTest
	@MethodSource("bundledCodecs")
	void theGroupCatalogIsStoredAsAnOrdinaryRow(
			final LayoutCodec layoutCodec
	) throws Exception {
		writeLayout(
				layoutCodec,
				GROUP_CATALOG_LAYOUT_IDENTIFIER,
				PersistableLayout.ofGroups(List.of("Debugging", "Presentation"))
		);

		assertThat(storageProvider.getLayoutIdentifiers(
				layoutCodec.getIdentifier()
		))
				.describedAs("layouts the destination reports")
				.contains(GROUP_CATALOG_LAYOUT_IDENTIFIER);

		final PersistableLayout restored =
				readLayout(layoutCodec, GROUP_CATALOG_LAYOUT_IDENTIFIER);

		assertThat(restored.groups())
				.describedAs("group catalog restored from a real row")
				.containsExactly("Debugging", "Presentation");
		assertThat(restored.bentoStates())
				.describedAs("docking state in the group catalog")
				.isEmpty();
	}

	/**
	 * Writes a layout through the real database storage.
	 *
	 * @param layoutCodec the codec to write with.
	 * @param layoutIdentifier addresses the layout.
	 * @param layout what to write.
	 */
	private static void writeLayout(
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
	 * {@return a layout read back through the real database storage.}
	 *
	 * @param layoutCodec the codec to read with.
	 * @param layoutIdentifier addresses the layout.
	 */
	private static PersistableLayout readLayout(
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
