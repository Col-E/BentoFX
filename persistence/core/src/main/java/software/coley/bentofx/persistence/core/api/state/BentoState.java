package software.coley.bentofx.persistence.core.api.state;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Represents the layout state of a {@code Bento}.
 *
 * @author Phil Bryant
 */
public class BentoState extends IdentifiableState {

	private final List<DockContainerRootBranchState> rootBranchStates;

	private final List<DragDropStageState> dragDropStageStates;

	private BentoState(
			final String identifier,
			final List<DockContainerRootBranchState> rootBranchStates,
			final List<DragDropStageState> dragDropStageStates
	) {

		super(identifier);
		this.rootBranchStates = List.copyOf(rootBranchStates);
		this.dragDropStageStates = List.copyOf(dragDropStageStates);
	}

	/**
	 * {@return an immutable {@link List} of the
	 * {@link DockContainerRootBranchState}s for the {@code Bento}'s root
	 * branches. Empty when none were specified.}
	 */
	public List<DockContainerRootBranchState> getRootBranchStates() {
		return rootBranchStates;
	}

	/**
	 * {@return an immutable {@link List} of the {@link DragDropStageState}s for
	 * the {@code Bento}'s drag/drop stages. Empty when none were specified.}
	 */
	public List<DragDropStageState> getDragDropStageStates() {
		return dragDropStageStates;
	}

	/**
	 * Extends {@link IdentifiableState#equals(Object)} with the root branch states
	 * and the drag/drop stage states. See that method for the contract.
	 *
	 * <p>Comparing a whole {@link BentoState} compares the layout beneath it, since
	 * the nested states carry this same contract all the way down.</p>
	 *
	 * @param o the object to compare against, may be {@code null}.
	 *
	 * @return {@code true} when {@code o} has exactly this runtime type and equal
	 * values for every persisted field.
	 */
	@Override
	public boolean equals(final @Nullable Object o) {
		if (this == o) {
			return true;
		}

		// The instanceof narrows the type for the compiler; super.equals settles the
		// exact-runtime-type check documented on IdentifiableState.equals.
		if (!(o instanceof final BentoState that) || !super.equals(o)) {
			return false;
		}

		return rootBranchStates.equals(that.rootBranchStates)
				&& dragDropStageStates.equals(that.dragDropStageStates);
	}

	/**
	 * {@return a hash code consistent with {@link #equals(Object)}.}
	 */
	@Override
	public int hashCode() {
		return Objects.hash(
				super.hashCode(),
				rootBranchStates,
				dragDropStageStates
		);
	}

	/**
	 * Builds a {@link BentoState}.
	 */
	public static class BentoStateBuilder {

		private final String identifier;

		private final List<DockContainerRootBranchState> rootBranchStates =
				new ArrayList<>();

		private final List<DragDropStageState> dragDropStageStates =
				new ArrayList<>();

		/**
		 * Constructor.
		 * @param identifier the {@code Bento} identifier.
		 */
		public BentoStateBuilder(final String identifier) {
			this.identifier = requireNonNull(identifier);
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param rootBranchState the {@link DockContainerRootBranchState} to add.
		 */
		public BentoStateBuilder addRootBranchState(
				final DockContainerRootBranchState rootBranchState
		) {
			this.rootBranchStates.add(requireNonNull(rootBranchState));
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param dragDropStageState the {@link DragDropStageState} to add.
		 */
		public BentoStateBuilder addDragDropStageState(
				final DragDropStageState dragDropStageState
		) {
			this.dragDropStageStates.add(requireNonNull(dragDropStageState));
			return this;
		}

		/**
		 * {@return the {@link BentoState} built from this builder.}
		 */
		public BentoState build() {
			return new BentoState(
					identifier,
					rootBranchStates,
					dragDropStageStates
			);
		}
	}
}
