package software.coley.bentofx;

import jakarta.annotation.Nonnull;

/**
 * Outline of an object with access to its originating {@link Bento} instance.
 *
 * @author Matt Coley
 */
public interface BentoBacked {
	/**
	 * @return Bento instance responsible for this object.
	 */
	@Nonnull
	Bento getBento();
}