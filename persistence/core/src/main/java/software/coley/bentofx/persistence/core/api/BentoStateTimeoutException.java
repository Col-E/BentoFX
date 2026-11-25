package software.coley.bentofx.persistence.core.api;

/**
 * Indicates that a persistence task handed to the JavaFX application thread did
 * not run within its allotted time.
 *
 * <p>This is a distinct type rather than a plain {@link BentoStateException}
 * because the two demand opposite responses. An ordinary failure to read a
 * layout means the persisted state is unusable, so falling back to a default
 * layout is the right answer. A timeout means the opposite: the persisted state
 * might be fine but the JavaFX thread never got around to the work. Substituting
 * a default layout in that case would discard a good layout, and the next
 * automatic save would write that default over the saved copy. Restoring
 * therefore lets this type escape instead of treating it as
 * "no layout available".</p>
 *
 * @author Phil Bryant
 */
public class BentoStateTimeoutException extends BentoStateException {

	/**
	 * Constructor.
	 * @param message describes which task timed out, and after how long.
	 */
	public BentoStateTimeoutException(final String message) {
		super(message);
	}

	/**
	 * Constructor.
	 * @param message describes which task timed out, and after how long.
	 * @param throwable the underlying failure.
	 */
	public BentoStateTimeoutException(
			final String message,
			final Throwable throwable
	) {
		super(message, throwable);
	}
}
