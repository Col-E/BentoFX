module bento.fx {
	requires static jakarta.annotation;

	requires javafx.base;
	requires javafx.graphics;
	requires javafx.controls;

	// Just open/export everything. Do whatever you want.
	exports software.coley.bentofx;
	exports software.coley.bentofx.builder;
	exports software.coley.bentofx.content;
	exports software.coley.bentofx.header;
	exports software.coley.bentofx.impl;
	exports software.coley.bentofx.impl.content;
	exports software.coley.bentofx.impl.layout;
	exports software.coley.bentofx.layout;
	exports software.coley.bentofx.path;
	exports software.coley.bentofx.util;
	opens software.coley.bentofx;
	opens software.coley.bentofx.builder;
	opens software.coley.bentofx.content;
	opens software.coley.bentofx.header;
	opens software.coley.bentofx.impl;
	opens software.coley.bentofx.impl.content;
	opens software.coley.bentofx.impl.layout;
	opens software.coley.bentofx.layout;
	opens software.coley.bentofx.path;
	opens software.coley.bentofx.util;
}