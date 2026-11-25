import org.jspecify.annotations.NullMarked;
import software.coley.bentofx.persistence.core.api.provider.LayoutStorageProvider;
import software.coley.bentofx.persistence.impl.storage.file.provider.FileLayoutStorageProvider;

/**
 * This module implements the Application Programming Interface (API) for
 * encoding and decoding the layout of BentoFX docking framework components
 * files.
 *
 * @author Phil Bryant
 */
@NullMarked
module bento.fx.persistence.storage.file {

	requires transitive bento.fx.persistence.core;

	requires static org.jspecify;

	exports software.coley.bentofx.persistence.impl.storage.file;
	exports software.coley.bentofx.persistence.impl.storage.file.provider;

	provides LayoutStorageProvider with FileLayoutStorageProvider;
}
