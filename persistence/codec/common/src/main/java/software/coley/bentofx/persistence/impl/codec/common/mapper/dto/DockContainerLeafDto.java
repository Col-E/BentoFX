package software.coley.bentofx.persistence.impl.codec.common.mapper.dto;

import javafx.geometry.Side;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mappable Data Transfer Object representing the layout state of a
 * {@code DockContainerLeaf}.
 *
 * @author Phil Bryant
 */
public final class DockContainerLeafDto extends DockContainerDto {

	/**
	 * Creates an empty leaf container DTO for a mapper to populate.
	 */
	public DockContainerLeafDto() {
		// This empty constructor exists merely to support Javadoc and its
		// recommended practice for providing the comment for the default
		// constructor.
	}

	/**
	 * The dockables this leaf holds, in the order they are shown.
	 */
	public final List<DockableDto> dockables =
			new ArrayList<>();

	/**
	 * The identifier of the dockable that was selected, or {@code null} when
	 * none was.
	 */
	public @Nullable String selectedDockableIdentifier;

	/**
	 * The side of the leaf its headers are shown on.
	 */
	public @Nullable Side side;

	/**
	 * {@code true} when the leaf may be resized relative to its parent.
	 */
	public @Nullable Boolean isResizableWithParent;

	/**
	 * {@code true} when the leaf may be split into further containers.
	 */
	public @Nullable Boolean isCanSplit;

	/**
	 * The size in pixels the leaf occupied while uncollapsed.
	 */
	public @Nullable Double uncollapsedSizePx;

	/**
	 * {@code true} when the leaf was collapsed.
	 */
	public @Nullable Boolean isCollapsed;
}
