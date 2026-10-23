package software.coley.bentofx.persistence.impl.storage.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.*;
import static software.coley.bentofx.persistence.impl.storage.db.DockingLayoutEntityCompositeKey.MAX_COMPOSITE_KEY_LENGTH;

class DatabaseLayoutStorageITP {

	private static final String ENTITY_MANAGER_FACTORY_IDENTIFIER = "bentoLayout";
	private static final String JDBC_URL_PROPERTY = "jakarta.persistence.jdbc.url";
	private static final String JDBC_FILE_URL_PREFIX = "jdbc:h2:file:";
	private static final String JDBC_FILE_URL_SUFFIX = ";DB_CLOSE_DELAY=-1";
	private static final String DATABASE_FILE_NAME = "layouts";
	private static final String TEST_LAYOUT_IDENTIFIER = "test-layout";
	private static final String TEST_CODEC_IDENTIFIER = "none";
	private static final String TEST_DATA =
			"This is test data for the layout.";
	private static final String UPDATED_TEST_DATA =
			"This is updated data for the layout.";
	private static final String LAYOUT_EXISTS_AFTER_WRITE_DESCRIPTION =
			"The layout should exist after writing data.";
	private static final String LAYOUT_ID_PARAMETER = "layoutId";
	private static final String CODEC_ID_PARAMETER = "codecId";
	private static final String DELETE_TEST_LAYOUT_QUERY =
			"DELETE FROM DockingLayoutEntity d " +
					"WHERE d.key.layoutIdentifier = :" + LAYOUT_ID_PARAMETER + " " +
					"AND d.key.codecIdentifier = :" + CODEC_ID_PARAMETER;
	private static final String WINDOWS_PATH_SEPARATOR = "\\";
	private static final String URL_PATH_SEPARATOR = "/";

	@TempDir
	private static Path temporaryDirectory;

	private static EntityManagerFactory entityManagerFactory;

	private LayoutStorage storage;

	@BeforeAll
	static void setUpAll() {
		entityManagerFactory =
				Persistence.createEntityManagerFactory(
						ENTITY_MANAGER_FACTORY_IDENTIFIER,
						Map.of(JDBC_URL_PROPERTY, createJdbcUrl())
				);
	}

	@AfterAll
	static void tearDownAll() {
		if (entityManagerFactory.isOpen()) {
			entityManagerFactory.close();
		}
	}

	@BeforeEach
	void setUp() {
		storage =
				new DatabaseLayoutStorage(
						entityManagerFactory,
						TEST_LAYOUT_IDENTIFIER,
						TEST_CODEC_IDENTIFIER
				);

		deleteTestLayout();
	}

	@AfterEach
	void tearDown() {
		deleteTestLayout();
	}

	/**
	 * Runs each native query in {@link DatabaseLayoutStorage} against the real
	 * schema Hibernate generates from the entity annotations. A rename of a
	 * {@code @Table} or {@code @Column} name on {@link DockingLayoutEntity} or
	 * {@link DockingLayoutEntityCompositeKey} that is not matched by a rename
	 * in the hardcoded query strings fails here rather than silently in
	 * production.
	 */
	@Test
	void nativeQueriesParseAgainstTheRealSchema() {
		try (final EntityManager em = entityManagerFactory.createEntityManager()) {
			assertThatCode(() ->
					em.createNativeQuery(DatabaseLayoutStorage.PAYLOAD_LENGTH_QUERY)
							.setParameter(1, TEST_LAYOUT_IDENTIFIER)
							.setParameter(2, TEST_CODEC_IDENTIFIER)
							.getResultList()
			)
					.describedAs("PAYLOAD_LENGTH_QUERY against the real schema")
					.doesNotThrowAnyException();

			assertThatCode(() ->
					em.createNativeQuery(DatabaseLayoutStorage.LAYOUT_IDENTIFIERS_QUERY)
							.setParameter(1, TEST_CODEC_IDENTIFIER)
							.getResultList()
			)
					.describedAs("LAYOUT_IDENTIFIERS_QUERY against the real schema")
					.doesNotThrowAnyException();

			em.getTransaction().begin();
			try {
				assertThatCode(() ->
						em.createNativeQuery(DatabaseLayoutStorage.DELETE_LAYOUT_QUERY)
								.setParameter(1, TEST_LAYOUT_IDENTIFIER)
								.setParameter(2, TEST_CODEC_IDENTIFIER)
								.executeUpdate()
				)
						.describedAs("DELETE_LAYOUT_QUERY against the real schema")
						.doesNotThrowAnyException();
			} finally {
				em.getTransaction().rollback();
			}
		}
	}

	@Test
	void testDatabaseLayoutStorageInitialNonExistence() {
		assertThat(storage.exists())
				.describedAs("The layout should not exist initially.")
				.isFalse();
	}

	@Test
	void testWriteAndReadData() throws IOException {
		writeData(TEST_DATA);

		assertThat(storage.exists())
				.describedAs(LAYOUT_EXISTS_AFTER_WRITE_DESCRIPTION)
				.isTrue();

		assertThat(readData())
				.describedAs("Read data should match the written data.")
				.isEqualTo(TEST_DATA);
	}

