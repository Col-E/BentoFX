package software.coley.bentofx.event;

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
	void onDockEvent(DockEvent event);
}
