package software.coley.bentofx;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Outline of an <i>(ideally uniquely)</i> identifiable object.
 *
 * @author Matt Coley
 */
public interface Identifiable {
	/**
	 * @return This objects identifier.
	 */
	@NonNull
	String getIdentifier();

	/**
	 * @param other
	 * 		Another identifiable object.
	 *
	 * @return {@code true} when the other object has the same identifier.
	 */
	boolean matchesIdentity(@NonNull Identifiable other);
}