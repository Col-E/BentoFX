package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import java.util.Objects;

/**
 * The four collaborators that drive the behavior behind
 * {@code LayoutsMenu}, grouped into one object. Holds no logic of its own; a
 * class whose only job is to read and write stored layouts, groups, the
 * active layout, and the default-arrangement state keeps those four pieces in
 * one place rather than threading each one through a long parameter list.
 *
 * <p>Package-private: an application constructs a {@code LayoutsMenu}, which
 * builds and owns its controller; nothing outside this package reaches it.</p>
 *
 * @author Phil Bryant
 */
record LayoutsController(
		LayoutGroupOperations groupOperations,
		ActiveLayoutState activeLayoutState,
		DefaultArrangementState defaultArrangementState,
		LayoutOperations layoutOperations
) {
	/**
	 * Builds a controller from four already-constructed collaborators. The
	 * collaborators have references between themselves ({@link LayoutOperations}
	 * reads {@link LayoutGroupOperations}, {@link ActiveLayoutState}, and
	 * {@link DefaultArrangementState}), so the caller is responsible for
	 * building them in the right order.
	 *
	 * @param groupOperations
	 * 		handles group CRUD and catalog operations.
	 * @param activeLayoutState
	 * 		tracks which named layout is currently active.
	 * @param defaultArrangementState
	 * 		tracks whether the default layout is
	 * 		currently showing.
	 * @param layoutOperations
	 * 		handles layout CRUD: save, restore, rename,
	 * 		delete, and write.
	 */
	LayoutsController(
			final LayoutGroupOperations groupOperations,
			final ActiveLayoutState activeLayoutState,
			final DefaultArrangementState defaultArrangementState,
			final LayoutOperations layoutOperations
	) {
		this.groupOperations =
				Objects.requireNonNull(groupOperations, "groupOperations");
		this.activeLayoutState =
				Objects.requireNonNull(activeLayoutState, "activeLayoutState");
		this.defaultArrangementState =
				Objects.requireNonNull(defaultArrangementState, "defaultArrangementState");
		this.layoutOperations =
				Objects.requireNonNull(layoutOperations, "layoutOperations");
	}

	/** {@return the group CRUD and catalog operations.} */
	@Override
	public LayoutGroupOperations groupOperations() {
		return groupOperations;
	}

	/** {@return the tracker for which named layout is currently active.} */
	@Override
	public ActiveLayoutState activeLayoutState() {
		return activeLayoutState;
	}

	/** {@return the tracker for whether the default layout is currently showing.} */
	@Override
	public DefaultArrangementState defaultArrangementState() {
		return defaultArrangementState;
	}

	/** {@return the layout CRUD operations: save, restore, rename, delete, and write.} */
	@Override
	public LayoutOperations layoutOperations() {
		return layoutOperations;
	}
}
