package software.coley.bentofx.persistence.impl.storage.db.provider;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import software.coley.bentofx.persistence.core.api.provider.LayoutStorageProvider;
import software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorageLocations;
import software.coley.bentofx.persistence.impl.storage.db.DatabaseLayoutStorage;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the {@link LayoutStorageProvider} interface for persisting
 * Bento layouts to H2 databases.
 *
 * <p>Every storage for one database file shares one {@link EntityManagerFactory},
 * created when the first one is requested. A factory starts Hibernate and brings
 * a connection pool with it, while the layouts it serves are one file that a
 * single connection would satisfy; what a saver and a restorer each need of
 * their own is the {@link LayoutStorage}, which is still built per call. The
 * database file is resolved on every call, so changing the home directory
 * moves later storages to a new file (and factory) while storages already
 * handed out keep theirs.</p>
 *
 * <p>Nothing closes the factories before the JVM exits, which is also when
 * the pools and the embedded databases go. There is no earlier moment to close
 * them at: the storages are handed to components that outlive individual saves,
 * and {@link LayoutStorageProvider} has no shutdown of its own to hook into.</p>
 *
 * <p>The database file location is determined by
 * {@link LayoutStorageLocations#HOME_DIRECTORY_PROPERTY} and
 * {@link LayoutStorageLocations#NAMESPACE_PROPERTY}.</p>
 *
 * <p><b>Deployment surface.</b> Supported on the class path and on the module
 * path (JPMS). The module descriptor already declares the Jakarta Persistence,
 * Hibernate, H2 and HikariCP dependencies and opens the entity package for
 * reflection, and the integration tests run on both paths. No extra
 * configuration is required of the application.</p>
 *
 * <p><b>OSGi is out of scope.</b> This module is not an OSGi bundle: it ships
 * no bundle manifest and no {@code Meta-Persistence} header, and the ORM it
 * depends on no longer publishes an OSGi integration module. Dropping this
 * JAR into an OSGi container is not expected to work. An application that
 * targets OSGi should implement its own {@code LayoutStorageProvider} against
 * whatever persistence stack its container supports; the Extending
 * Persistence guide describes the path.</p>
 *
 * <p><b>Nested classloader environments.</b> Jakarta Persistence resolves its
 * provider via the context classloader at the time of the first call to
 * {@code createEntityManagerFactory}. This provider caches one factory per
 * database file, so an application that reconfigures its module layer at
 * runtime would need to switch to a different home directory to pick up the
 * change.</p>
 *
 * @author Phil Bryant
 */
public class DatabaseLayoutStorageProvider implements LayoutStorageProvider {

	private static final String IDENTIFIER = "h2";

	private static final String PERSISTENCE_UNIT_NAME = "bentoLayout";

	private static final String JDBC_URL_PROPERTY = "jakarta.persistence.jdbc.url";

	/**
	 * The H2 database file's basename, below the resolved BentoFX home. H2
	 * appends its own extension, so this never collides with the file-backed
	 * provider's identically-named {@code layouts} directory - a directory
	 * and a {@code layouts.mv.db} file are different filesystem entries.
	 */
	private static final String DATABASE_FILE_NAME = "layouts";

	/**
	 * One factory per database file, so storages already handed out keep the
	 * factory they were given when the home changes. Guarded by this
	 * provider's monitor.
	 */
	private final Map<Path, EntityManagerFactory> entityManagerFactories =
			new HashMap<>();

	/**
	 * Creates a {@code DatabaseLayoutStorageProvider} for persisting Bento
	 * layouts to an H2 database.
	 */
	public DatabaseLayoutStorageProvider() {
		// This empty constructor exists merely to support Javadoc and its
		// recommended practice for providing the comment for the default
		// constructor.
	}

	@Override
	public String getIdentifier() {
		return IDENTIFIER;
	}

	@Override
	public LayoutStorage getLayoutStorage(
			final String layoutIdentifier,
			final String codecIdentifier
	) {
		LayoutIdentifiers.requireValid(layoutIdentifier, codecIdentifier);

		return new DatabaseLayoutStorage(
				getEntityManagerFactory(),
				layoutIdentifier,
				codecIdentifier
		);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>One row per layout and codec, so the layouts stored are the layout
	 * identifiers on rows with a payload.</p>
	 */
	@Override
	public List<String> getLayoutIdentifiers(final String codecIdentifier) {
		return DatabaseLayoutStorage.getLayoutIdentifiers(
				getEntityManagerFactory(),
				codecIdentifier
		);
	}

	@Override
	public boolean deleteLayout(
			final String layoutIdentifier,
			final String codecIdentifier
	) {
		LayoutIdentifiers.requireValid(layoutIdentifier, codecIdentifier);

		return DatabaseLayoutStorage.deleteLayout(
				getEntityManagerFactory(),
				layoutIdentifier,
				codecIdentifier
		);
	}

	/**
	 * {@return the {@link EntityManagerFactory} for the database the current home
	 * resolves to, creating it when this is the first call to need it.}
	 *
	 * <p>Synchronized because the first caller is the one that creates it, and
	 * nothing says a saver, a restorer and a catalog lookup happen on one
	 * thread.</p>
	 *
	 * <p>The JDBC URL {@code META-INF/persistence.xml} declares is overridden
	 * here with one built from {@link LayoutStorageLocations#resolveBentoFxHome()},
	 * so the two properties it reads apply to this provider the same way they
	 * apply to the file-backed one - the XML value is what a JPA tool sees if
	 * it opens this persistence unit directly, not what this class actually
	 * uses.</p>
	 */
	private synchronized EntityManagerFactory getEntityManagerFactory() {
		// Resolved on every call, as the file-backed provider does, so a home
		// changed after this provider was first used takes effect here too.
		final Path databaseFile =
				LayoutStorageLocations.resolveBentoFxHome().resolve(DATABASE_FILE_NAME);

		return entityManagerFactories.computeIfAbsent(
				databaseFile,
				DatabaseLayoutStorageProvider::createEntityManagerFactory
		);
	}

	/**
	 * {@return a new {@link EntityManagerFactory} for the database file.}
	 *
	 * @param databaseFile the database file, without H2's extension.
	 * @throws IllegalStateException when the path holds a {@code ;}, which H2
	 * would read as the start of its URL settings. H2 cannot escape one in a
	 * file path, so such a path would either fail to open or, worse, have part
	 * of it applied as settings.
	 */
	private static EntityManagerFactory createEntityManagerFactory(
			final Path databaseFile
	) {
		final String path = databaseFile.toString();

		if (path.indexOf(';') >= 0) {
			throw new IllegalStateException(
					"The H2 layout database cannot be kept at '" + path + "': "
							+ "H2 reads ';' in a database path as the start of its "
							+ "settings. Choose a home directory without one ("
							+ LayoutStorageLocations.HOME_DIRECTORY_PROPERTY + ")."
			);
		}

		return Persistence.createEntityManagerFactory(
				PERSISTENCE_UNIT_NAME,
				Map.of(JDBC_URL_PROPERTY, "jdbc:h2:file:" + path)
		);
	}
}
