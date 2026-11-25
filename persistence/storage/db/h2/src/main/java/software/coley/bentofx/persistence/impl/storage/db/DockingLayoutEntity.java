package software.coley.bentofx.persistence.impl.storage.db;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Represents a row in a table in a relational database for storing Bento
 * layouts.
 *
 * <p>Package-private, along with its mutable fields, so that the exported
 * package hands no one a handle on a stored layout. Hibernate reaches it
 * through the {@code opens} directive in the module descriptor.</p>
 *
 * @author Phil Bryant
 */
@Entity
@Table(name = DockingLayoutEntity.TABLE_NAME)
class DockingLayoutEntity {

	static final String TABLE_NAME = "docking_layout";

	static final String PAYLOAD_COLUMN_NAME = "payload";

	@EmbeddedId
	@Nullable DockingLayoutEntityCompositeKey key;

	@Lob
	@Column(name = PAYLOAD_COLUMN_NAME, nullable = false)
	byte[] payload = new byte[0];

	@Column(name = "updated_at", nullable = false)
	Instant updatedAt = Instant.EPOCH;
}
