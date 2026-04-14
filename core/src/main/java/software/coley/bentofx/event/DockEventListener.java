package software.coley.bentofx.event;

import org.jspecify.annotations.NonNull;

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
	void onDockEvent(@NonNull DockEvent event);
}