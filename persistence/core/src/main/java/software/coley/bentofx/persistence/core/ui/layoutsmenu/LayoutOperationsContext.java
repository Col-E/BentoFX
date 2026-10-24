package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import org.jspecify.annotations.Nullable;

/**
 * Callbacks a holder of {@link LayoutOperations} supplies for the
 * confirmation and error-reporting hooks that class cannot own itself.
 *
 * <p>Package-private because this is a seam for the refactor and is not part
 * of the public API.</p>
 *
 * @author Phil Bryant
 */
interface LayoutOperationsContext {

	/**
	 * {@return {@code true} when the user agreed; otherwise, {@code false}.}
	 *
	 * <p>Defaults to no, so that dismissing the dialog does not carry out
	 * something that cannot be undone.</p>
	 *
	 * @param header what is being asked.
	 * @param content what it means.
	 */
	boolean confirmLayoutAction(String header, String content);

	/**
	 * Tells the user that something about a layout could not be done.
	 *
	 * @param header what could not be done.
	 * @param content why, in whatever detail is available.
	 */
	void showLayoutError(String header, @Nullable String content);
}
