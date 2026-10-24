import org.jspecify.annotations.NullMarked;

/**
 * This module is a basic JavaFX application demonstrating use of the BentoFX
 * docking framework, the persistence API, and sample codec and storage
 * implementations included in the persistence framework.
 *
 * @author Phil Bryant
 */
@NullMarked
module bento.fx.demo.persistence {

	requires bento.fx;
	requires bento.fx.persistence.core;

	requires java.logging;
	requires javafx.controls;
	requires org.slf4j;

	requires static org.jspecify;

	// This must be exported for the JavaFX launcher to access the
	// application classes in them.
	exports software.coley.bentofx.demo.persistence;

	// No 'uses' clause for the persistence provider: DockingLayoutPersistence
	// performs that lookup inside the persistence API module.
}
