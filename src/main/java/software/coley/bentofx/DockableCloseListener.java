package software.coley.bentofx;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.header.Header;

/**
 * Listener to add to a {@link Dockable} that is invoked when it is removed from a {@link Content}
 * with the intent to {@link Header.RemovalReason#CLOSING close it}.
 *
 * @author Matt Coley
 */
public interface DockableCloseListener {
	/**
	 * @param dockable
	 * 		Closed dockable.
	 */
	void onClose(@Nonnull Dockable dockable);
}
