package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.storage.LayoutIdentifierProblem;

/**
 * Callbacks a holder of {@link LayoutGroupOperations} supplies for the
 * confirmation, error-reporting, and post-operation hooks.
 *
 * <p>Package-private because it is not part of the public API.</p>
 *
 * @author Phil Bryant
 */
interface LayoutGroupOperationsContext {

	/**
	 * {@return {@code true} when the user agreed; otherwise, {@code false}.}
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

	/**
	 * {@return why a name a user typed cannot address a layout.}
	 *
	 * @param problem which rule the derived identifier broke.
	 */
	String problemText(LayoutIdentifierProblem problem);

	/**
	 * Rebuilds the menu from current storage.
	 */
	void populate();

	/**
	 * Keeps the active-layout state in step with a naming change just written.
	 *
	 * @param renamedLayout the layout as it is now named in storage.
	 */
	void followNamingChange(LayoutPersistenceProfile renamedLayout);
}
