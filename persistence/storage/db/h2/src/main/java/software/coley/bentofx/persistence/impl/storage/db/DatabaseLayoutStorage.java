package software.coley.bentofx.persistence.impl.storage.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of the {@link LayoutStorage} interface for persisting Bento
 * layouts to a database using a Jakarta {@link EntityManagerFactory}.
 *
 * @author Phil Bryant
 */
public class DatabaseLayoutStorage implements LayoutStorage {

	private static final Logger logger =
			LoggerFactory.getLogger(DatabaseLayoutStorage.class);


	// Native queries, because JPQL's LENGTH does not accept a byte[] in
	// Hibernate and the point of asking for length is not reading the
	// payload. Written as plain string literals (no concatenation) so
	// SonarQube's S2077 cannot see dynamic SQL formatting. The literal
	// column names here must match the @Table / @Column names on
	// DockingLayoutEntity and DockingLayoutEntityCompositeKey; the ITP
	// tests exercise every one of them against a real database, so a
	// drift fails there rather than silently in production. A row whose
	// payload is empty is not a layout.

	/** The {@code requireNonNull} message naming the {@code codecIdentifier} parameter. */
	private static final String CODEC_IDENTIFIER_PARAMETER = "codecIdentifier";

	static final String PAYLOAD_LENGTH_QUERY =
			"select length(payload) from docking_layout"
					+ " where layout_id = ?1 and codec_id = ?2";

	static final String LAYOUT_IDENTIFIERS_QUERY =
			"select layout_id from docking_layout"
					+ " where codec_id = ?1 and length(payload) > 0";

	static final String DELETE_LAYOUT_QUERY =
			"delete from docking_layout"
					+ " where layout_id = ?1 and codec_id = ?2";

	private final EntityManagerFactory emf;
	private final String layoutIdentifier;
	private final String codecIdentifier;

