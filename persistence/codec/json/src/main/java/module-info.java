import org.jspecify.annotations.NullMarked;
import software.coley.bentofx.persistence.core.api.provider.LayoutCodecProvider;
import software.coley.bentofx.persistence.impl.codec.json.provider.JsonLayoutCodecProvider;

/**
 * This module implements the Application Programming Interface (API) for
 * encoding and decoding the layout of BentoFX docking framework components
 * using JavaScript Object Notation (JSON).
 *
 * @author Phil Bryant
 */
@NullMarked
module bento.fx.persistence.codec.json {

	requires transitive bento.fx.persistence.core;

	requires bento.fx.persistence.codec.common;

	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.databind;

	requires static org.jspecify;

	exports software.coley.bentofx.persistence.impl.codec.json;
	exports software.coley.bentofx.persistence.impl.codec.json.provider;

	provides LayoutCodecProvider with JsonLayoutCodecProvider;
}
