package software.coley.bentofx.persistence.core.api.state;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Represents the layout state of an {@code Identifiable}.
 *
 * @author Phil Bryant
 */
public abstract class IdentifiableState {

	private final String identifier;

	/**
	 * Constructor.
	 * @param identifier the identifier distinguishing this state from other
	 * states of the same type.
	 */
	protected IdentifiableState(
			final String identifier
	) {
		this.identifier = Objects.requireNonNull(identifier);
	}

	/**
	 * {@return the identifier distinguishing this state from other states of
	 * the same type.}
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Compares this state to another by exact runtime type and by every
	 * persisted field, this class contributing the identifier.
	 *
	 * <p>Two states are equal when they would restore the same layout, which
	 * makes this the check for "did the layout actually change" - comparing a
	 * freshly captured state against the previous one, without encoding
	 * either.</p>
	 *
	 * <p><b>Exact runtime type, not {@code instanceof}.</b> Two things force it.
	 * {@link DockContainerRootBranchState} extends
	 * {@link DockContainerBranchState} and adds no field of its own, yet the
	 * two restore differently - one becomes a root branch, the other an
	 * ordinary child branch - so a fields-only comparison would call them
	 * equal. And {@link DockContainerBranchState} and
	 * {@link DockContainerLeafState} are {@code non-sealed}, so an application
	 * may subclass either and add fields of its own; comparing exact types
	 * keeps this relation symmetric and transitive against such a subclass,
	 * which {@code instanceof} cannot.</p>
	 *
	 * <p>Subclasses extend this by calling {@code super.equals(o)} first, which
	 * settles the type check for the whole chain, and then comparing their own
	 * fields.</p>
	 *
	 * <p>Callable from any thread. Every state in this package is immutable, so
	 * this reads no mutable shared data and touches no scene graph.</p>
	 *
	 * @param o the object to compare against, may be {@code null}.
	 *
	 * @return {@code true} when {@code o} has exactly this runtime type and
	 * equal values for every persisted field.
	 */
	// EqualsGetClass: getClass is the point of this method, not an oversight.
	// See the exact-runtime-type paragraph above for the two reasons instanceof
	// is wrong here, and DockContainerRootBranchStateTest for the case that
	// pins it.
	@SuppressWarnings("EqualsGetClass")
	@Override
	public boolean equals(final @Nullable Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		return identifier.equals(((IdentifiableState) o).identifier);
	}

	/**
	 * {@return a hash code consistent with {@link #equals(Object)}, this class
	 * contributing the identifier.}
	 *
	 * <p>Subclasses fold this in through {@code super.hashCode()}. Callable
	 * from any thread, for the reason given on {@link #equals(Object)}.</p>
	 */
	@Override
	public int hashCode() {
		return identifier.hashCode();
	}
}
