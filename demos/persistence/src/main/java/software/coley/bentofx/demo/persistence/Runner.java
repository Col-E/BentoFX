package software.coley.bentofx.demo.persistence;

import javafx.application.Application;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorageLocations;

import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * Starts the {@link BoxApp} application. Derived from the {@code Runner} class
 * in the basic demo.
 *
 * @author Matt Coley
 * @author Phil Bryant
 */
public class Runner {

	private static final String LOGGING_PROPERTIES = "logging.properties";

	/**
	 * This demo's own subdirectory of the resolved BentoFX home - see
	 * {@link LayoutStorageLocations#configureNamespace}. Set here mainly to
	 * demonstrate the mechanism; a real application would pick something
	 * that identifies itself, such as its own application ID.
	 */
	private static final String PERSISTENCE_NAMESPACE = "persistence-demo";

	// Using standard outputs when errors occur during logging initializing.
	@SuppressWarnings("java:S106")
	public static void main(String[] args) {

		// Initialize java.util.logging
		try (InputStream inputStream = BoxApp.class.getResourceAsStream(
				"/" + LOGGING_PROPERTIES
		)) {
			if (inputStream != null) {
				LogManager.getLogManager().readConfiguration(inputStream);
			} else {
				System.err.println(
						"Could not read " + LOGGING_PROPERTIES + ". Using " +
								"default Java Utility Logging configuration."
				);
			}
		} catch (Exception e) {
			// Reported the same way as the missing-resource case above, and for the
			// same reason: the logging this would otherwise go through is what just
			// failed to configure.
			System.err.println(
					"Could not read " + LOGGING_PROPERTIES + " (" + e + "). Using " +
							"default Java Utility Logging configuration."
			);
		}

		// Gives this demo's persisted layouts their own subdirectory of the
		// resolved BentoFX home, rather than the shared default every
		// unconfigured BentoFX-based application on this machine would use.
		// Must happen before DockingLayoutPersistence.provider() is first
		// called, which for BoxApp is as soon as its constructor runs - i.e.
		// before Application.launch(...) below, not from inside BoxApp
		// itself. An application can reach the same setting with no code at
		// all, through the BENTOFX_PERSISTENCE_NAMESPACE environment
		// variable - see LayoutStorageLocations.
		LayoutStorageLocations.configureNamespace(PERSISTENCE_NAMESPACE);

		// Launch the application
		Application.launch(BoxApp.class, args);
	}
}
