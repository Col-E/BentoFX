package software.coley.bentofx;

import org.jspecify.annotations.NonNull;

/**
 * Outline of an object with access to its originating {@link Bento} instance.
 *
 * @author Matt Coley
 */
public interface BentoBacked {
	/**
	 * @return Bento instance responsible for this object.
	 */
	@NonNull
	Bento getBento();
}