package software.coley.bentofx.persistence.impl.storage.db;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for uniquely identifying docking layouts.
 *
 * @author Phil Bryant
 */
@Embeddable
public class DockingLayoutEntityCompositeKey implements Serializable {

	/**
	 * The width of both identifier columns, taken from the rule every storage
	 * implementation shares.
	 *
	 * <p>{@link LayoutIdentifiers#MAX_JOINED_LENGTH} is what the two identifiers may
	 * take <em>together</em>, for the reason given where it is declared. A column
	 * that wide therefore holds anything either of them can validly be, and the
	 * number lives in one place rather than being repeated here.</p>
	 */
	public static final int MAX_COMPOSITE_KEY_LENGTH = LayoutIdentifiers.MAX_JOINED_LENGTH;

	/**
	 * The name of the column containing the unique identifier for each layout.
	 */
	public static final String LAYOUT_ID_COLUMN_NAME = "layout_id";

	/**
	 * The name of the column containing the unique identifier for each layout
	 * codec.
	 */
	public static final String CODEC_ID_COLUMN_NAME = "codec_id";

	/**
	 * The name of the unique identifier for a layout.
	 */
	@Column(name = LAYOUT_ID_COLUMN_NAME, nullable = false, length = MAX_COMPOSITE_KEY_LENGTH)
	public @Nullable String layoutIdentifier;

	/**
	 * The name of the unique identifier for a layout's codec.
	 */
	@Column(name = CODEC_ID_COLUMN_NAME, nullable = false, length = MAX_COMPOSITE_KEY_LENGTH)
	public @Nullable String codecIdentifier;

	/**
	 * Creates an uninitialized {@code DockingLayoutEntityCompositeKey}.
	 */
	public DockingLayoutEntityCompositeKey() {}

	/**
	 * Creates a {@code DockingLayoutEntityCompositeKey}, with the layout and
	 * codec identifiers initialized.
	 * @param layoutIdentifier the name of the unique identifier for the layout.
	 * @param codecIdentifier the name of the unique identifier for the layout's
	 * codec.
	 */
	public DockingLayoutEntityCompositeKey(
			final String layoutIdentifier,
			final String codecIdentifier
	) {
		this.layoutIdentifier = Objects.requireNonNull(layoutIdentifier, "layoutIdentifier");
		this.codecIdentifier = Objects.requireNonNull(codecIdentifier, "codecIdentifier");
	}

	@Override
	public boolean equals(final @Nullable Object that) {
		if (this == that) {
			return true;
		}

		if (!(that instanceof final DockingLayoutEntityCompositeKey thatKey)) {
			return false;
		}

		return Objects.equals(layoutIdentifier, thatKey.layoutIdentifier)
				&& Objects.equals(codecIdentifier, thatKey.codecIdentifier);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				layoutIdentifier,
				codecIdentifier
		);
	}
}
