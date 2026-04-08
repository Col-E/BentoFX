/**
 * This module is a basic JavaFX application demonstrating use of the
 * BentoFX docking framework.
 *
 * @author Phil Bryant
 */
module bento.fx.demo.basic {

    requires bento.fx;

    requires javafx.controls;

    requires static jakarta.annotation;

    // This must be exported for the JavaFX launcher to access the
    // application classes in them.
    exports software.coley.boxfx.demo.basic;
}
