package software.coley.bentofx;

import org.jspecify.annotations.Nullable;

/**
 * Outline of an <i>(ideally uniquely)</i> identifiable object.
 *
 * @author Matt Coley
 */
public interface Identifiable {

	/**
	 * {@return This object's identifier.}
	 */
	String getIdentifier();

	/**
	 * {@return {@code true} when the other object has the same identifier.}
	 *
	 * @param other Another identifiable object.
	 */
	default boolean matchesIdentity(final @Nullable Identifiable other) {
		return other != null &&
				this.getIdentifier().equals(other.getIdentifier());
	}
}
