module bento.fx {
	requires static jakarta.annotation;

	requires javafx.base;
	requires javafx.graphics;
	requires javafx.controls;
	requires java.desktop;

	// Just open/export everything. Do whatever you want.
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
	opens software.coley.bentofx;
	opens software.coley.bentofx.building;
	opens software.coley.bentofx.control;
	opens software.coley.bentofx.control.canvas;
	opens software.coley.bentofx.dockable;
	opens software.coley.bentofx.event;
	opens software.coley.bentofx.layout;
	opens software.coley.bentofx.layout.container;
	opens software.coley.bentofx.path;
	opens software.coley.bentofx.search;
	opens software.coley.bentofx.util;
}