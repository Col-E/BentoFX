package software.coley.bentofx.building;

import software.coley.bentofx.control.Header;
import software.coley.bentofx.control.HeaderPane;
import software.coley.bentofx.dockable.Dockable;

/**
 * Factory for building {@link Header} in a parent {@link HeaderPane}.
 *
 * @author Matt Coley
 */
public interface HeaderFactory {
	/**
	 * @param dockable
	 * 		Dockable to wrap.
	 * @param parentPane
	 * 		Parent header pane.
	 *
	 * @return New header.
	 */
	Header newHeader(Dockable dockable, HeaderPane parentPane);
}
