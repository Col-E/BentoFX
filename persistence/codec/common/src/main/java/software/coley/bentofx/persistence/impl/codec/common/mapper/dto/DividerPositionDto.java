package software.coley.bentofx.persistence.impl.codec.common.mapper.dto;

import org.jspecify.annotations.Nullable;

/**
 * Mappable Data Transfer Object representing the divider positions in a
 * BentoFX layout.
 *
 * @author Phil Bryant
 */
public class DividerPositionDto {

	/**
	 * Creates an empty divider position DTO for a mapper to populate.
	 */
	public DividerPositionDto() {
		// This empty constructor exists merely to support Javadoc and its
		// recommended practice for providing the comment for the default
		// constructor.
	}

	/**
	 * Index of the divider within its container's list of dividers.
	 */
	public @Nullable Integer index;

	/**
	 * The divider's position, as the fraction of the container's length that
	 * precedes it.
	 */
	public @Nullable Double position;
}
