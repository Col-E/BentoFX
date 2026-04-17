import org.jspecify.annotations.NullMarked;

/**
 * A docking system for JavaFX.
 * <p>
 * Everything is open/exported. Do whatever you want.
 * </p>
 */
@NullMarked
open module bento.fx {
	requires static org.jspecify;

	requires javafx.base;
	requires javafx.graphics;
	requires javafx.controls;
	requires java.desktop;

	exports software.coley.bentofx;
	exports software.coley.bentofx.building;
	exports software.coley.bentofx.control;
	exports software.coley.bentofx.control.canvas;
	exports software.coley.bentofx.dockable;
	exports software.coley.bentofx.event;
	exports software.coley.bentofx.layout;
	exports software.coley.bentofx.layout.container;
	exports software.coley.bentofx.path;
	exports software.coley.bentofx.search;
	exports software.coley.bentofx.util;
}
