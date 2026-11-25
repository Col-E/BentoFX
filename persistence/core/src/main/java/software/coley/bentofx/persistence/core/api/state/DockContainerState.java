package software.coley.bentofx.persistence.core.api.state;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the layout state of a {@code DockContainer}.
 *
 * <p>Abstract and sealed to the two classes a {@code DockContainer} can
 * actually take, mirroring {@code DockContainer} itself</p>
 *
 * @author Phil Bryant
 */
public abstract sealed class DockContainerState
		extends IdentifiableState
		permits DockContainerBranchState, DockContainerLeafState {

	private final List<DockableState> childDockableStates;
	@Nullable
	private final Boolean pruneWhenEmpty;

	/**
	 * Constructor.
	 * @param identifier the {@code DockContainer} identifier.
	 * @param pruneWhenEmpty whether the container should be pruned when it
	 * holds nothing, {@code null} leaves it unspecified.
	 * @param childDockableStates the {@link DockableState}s for the
	 * {@code Dockable}s the container holds directly.
	 */
	protected DockContainerState(
			final String identifier,
			final @Nullable Boolean pruneWhenEmpty,
			final List<DockableState> childDockableStates
	) {
		super(identifier);
		this.childDockableStates = List.copyOf(childDockableStates);
		this.pruneWhenEmpty = pruneWhenEmpty;
	}

	/**
	 * {@return the child dockable states.}
	 */
	public List<DockableState> getChildDockableStates() {
		return childDockableStates;
	}

	/**
	 * {@return an {@code Optional} specifying whether the dock container should
	 * be pruned when empty, an empty {@code Optional} when unspecified.}
	 */
	public Optional<Boolean> doPruneWhenEmpty() {
		return Optional.ofNullable(pruneWhenEmpty);
	}

	/**
	 * Extends {@link IdentifiableState#equals(Object)} with the child dockable
	 * states and the prune-when-empty flag. See that method for the contract.
	 *
	 * @param o the object to compare against, may be {@code null}.
	 *
	 * @return {@code true} when {@code o} has exactly this runtime type and
	 * equal values for every persisted field.
	 */
	@Override
	public boolean equals(final @Nullable Object o) {
		if (this == o) {
			return true;
		}

		// The instanceof narrows the type for the compiler; super.equals
		// settles the exact-runtime-type check documented on
		// IdentifiableState.equals.
		if (!(o instanceof final DockContainerState that) || !super.equals(o)) {
			return false;
		}

		return childDockableStates.equals(that.childDockableStates)
				&& Objects.equals(pruneWhenEmpty, that.pruneWhenEmpty);
	}

	/**
	 * {@return a hash code consistent with {@link #equals(Object)}.}
	 */
	@Override
	public int hashCode() {
		return Objects.hash(
				super.hashCode(),
				childDockableStates,
				pruneWhenEmpty
		);
	}
}
