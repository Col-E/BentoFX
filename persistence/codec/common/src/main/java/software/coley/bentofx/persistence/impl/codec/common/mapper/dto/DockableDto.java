package software.coley.bentofx.persistence.impl.codec.common.mapper.dto;

import org.jspecify.annotations.Nullable;

/**
 * Mappable Data Transfer Object representing the layout state of a
 * {@code Dockable}.
 *
 * @author Phil Bryant
 */
public class DockableDto {

	/**
	 * Creates an empty dockable DTO for a mapper to populate.
	 */
	public DockableDto() {
		// This empty constructor exists merely to support Javadoc and its
		// recommended practice for providing the comment for the default
		// constructor.
	}

	/**
	 * The dockable's identifier.
	 */
	public @Nullable String identifier;

	/**
	 * The dockable's title, or {@code null} when it was saved without one.
	 */
	public @Nullable String title;

	/**
	 * The dockable's tooltip text, or {@code null} when it was saved without one.
	 */
	public @Nullable String tooltipText;

	/**
	 * The dockable's drag group mask.
	 */
	public @Nullable Integer dragGroupMask;

	/**
	 * {@code true} when the dockable is closable.
	 */
	public @Nullable Boolean isClosable;
}
