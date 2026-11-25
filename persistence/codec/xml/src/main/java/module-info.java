import org.jspecify.annotations.NullMarked;
import software.coley.bentofx.persistence.core.api.provider.LayoutCodecProvider;
import software.coley.bentofx.persistence.impl.codec.xml.provider.XmlLayoutCodecProvider;

/**
 * This module implements the Application Programming Interface (API) for
 * encoding and decoding the layout of BentoFX docking framework components
 * using the eXtensible Markup Language (XML).
 *
 * @author Phil Bryant
 */
@NullMarked
module bento.fx.persistence.codec.xml {

	requires transitive bento.fx.persistence.core;

	requires bento.fx.persistence.codec.common;

	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.dataformat.xml;
	requires java.xml;

	requires static org.jspecify;

	exports software.coley.bentofx.persistence.impl.codec.xml;
	exports software.coley.bentofx.persistence.impl.codec.xml.provider;

	provides LayoutCodecProvider with XmlLayoutCodecProvider;
}
