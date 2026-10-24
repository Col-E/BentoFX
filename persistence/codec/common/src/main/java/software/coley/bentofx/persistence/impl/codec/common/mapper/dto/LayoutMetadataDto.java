package software.coley.bentofx.persistence.impl.codec.common.mapper.dto;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Metadata about a persisted BentoFX docking layout.
 *
 * @author Phil Bryant
 */
public class LayoutMetadataDto {

	/**
	 * Creates an empty layout metadata DTO for a mapper to populate.
	 */
	public LayoutMetadataDto() {
		// This empty constructor exists merely to support Javadoc and its
		// recommended practice for providing the comment for the default
		// constructor.
	}

	/**
	 * Version of the persisted layout schema.
	 */
	public @Nullable Integer schemaVersion;

	/**
	 * The layout's human-readable name, or {@code null} when it was saved
	 * without one.
	 */
	public @Nullable String displayName;

	/**
	 * The group the layout belongs to, or {@code null} when it belongs to none.
	 */
	public @Nullable String group;

	/**
	 * The groups that exist, populated only on the reserved group catalog and
	 * empty on every other layout.
	 */
	public List<String> groups = new ArrayList<>();
}
