package software.coley.bentofx.persistence.impl.codec.common.mapper.dto;

import org.jspecify.annotations.Nullable;

/**
 * Mappable Data Transfer Object representing the layout state of a
 * {@code DockContainer}.
 *
 * @author Phil Bryant
 */
public abstract sealed class DockContainerDto
		permits DockContainerBranchDto, DockContainerLeafDto {

	/**
	 * Creates an empty container DTO for a subclass to populate.
	 */
	protected DockContainerDto() { }

	/**
	 * The container's identifier.
	 */
	public @Nullable String identifier;

	/**
	 * {@code true} when the container prunes itself from its parent once empty.
	 */
	public @Nullable Boolean pruneWhenEmpty;
}
