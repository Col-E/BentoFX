package software.coley.bentofx.persistence.impl.codec.common.mapper.dto;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mappable Data Transfer Object representing the state of a BentoFX layout.
 *
 * @author Phil Bryant
 */
public class DockingLayoutDto {

	/**
	 * The schema version this framework writes.
	 *
	 * <p>Bump this only for a change that a reader of the previous version
	 * cannot cope with. {@code BentoStateMapper.validateSupportedMetadata}
	 * accepts every version from 1 up to this one, so a layout written by an
	 * older framework still restores; the reverse does not hold, and refusing to
	 * read a newer layout is what the version is for.</p>
	 */
	private static final int CURRENT_SCHEMA_VERSION = 1;

	/**
	 * Creates an empty docking layout DTO for a mapper to populate.
	 */
	public DockingLayoutDto() {
		// This empty constructor exists merely to support Javadoc and its
		// recommended practice for providing the comment for the default
		// constructor.
	}

	/**
	 * The layout's metadata.
	 */
	public @Nullable LayoutMetadataDto metadata;

	/**
	 * The Bento states this layout holds, in registration order.
	 */
	public final List<BentoStateDto> bentoStates =
			new ArrayList<>();

	/**
	 * {@return the schema version this framework writes}
	 */
	public static int getCurrentSchemaVersion() {
		return CURRENT_SCHEMA_VERSION;
	}
}
