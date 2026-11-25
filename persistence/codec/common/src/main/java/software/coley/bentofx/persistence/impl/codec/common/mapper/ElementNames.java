package software.coley.bentofx.persistence.impl.codec.common.mapper;

/**
 * The element names an encoded layout uses, shared by every codec that binds these
 * DTOs so that one format's names cannot drift from another's.
 *
 * @author Phil Bryant
 */
public final class ElementNames {

	/** Element name for the root of an encoded docking layout. */
	public static final String DOCKING_LAYOUT_ROOT_ELEMENT_NAME = "dockingLayout";

	/** Element name for a layout's metadata. */
	public static final String METADATA_ELEMENT_NAME = "metadata";

	/** Element name for the schema version a layout declares. */
	public static final String SCHEMA_VERSION_ELEMENT_NAME = "schemaVersion";

	/** Element name for a layout's display name. */
	public static final String DISPLAY_NAME_ELEMENT_NAME = "displayName";

	/** Element name for the group a layout belongs to. */
	public static final String GROUP_ELEMENT_NAME = "group";

	/** Element name for the list of groups a group catalog holds. */
	public static final String GROUP_LIST_ELEMENT_NAME = "groups";

	/** Element name for a single group's name. */
	public static final String GROUP_NAME_ELEMENT_NAME = "groupName";

	/** Element name for the list of Bento states a layout holds. */
	public static final String BENTO_LIST_ELEMENT_NAME = "bentos";

	/** Element name for a single Bento state. */
	public static final String BENTO_ELEMENT_NAME = "bento";

	/** Element name for the list of root branches a Bento holds. */
	public static final String ROOT_BRANCH_LIST_ELEMENT_NAME = "rootBranches";

	/** Element name for a single root branch. */
	public static final String ROOT_BRANCH_ELEMENT_NAME = "rootBranch";

	/** Element name for the list of divider positions a container holds. */
	public static final String DIVIDER_POSITION_LIST_ELEMENT_NAME = "dividerPositions";

	/** Element name for a single divider position. */
	public static final String DIVIDER_ELEMENT_NAME = "divider";

	/** Element name for the list of branch containers a parent holds. */
	public static final String BRANCH_LIST_ELEMENT_NAME = "branches";

	/** Element name for a single branch container. */
	public static final String BRANCH_ELEMENT_NAME = "branch";

	/** Element name for a single leaf container. */
	public static final String LEAF_ELEMENT_NAME = "leaf";

	/** Element name for the list of child containers a branch holds. */
	public static final String CHILD_DOCK_CONTAINER_LIST_ELEMENT_NAME = "childDockContainers";

	/** Element name for the list of dockables a leaf container holds. */
	public static final String DOCKABLE_LIST_ELEMENT_NAME = "dockables";

	/** Element name for a single dockable. */
	public static final String DOCKABLE_ELEMENT_NAME = "dockable";

	/** Element name for the list of drag-drop stages a Bento holds. */
	public static final String DRAG_DROP_STAGE_LIST_ELEMENT_NAME = "dragDropStages";

	/** Element name for a single drag-drop stage. */
	public static final String DRAG_DROP_STAGE_ELEMENT_NAME = "dragDropStage";

	private ElementNames() {
		throw new IllegalStateException("Utility class");
	}
}
