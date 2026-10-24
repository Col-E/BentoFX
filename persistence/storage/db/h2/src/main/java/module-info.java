import org.jspecify.annotations.NullMarked;
import software.coley.bentofx.persistence.core.api.provider.LayoutStorageProvider;
import software.coley.bentofx.persistence.impl.storage.db.provider.DatabaseLayoutStorageProvider;

/**
 * This module implements the Bento persistence database storage provider using
 * an H2 database configured using the Jakarta persistence API with Hikari for
 * thread pooling.
 *
 * @author Phil Bryant
 */
@NullMarked
module bento.fx.persistence.storage.db.h2Database {

	requires transitive bento.fx.persistence.core;
	requires transitive jakarta.persistence;

	requires static org.jspecify;

	requires com.zaxxer.hikari;
	requires jakarta.cdi.lang.model;
	requires jakarta.el;
	requires jakarta.transaction;
	requires jakarta.xml.bind;
	requires net.bytebuddy;

	requires org.hibernate.orm.core;
	requires org.hibernate.orm.hikaricp;
	requires org.hibernate.validator;
	requires org.slf4j;

	// The storage and entity package is not exported: its classes are this
	// provider's implementation, and the entity key must stay public for JPA,
	// which would otherwise make it API. Hibernate reaches it through the opens
	// below.
	exports software.coley.bentofx.persistence.impl.storage.db.provider;

	opens software.coley.bentofx.persistence.impl.storage.db to
			org.hibernate.orm.core;

	provides LayoutStorageProvider with
			DatabaseLayoutStorageProvider;
}
