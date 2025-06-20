package software.coley.bentofx.event;

import jakarta.annotation.Nonnull;

/**
 * Listener invoked by the firing of any {@link DockEvent}.
 *
 * @author Matt Coley
 */
public interface DockEventListener {
	/**
	 * @param event
	 * 		Event fired.
	 */
	void onDockEvent(@Nonnull DockEvent event);
}