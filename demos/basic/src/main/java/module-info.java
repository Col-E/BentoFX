import org.jspecify.annotations.NullMarked;

/**
 * This module is a basic JavaFX application demonstrating use of the
 * BentoFX docking framework.
 *
 * @author Phil Bryant
 */
@NullMarked
module bento.fx.demo.basic {

    requires bento.fx;

    requires javafx.controls;

    requires static org.jspecify;

    // This must be exported for the JavaFX launcher to access the
    // application classes in them.
    exports demo;
}
