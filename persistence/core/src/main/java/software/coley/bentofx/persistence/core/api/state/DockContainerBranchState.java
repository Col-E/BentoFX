package software.coley.bentofx.persistence.core.api.state;

import javafx.geometry.Orientation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Represents the layout state of a {@code DockContainerBranch}.
 *
 * @author Phil Bryant
 */
public non-sealed class DockContainerBranchState extends DockContainerState {

	private final @Nullable Orientation orientation;
	private final Map<Integer, Double> dividerPositions;
	private final List<DockContainerState> childDockContainerStates;

	/**
	 * Constructor.
	 * @param identifier the {@code DockContainerBranch} identifier.
	 * @param pruneWhenEmpty whether empty branches should be pruned,
	 * {@code null} leaves it unspecified.
	 * @param childDockableStates the {@link DockableState}s for the
	 * {@code Dockable}s the branch holds directly.
	 * @param orientation the branch orientation, {@code null} leaves it
	 * unspecified.
	 * @param dividerPositions divider index to divider position, each between
	 * 0.0 and 1.0 (inclusive).
	 * @param childDockContainerStates the {@link DockContainerState}s for the
	 * branch's direct children, in order.
	 */
	protected DockContainerBranchState(
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
				childDockableStates
		);
		this.orientation = orientation;
		this.dividerPositions = Map.copyOf(dividerPositions);
		this.childDockContainerStates = List.copyOf(childDockContainerStates);
	}

	/**
	 * {@return an {@link Optional} containing the branch {@link Orientation},
	 * an empty {@link Optional} when the {@link Orientation} has not been
	 * specified.}
	 */
	public Optional<Orientation> getOrientation() {
		return Optional.ofNullable(orientation);
	}

	/**
	 * {@return an immutable {@link Map} of divider index to divider position,
	 * each position between 0.0 and 1.0 (inclusive). Empty when no divider
	 * positions were specified.}
	 */
	public Map<Integer, Double> getDividerPositions() {
		return dividerPositions;
	}

	/**
	 * {@return an immutable {@link List} of the {@link DockContainerState}s for
	 * the branch's direct children, in order. Empty when the branch has no
	 * children.}
	 */
	public List<DockContainerState> getChildDockContainerStates() {
		return childDockContainerStates;
	}

	/**
	 * Extends {@link DockContainerState#equals(Object)} with the orientation, the
	 * divider positions and the ordered child container states. See
	 * {@link IdentifiableState#equals(Object)} for the contract.
	 *
	 * <p>Child order is part of the comparison, because it is part of the layout:
	 * two branches holding the same containers left-to-right versus right-to-left
	 * are different layouts.</p>
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
		if (!(o instanceof final DockContainerBranchState that)
				|| !super.equals(o)) {
			return false;
		}

		return orientation == that.orientation
				&& dividerPositions.equals(that.dividerPositions)
				&& childDockContainerStates.equals(that.childDockContainerStates);
	}

	/**
	 * {@return a hash code consistent with {@link #equals(Object)}.}
	 */
	@Override
	public int hashCode() {
		return Objects.hash(
				super.hashCode(),
				orientation,
				dividerPositions,
				childDockContainerStates
		);
	}

	/**
	 * Builds a {@link DockContainerBranchState}.
	 */
	public static class DockContainerBranchStateBuilder {

		private final String identifier;
		private @Nullable Boolean pruneWhenEmpty;
		private @Nullable Orientation orientation;
		private final Map<Integer, Double> dividerPositions =
				new LinkedHashMap<>();
		private final List<DockContainerState> childDockContainerStates =
				new ArrayList<>();

		/**
		 * Constructor.
		 * @param identifier the {@code DockContainerBranch} identifier.
		 */
		public DockContainerBranchStateBuilder(final String identifier) {

			this.identifier = requireNonNull(identifier);
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param pruneWhenEmpty whether empty branches should be pruned,
		 * {@code null} leaves prune-when-empty unspecified.
		 */
		public DockContainerBranchStateBuilder setPruneWhenEmpty(
				final @Nullable Boolean pruneWhenEmpty
		) {
			this.pruneWhenEmpty = pruneWhenEmpty;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param orientation the branch orientation, {@code null} leaves
		 * {@link Orientation} unspecified.
		 */
		public DockContainerBranchStateBuilder setOrientation(
				final @Nullable Orientation orientation
		) {
			this.orientation = orientation;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param dividerIndex the index of the divider.
		 * @param dividerPosition the divider position, between 0.0 and 1.0 (inclusive).
		 */
		public DockContainerBranchStateBuilder addDividerPosition(
				final Integer dividerIndex,
				final Double dividerPosition
		) {
			dividerPositions.put(
					requireNonNull(dividerIndex),
					requireNonNull(dividerPosition)
			);
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param dockContainerState the {@link DockContainerState} to add.
		 */
		public DockContainerBranchStateBuilder addDockContainerState(
				final DockContainerState dockContainerState
		) {
			this.childDockContainerStates.add(requireNonNull(dockContainerState));
			return this;
		}

		/**
		 * {@return the {@link DockContainerBranchState} built from this builder.}
		 */
		public DockContainerBranchState build() {
			return new DockContainerBranchState(
					identifier,
					pruneWhenEmpty,
					List.of(),
					orientation,
					dividerPositions,
					childDockContainerStates
			);
		}
	}
}
