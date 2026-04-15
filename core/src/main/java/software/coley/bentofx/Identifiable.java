package software.coley.bentofx;

/**
 * Outline of an <i>(ideally uniquely)</i> identifiable object.
 *
 * @author Matt Coley
 */
public interface Identifiable {
	/**
	 * @return This objects identifier.
	 */
	String getIdentifier();

	/**
	 * @param other
	 * 		Another identifiable object.
	 *
	 * @return {@code true} when the other object has the same identifier.
	 */
	boolean matchesIdentity(Identifiable other);
}
