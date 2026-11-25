package software.coley.bentofx.persistence.core.api;

/**
 * Indicates conditions that applications might want to catch when the saving
 * and restoring the layout of BentoFX docking components.
 *
 * @author Phil Bryant
 */
public class BentoStateException extends Exception {

	/**
	 * Constructor.
	 * @param message describes what could not be saved or restored, and why.
	 */
	public BentoStateException(final String message) {
		super(message);
	}

	/**
	 * Constructor.
	 * @param message describes what could not be saved or restored, and why.
	 * @param throwable the underlying failure.
	 */
	public BentoStateException(
			final String message,
			final Throwable throwable
	) {
		super(message, throwable);
	}
}