	/**
	 * Constructor.
	 *
	 * @param emf the factory this storage takes its entity managers from. It
	 * remains the caller's to close.
	 * @param layoutIdentifier identifies the layout within the database.
	 * @param codecIdentifier identifies the codec whose output is stored.
	 */
	public DatabaseLayoutStorage(
			final EntityManagerFactory emf,
			final String layoutIdentifier,
			final String codecIdentifier
	) {
		this.emf = requireNonNull(emf, "emf");
		this.layoutIdentifier = requireNonNull(layoutIdentifier, "layoutIdentifier");
		this.codecIdentifier = requireNonNull(codecIdentifier, CODEC_IDENTIFIER_PARAMETER);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Asks the database for the payload's length rather than for the payload,
	 * so that answering whether a layout exists does not read the layout.</p>
	 */
	@Override
	public boolean exists() {

		try (final EntityManager em = emf.createEntityManager()) {

			final List<?> lengths =
					em.createNativeQuery(PAYLOAD_LENGTH_QUERY)
							.setParameter(1, layoutIdentifier)
							.setParameter(2, codecIdentifier)
							.getResultList();

			return !lengths.isEmpty()
					&& lengths.getFirst() instanceof final Number length
					&& length.longValue() > 0;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>A layout that is not stored arrives as an {@link IOException}.</p>
	 */
	@Override
	public InputStream openInputStream() throws IOException {

		try (final EntityManager em = emf.createEntityManager()) {

			logger.trace(
					"Creating input stream using layout {} and codec {}.",
					layoutIdentifier,
					codecIdentifier
			);

			final DockingLayoutEntity entity =
					em.find(
							DockingLayoutEntity.class,
							createKey()
					);

			// Qodana reports entity == null is always false,
			// but em.find(...) can return null.
			// noinspection ConstantValue
			if (entity == null) {
				throw new IOException(
						"No layout is stored for layout '" + layoutIdentifier
								+ "' and codec '" + codecIdentifier + "'."
				);
			}

			return new ByteArrayInputStream(entity.payload);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The returned stream holds the layout in memory and stores it when it is
	 * closed, so a caller that abandons the stream stores nothing.</p>
	 */
	@Override
	public OutputStream openOutputStream() {
		// Capture bytes, then persist on close()
		return new ByteArrayOutputStream() {
			private final AtomicBoolean isStreamClosed = new AtomicBoolean();

			@Override
			public void close() throws IOException {
				if (!isStreamClosed.compareAndSet(false, true)) {
					return;
				}

				super.close();

				final byte[] bytesToSave = toByteArray();

				final EntityManager em = emf.createEntityManager();
				final EntityTransaction tx = em.getTransaction();

				try (em) {
					tx.begin();

					final DockingLayoutEntityCompositeKey key = createKey();

					final DockingLayoutEntity existingEntity =
							em.find(
									DockingLayoutEntity.class,
									key
							);

					final DockingLayoutEntity entityToSave;

					// Qodana reports existingEntity == null is always false,
					// but em.find(...) can return null.
					// noinspection ConstantValue
					if (existingEntity == null) {
						entityToSave = new DockingLayoutEntity();
						entityToSave.key = key;
						em.persist(entityToSave);
					} else {
						entityToSave = existingEntity;
					}

					entityToSave.payload = bytesToSave;
					entityToSave.updatedAt = Instant.now();

					tx.commit();
				} catch (Exception e) {
					if (tx.isActive()) {
						tx.rollback();
					}

					throw new IOException(
							"Could not close output stream.",
							e
					);
				}
			}
		};
	}


	/**
	 * {@return the identifiers of the layouts stored for the supplied codec,
	 * in no particular order.}
	 *
	 * <p>A catalog is not a property of one layout's storage. It lives here so
	 * that the queries sit beside the mapping whose table and column names they
	 * are built from.</p>
	 *
	 * @param entityManagerFactory the factory to take an entity manager from. It remains the
	 * caller's to close.
	 * @param codecIdentifier identifies the codec whose layouts are wanted.
	 */
	public static List<String> getLayoutIdentifiers(
			final EntityManagerFactory entityManagerFactory,
			final String codecIdentifier
	) {
		requireNonNull(entityManagerFactory, "entityManagerFactory");
		requireNonNull(codecIdentifier, CODEC_IDENTIFIER_PARAMETER);

		try (final EntityManager entityManager =
					 entityManagerFactory.createEntityManager()) {

			final List<?> identifiers =
					entityManager.createNativeQuery(LAYOUT_IDENTIFIERS_QUERY)
							.setParameter(1, codecIdentifier)
							.getResultList();

			return identifiers.stream()
					.filter(String.class::isInstance)
					.map(String.class::cast)
					.toList();
		}
	}

	/**
	 * Removes the stored layout, if there is one.
	 *
	 * @param entityManagerFactory the factory to take an entity manager from.
	 * It remains the caller's to close.
	 * @param layoutIdentifier identifies the layout to remove.
	 * @param codecIdentifier identifies the codec whose output is stored.
	 * @return {@code true} when a row was removed; otherwise, {@code false}.
	 */
	public static boolean deleteLayout(
			final EntityManagerFactory entityManagerFactory,
			final String layoutIdentifier,
			final String codecIdentifier
	) {
		requireNonNull(entityManagerFactory, "entityManagerFactory");
		requireNonNull(layoutIdentifier, "layoutIdentifier");
		requireNonNull(codecIdentifier, CODEC_IDENTIFIER_PARAMETER);

		final EntityManager entityManager =
				entityManagerFactory.createEntityManager();
		final EntityTransaction transaction = entityManager.getTransaction();

		try (entityManager) {
			transaction.begin();

			final int removed =
					entityManager.createNativeQuery(DELETE_LAYOUT_QUERY)
					.setParameter(1, layoutIdentifier)
					.setParameter(2, codecIdentifier)
					.executeUpdate();

			transaction.commit();

			return removed > 0;
		} catch (final RuntimeException e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}

			throw e;
		}
	}

	/**
	 * {@return the key identifying this storage's row.}
	 */
	private DockingLayoutEntityCompositeKey createKey() {
		return new DockingLayoutEntityCompositeKey(
				layoutIdentifier,
				codecIdentifier
		);
	}
}
