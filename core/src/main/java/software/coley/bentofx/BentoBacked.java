package software.coley.bentofx;

/**
 * Outline of an object with access to its originating {@link Bento} instance.
 *
 * @author Matt Coley
 */
public interface BentoBacked {
	/**
	 * @return Bento instance responsible for this object.
	 */
	Bento getBento();
}
