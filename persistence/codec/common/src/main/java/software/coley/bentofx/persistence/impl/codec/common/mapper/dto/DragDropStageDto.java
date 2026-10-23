package software.coley.bentofx.persistence.impl.codec.common.mapper.dto;

import javafx.stage.Modality;
import org.jspecify.annotations.Nullable;

/**
 * Mappable Data Transfer Object representing the layout state of a
 * {@code DragDropStage}.
 *
 * @author Phil Bryant
 */
public class DragDropStageDto {

	/**
	 * Creates an empty drag-drop stage DTO for a mapper to populate.
	 */
	public DragDropStageDto() {
		// This empty constructor exists merely to support Javadoc and its
		// recommended practice for providing the comment for the default
		// constructor.
	}

	/**
	 * The stage's title, or {@code null} when it was saved without one.
	 */
	public @Nullable String title;

	/**
	 * The stage's x coordinate.
	 */
	public @Nullable Double x;

	/**
	 * The stage's y coordinate.
	 */
	public @Nullable Double y;

	/**
	 * The stage's width.
	 */
	public @Nullable Double width;

	/**
	 * The stage's height.
	 */
	public @Nullable Double height;

	/**
	 * The stage's modality.
	 */
	public @Nullable Modality modality;

	/**
	 * The stage's opacity.
	 */
	public @Nullable Double opacity;

	/**
	 * {@code true} when the stage was iconified.
	 */
	public @Nullable Boolean iconified;

	/**
	 * {@code true} when the stage was in full-screen.
	 */
	public @Nullable Boolean fullScreen;

	/**
	 * {@code true} when the stage was maximized.
	 */
	public @Nullable Boolean maximized;

	/**
	 * {@code true} when the stage was always on top.
	 */
	public @Nullable Boolean alwaysOnTop;

	/**
	 * {@code true} when the stage was resizable.
	 */
	public @Nullable Boolean resizable;

	/**
	 * {@code true} when the stage was showing.
	 */
	public @Nullable Boolean showing;

	/**
	 * {@code true} when the stage was focused.
	 */
	public @Nullable Boolean focused;

	/**
	 * {@code true} when the stage closes itself once empty.
	 */
	public @Nullable Boolean autoCloseWhenEmpty;

	/**
	 * The root branch shown in this stage, or {@code null} when it held none.
	 */
	public @Nullable DockContainerRootBranchDto dockContainerRootBranchDto;
}
