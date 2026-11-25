package software.coley.bentofx.persistence.impl.codec.common.mapper.dto;

import javafx.geometry.Orientation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mappable Data Transfer Object representing the layout state of a
 * {@code DockContainerRootBranch}.
 *
 * @author Phil Bryant
 */
public class DockContainerRootBranchDto {

	/**
	 * Creates an empty root branch DTO for a mapper to populate.
	 */
	public DockContainerRootBranchDto() {
		// This empty constructor exists merely to support Javadoc and its
		// recommended practice for providing the comment for the default
		// constructor.
	}

	/**
	 * The root branch's identifier.
	 */
	public @Nullable String identifier;

	/**
	 * {@code true} when the root branch prunes itself once empty.
	 */
	public @Nullable Boolean pruneWhenEmpty;

	/**
	 * The root branch's orientation.
	 */
	public @Nullable Orientation orientation;

	/**
	 * The positions of the dividers between this root branch's child containers.
	 */
	public final List<DividerPositionDto> dividerPositions =
			new ArrayList<>();

	/**
	 * The child containers, in the order the root branch holds them.
	 */
	public final List<DockContainerDto> childDockContainers =
			new ArrayList<>();
}
