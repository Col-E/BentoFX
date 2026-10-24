package software.coley.bentofx.persistence.impl.codec.common.mapper.dto;

import javafx.geometry.Orientation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mappable Data Transfer Object representing the layout state of a
 * {@code DockContainer}.
 *
 * @author Phil Bryant
 */
public final class DockContainerBranchDto extends DockContainerDto {

	/**
	 * Creates an empty branch container DTO for a mapper to populate.
	 */
	public DockContainerBranchDto() {
		// This empty constructor exists merely to support Javadoc and its
		// recommended practice for providing the comment for the default
		// constructor.
	}

	/**
	 * The positions of the dividers between this branch's child containers.
	 */
	public final List<DividerPositionDto> dividerPositions =
			new ArrayList<>();

	/**
	 * The child containers, in the order the branch holds them.
	 */
	public final List<DockContainerDto> childDockContainers =
			new ArrayList<>();

	/**
	 * The branch's orientation.
	 */
	public @Nullable Orientation orientation;
}
