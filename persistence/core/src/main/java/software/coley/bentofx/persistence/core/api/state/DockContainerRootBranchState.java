package software.coley.bentofx.persistence.core.api.state;

import javafx.geometry.Orientation;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Represents the layout state of a {@code DockContainerRootBranch}.
 *
 * @author Phil Bryant
 */
public class DockContainerRootBranchState extends DockContainerBranchState {

	// No equals or hashCode overrides, deliberately. This class adds no field
	// to DockContainerBranchState, and IdentifiableState#equals(Object) compares
	// exact runtime types, so a root branch state is already unequal to an
	// ordinary branch state carrying the same values. That distinction matters,
	// because the two restore differently.

	private DockContainerRootBranchState(
			final String identifier,
			final @Nullable Boolean pruneWhenEmpty,
			final List<DockableState> childDockableStates,
			final @Nullable Orientation orientation,
			final Map<Integer, Double> dividerPositions,
			final List<DockContainerState> childDockContainerStates
	) {
		super(
				identifier,
				pruneWhenEmpty,
				childDockableStates,
				orientation,
				dividerPositions,
				childDockContainerStates
		);
	}

	/**
	 * Builds a {@link DockContainerRootBranchState}.
	 *
	 * <p>A root branch state holds nothing a {@link DockContainerBranchState} does
	 * not, so this collects into a
	 * {@link DockContainerBranchState.DockContainerBranchStateBuilder} rather than
	 * repeating its six fields and their null checks. The mutators stay, one line
	 * each, because they have to return <em>this</em> type for chaining:
	 * inheriting them instead would hand callers a {@code build()} returning the
	 * wrong state, and making the return type generic over a self type is a steep
	 * price for two subclasses.</p>
	 */
	public static class DockContainerRootBranchStateBuilder {

		private final DockContainerBranchStateBuilder branchStateBuilder;

		/**
		 * Constructor.
		 * @param identifier the {@code DockContainerRootBranch} identifier.
		 */
		public DockContainerRootBranchStateBuilder(
				final String identifier
		) {
			this.branchStateBuilder =
					new DockContainerBranchStateBuilder(identifier);
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param pruneWhenEmpty whether empty branches should be pruned,
		 * {@code null} leaves prune-when-empty unspecified.
		 */
		public DockContainerRootBranchStateBuilder setPruneWhenEmpty(
				final @Nullable Boolean pruneWhenEmpty
		) {
			branchStateBuilder.setPruneWhenEmpty(pruneWhenEmpty);
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param orientation the branch orientation, {@code null} leaves
		 * {@link Orientation} unspecified.
		 */
		public DockContainerRootBranchStateBuilder setOrientation(
				final @Nullable Orientation orientation
		) {
			branchStateBuilder.setOrientation(orientation);
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param dividerIndex the index of the divider.
		 * @param dividerPosition the divider position, between 0.0 and 1.0
		 * (inclusive).
		 */
		public DockContainerRootBranchStateBuilder addDividerPosition(
				final Integer dividerIndex,
				final Double dividerPosition
		) {
			branchStateBuilder.addDividerPosition(dividerIndex, dividerPosition);
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param dockContainerState the {@link DockContainerState} to add.
		 */
		public DockContainerRootBranchStateBuilder addDockContainerState(
				final DockContainerState dockContainerState
		) {
			branchStateBuilder.addDockContainerState(dockContainerState);
			return this;
		}

		/**
		 * {@return the {@link DockContainerRootBranchState} built from this
		 * builder.}
		 */
		public DockContainerRootBranchState build() {
			// Building the branch state first and copying across, so the
			// collected values are read back through one set of accessors
			// rather than by reaching into the delegate's fields.
			final DockContainerBranchState branchState =
					branchStateBuilder.build();

			return new DockContainerRootBranchState(
					branchState.getIdentifier(),
					branchState.doPruneWhenEmpty().orElse(null),
					branchState.getChildDockableStates(),
					branchState.getOrientation().orElse(null),
					branchState.getDividerPositions(),
					branchState.getChildDockContainerStates()
			);
		}
	}
}
