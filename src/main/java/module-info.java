module bento.fx {
	requires jakarta.annotation;

	requires javafx.base;
	requires javafx.graphics;
	requires javafx.controls;

	// Just open everything. Do whatever you want.
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