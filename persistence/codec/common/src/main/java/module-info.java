import org.jspecify.annotations.NullMarked;

/**
 * This module provides classes used by multiple implementations persisting
 * BentoFX layouts.
 *
 * @author Phil Bryant
 */
@NullMarked
// The codec modules the exports below are qualified to depend on this one, so
// they are never visible while it compiles; javac's "module not found" warning
// for them is expected.
@SuppressWarnings("module")
module bento.fx.persistence.codec.common {

	requires transitive bento.fx.persistence.core;

	requires transitive javafx.graphics;

	requires static org.jspecify;

	// Only the codecs built on this mapping may use it: the DTOs are mutable and
	// the mapping is an implementation detail, not API an application should
	// compile against.
	exports software.coley.bentofx.persistence.impl.codec.common.mapper to
			bento.fx.persistence.codec.json,
			bento.fx.persistence.codec.xml;
	exports software.coley.bentofx.persistence.impl.codec.common.mapper.dto to
			bento.fx.persistence.codec.json,
			bento.fx.persistence.codec.xml;

	opens software.coley.bentofx.persistence.impl.codec.common.mapper.dto;
}