	@Test
	void testOverwriteData() throws IOException {
		writeData(TEST_DATA);

		assertThat(storage.exists())
				.describedAs(LAYOUT_EXISTS_AFTER_WRITE_DESCRIPTION)
				.isTrue();

		writeData(UPDATED_TEST_DATA);

		assertThat(readData())
				.describedAs("Read data should match the updated data.")
				.isEqualTo(UPDATED_TEST_DATA);
	}

	@Test
	void storesDescriptiveIdentifiers() throws IOException {

		final String partOne = "a-descriptive-layout-identifier-well-past-twenty-four";
		final String partDeux = "proto";

		assertThat(partOne.length())
				.describedAs("first portion of the composite key length")
				.isLessThanOrEqualTo(MAX_COMPOSITE_KEY_LENGTH);

		assertThat(partDeux.length())
				.describedAs("second portion of the composite key length")
				.isLessThanOrEqualTo(MAX_COMPOSITE_KEY_LENGTH);

		final LayoutStorage longIdentifierStorage = new DatabaseLayoutStorage(
				entityManagerFactory,
				partOne,
				partDeux
		);

		try (OutputStream outputStream = longIdentifierStorage.openOutputStream()) {
			outputStream.write(TEST_DATA.getBytes(UTF_8));
		}

		assertThat(longIdentifierStorage.exists())
				.describedAs(LAYOUT_EXISTS_AFTER_WRITE_DESCRIPTION)
				.isTrue();

		try (InputStream inputStream = longIdentifierStorage.openInputStream()) {
			assertThat(new String(inputStream.readAllBytes(), UTF_8))
					.describedAs("Read data should match the written data.")
					.isEqualTo(TEST_DATA);
		}
	}

	@Test
	void readingALayoutThatIsNotStoredThrowsIOException() {
		assertThatThrownBy(storage::openInputStream)
				.describedAs("reading a layout with no row")
				.isInstanceOf(IOException.class)
				.hasMessageContaining(TEST_LAYOUT_IDENTIFIER)
				.hasMessageContaining(TEST_CODEC_IDENTIFIER);
	}

	@Test
	void closingOneStorageLeavesTheSharedFactoryOpen() throws IOException {
		writeData(TEST_DATA);

		// A second storage on the same factory, closed the way the component
		// that owns it would close it.
		try (LayoutStorage ownStorage =
					 new DatabaseLayoutStorage(
							 entityManagerFactory,
							 TEST_LAYOUT_IDENTIFIER,
							 TEST_CODEC_IDENTIFIER
					 )) {
			assertThat(ownStorage.exists())
					.describedAs(LAYOUT_EXISTS_AFTER_WRITE_DESCRIPTION)
					.isTrue();
		}

		assertThat(entityManagerFactory.isOpen())
				.describedAs("the factory after a storage that used it was closed")
				.isTrue();

		assertThat(readData())
				.describedAs("data read through a storage sharing that factory")
				.isEqualTo(TEST_DATA);
	}

	@Test
	void closingTheOutputStreamTwicePersistsOnlyOnce() throws IOException {
		final OutputStream outputStream = storage.openOutputStream();
		outputStream.write(TEST_DATA.getBytes(UTF_8));

		outputStream.close();
		outputStream.close();

		assertThat(readData())
				.describedAs("data after closing the output stream twice")
				.isEqualTo(TEST_DATA);
	}

	private static String createJdbcUrl() {
		return JDBC_FILE_URL_PREFIX + temporaryDirectory
				.resolve(DATABASE_FILE_NAME)
				.toAbsolutePath()
				.normalize()
				.toString()
				.replace(WINDOWS_PATH_SEPARATOR, URL_PATH_SEPARATOR) +
				JDBC_FILE_URL_SUFFIX;
	}

	private void writeData(final String data) throws IOException {
		try (OutputStream outputStream = storage.openOutputStream()) {
			outputStream.write(data.getBytes(UTF_8));
		}
	}

	private String readData() throws IOException {
		try (InputStream inputStream = storage.openInputStream();
			 BufferedReader reader =
					 new BufferedReader(
							 new InputStreamReader(
									 inputStream,
									 UTF_8
							 )
					 )) {
			final StringBuilder data = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				data.append(line);
			}
			return data.toString();
		}
	}

	private void deleteTestLayout() {
		try (EntityManager entityManager =
					 entityManagerFactory.createEntityManager()) {
			entityManager.getTransaction().begin();

			entityManager.createQuery(DELETE_TEST_LAYOUT_QUERY)
					.setParameter(LAYOUT_ID_PARAMETER, TEST_LAYOUT_IDENTIFIER)
					.setParameter(CODEC_ID_PARAMETER, TEST_CODEC_IDENTIFIER)
					.executeUpdate();

			entityManager.getTransaction().commit();
		}
	}
}
