package software.coley.bentofx.persistence.impl.storage.file.provider;

import software.coley.bentofx.persistence.core.api.provider.LayoutStorageProvider;
import software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorageLocations;
import software.coley.bentofx.persistence.impl.storage.file.FileLayoutStorage;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of the {@link LayoutStorageProvider} interface for persisting
 * Bento layouts to a file.
 *
 * <p>Where those files are kept is controlled by
 * {@link LayoutStorageLocations#HOME_DIRECTORY_PROPERTY} and
 * {@link LayoutStorageLocations#NAMESPACE_PROPERTY}.</p>
 *
 * <p><b>On-disk layout.</b> {@code FileLayoutStorageProvider} keeps layouts
 * under {@code <home>/.bentofx[/<namespace>]/layouts/}, named
 * {@code <layoutIdentifier>.<codecIdentifier>} - the codec identifier is the
 * file extension, not a directory level. Layouts written by different codecs
 * sit side by side in the same directory; a layout written by the JSON codec
 * is at {@code <layoutDir>/<layoutIdentifier>.json} and remains there when
 * the application switches to another codec, present on disk but invisible
 * through {@code getLayoutIdentifiers("xml")}. If an application needs to
 * carry layouts across a codec change, migrate them explicitly at the switch
 * point: enumerate under the old codec, re-save under the new one, delete
 * the originals.</p>
 *
 * <p>See {@link LayoutStorageLocations} for how {@code <home>} and
 * {@code <namespace>} are resolved - the {@code <namespace>} subdirectory is
 * the mechanism for keeping multiple BentoFX-based applications on the same
 * machine from sharing one layouts directory. Groups are layout-level
 * metadata recorded in the
 * {@link software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers#GROUP_CATALOG_LAYOUT_IDENTIFIER}
 * catalog layout, not in the file path.</p>
 *
 * @author Phil Bryant
 */
public class FileLayoutStorageProvider implements LayoutStorageProvider {

	private static final String IDENTIFIER = "file";

	/**
	 * The directory layout files are kept in, below the BentoFX directory in the
	 * user's home.
	 *
	 * <p>Named rather than spelled into the path, so that the one place it is decided
	 * is the one place it is read. The BentoFX directory above it may hold whatever
	 * else an installation puts there, which is why layouts get a directory of their
	 * own instead of sitting loose beside it.</p>
	 */
	private static final String LAYOUTS_DIRECTORY_NAME = "layouts";

	/**
	 * Creates a {@code FileLayoutStorageProvider} for persisting Bento layouts
	 * to a file
	 */
	public FileLayoutStorageProvider() {
		// This empty constructor exists merely to support Javadoc and its
		// recommended practice for providing the comment for the default
		// constructor.
	}

	@Override
	public String getIdentifier() {
		return IDENTIFIER;
	}

	@Override
	public boolean isDefault() {
		return true;
	}

	@Override
	public LayoutStorage getLayoutStorage(
			final String layoutIdentifier,
			final String codecIdentifier
	) {
		LayoutIdentifiers.requireValid(layoutIdentifier, codecIdentifier);

		return new FileLayoutStorage(
				getLayoutFile(
						getLayoutDirectory(),
						getFileName(layoutIdentifier, codecIdentifier)
				)
		);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>One file per layout means the layouts stored are the directory's
	 * entries that end in the codec's extension. An empty file is skipped, for
	 * the same reason {@code FileLayoutStorage.exists()} skips it: there is
	 * nothing in it to restore.</p>
	 */
	@Override
	public List<String> getLayoutIdentifiers(final String codecIdentifier) {
		requireNonNull(codecIdentifier, "codecIdentifier");

		final Path directory = getLayoutDirectory();

		if (!Files.isDirectory(directory)) {
			return List.of();
		}

		final String extension = "." + getNormalizedFileExtension(codecIdentifier);

		try (final Stream<Path> entries = Files.list(directory)) {
			return entries
					.map(Path::toFile)
					.filter(file -> file.isFile() && file.length() > 0)
					.map(File::getName)
					.filter(name -> name.endsWith(extension)
							&& name.length() > extension.length())
					.map(name -> name.substring(0, name.length() - extension.length()))
					.toList();
		} catch (final IOException e) {
			// Unchecked, matching what LayoutStorage.exists() promises: a
			// destination that cannot be read says so rather than reporting that it
			// holds nothing.
			throw new UncheckedIOException(
					"Could not list the layouts in " + directory + ".",
					e
			);
		}
	}

	@Override
	public boolean deleteLayout(
			final String layoutIdentifier,
			final String codecIdentifier
	) {
		LayoutIdentifiers.requireValid(layoutIdentifier, codecIdentifier);

		final File layoutFile = getLayoutFile(
				getLayoutDirectory(),
				getFileName(layoutIdentifier, codecIdentifier)
		);

		try {
			return Files.deleteIfExists(layoutFile.toPath());
		} catch (final IOException e) {
			throw new UncheckedIOException(
					"Could not delete the layout file " + layoutFile + ".",
					e
			);
		}
	}

	/**
	 * {@return the absolute, normalized directory layouts are kept in.}
	 *
	 * <p>Resolved fresh from {@link LayoutStorageLocations#resolveBentoFxHome()}
	 * on every call rather than cached, so that redirecting it - which is
	 * otherwise unobservable from outside this class - takes effect
	 * immediately rather than only for whichever caller happens to run before
	 * this class is first loaded. See {@link LayoutStorageLocations} for how
	 * to point this at a different directory, or namespace it so more than
	 * one application's layouts do not share this one.</p>
	 */
	private static Path getLayoutDirectory() {
		return LayoutStorageLocations.resolveBentoFxHome()
				.resolve(LAYOUTS_DIRECTORY_NAME)
				.normalize();
	}

	/**
	 * {@return the file name the two identifiers name.}
	 *
	 * @param layoutIdentifier identifies the layout.
	 * @param codecIdentifier identifies the codec whose output is stored.
	 */
	private static String getFileName(
			final String layoutIdentifier,
			final String codecIdentifier
	) {
		return layoutIdentifier + "." + getNormalizedFileExtension(codecIdentifier);
	}

	/**
	 * {@return the codec identifier as a file extension, without a leading dot.}
	 *
	 * @param codecIdentifier identifies the codec whose output is stored, written
	 * either as {@code json} or as {@code .json}.
	 */
	private static String getNormalizedFileExtension(final String codecIdentifier) {
		return codecIdentifier.startsWith(".") ?
				codecIdentifier.substring(1) :
				codecIdentifier;
	}

	/**
	 * {@return the file the joined identifiers name inside the layout directory.}
	 *
	 * <p>Both identifiers come from the application and become one path
	 * component, so a separator or a {@code ..} segment in either would put the
	 * layout somewhere this provider did not choose.
	 * {@link LayoutIdentifiers#requireValid} has already refused those by name;
	 * this is the check that does not depend on having listed them, and it still
	 * catches what a list cannot - a drive-relative name on Windows, or one the
	 * platform rejects outright.</p>
	 *
	 * @param directory the absolute, normalized directory layouts are kept in.
	 * @param fileName the two identifiers joined by a {@code .}.
	 * @throws IllegalArgumentException if the identifiers do not name a file
	 * directly inside {@code directory}.
	 */
	private static File getLayoutFile(
			final Path directory,
			final String fileName
	) {
		final Path layoutFile;

		try {
			layoutFile = directory.resolve(fileName).normalize();
		} catch (final InvalidPathException e) {
			throw new IllegalArgumentException(
					"layoutIdentifier and codecIdentifier must name a file, but '"
							+ fileName + "' is not a usable path.",
					e
			);
		}

		if (!directory.equals(layoutFile.getParent())) {
			throw new IllegalArgumentException(
					"layoutIdentifier and codecIdentifier must name a file directly in "
							+ directory + ", but '" + fileName + "' resolves to "
							+ layoutFile + "."
			);
		}

		return layoutFile.toFile();
	}
}
