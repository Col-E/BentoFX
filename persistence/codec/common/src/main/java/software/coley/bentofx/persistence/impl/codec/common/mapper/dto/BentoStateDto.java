package software.coley.bentofx.persistence.impl.codec.common.mapper.dto;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mappable Data Transfer Object representing the state of a BentoFX layout.
 *
 * @author Phil Bryant
 */
public class BentoStateDto {

	/**
	 * Creates an empty Bento state DTO for a mapper to populate.
	 */
	public BentoStateDto() {
		// This empty constructor exists merely to support Javadoc and its
		// recommended practice for providing the comment for the default
		// constructor.
	}

	/**
	 * The Bento instance's identifier.
	 */
	public @Nullable String identifier;

	/**
	 * The root branches this Bento instance holds, in registration order.
	 */
	public final List<DockContainerRootBranchDto> rootBranches =
			new ArrayList<>();

	/**
	 * The drag-drop stages this Bento instance holds, in registration order.
	 */
	public final List<DragDropStageDto> dragDropStages =
			new ArrayList<>();
}
