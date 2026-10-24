package software.coley.bentofx.persistence.core.api.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Opens the streams a persisted BentoFX docking layout is read from and written
 * to, and reports whether one is already stored.
 * <p>
 * An instance is owned by whichever component it is handed to. The
 * {@link software.coley.bentofx.persistence.core.api.LayoutSaver} or
 * {@link software.coley.bentofx.persistence.core.api.LayoutRestorer} that receives a
 * {@code LayoutStorage} closes it when that component is closed, so a single
 * instance must not be shared between the two - closing the saver would close
 * the storage the restorer still reads from. Obtain one instance per component
 * from a
 * {@link software.coley.bentofx.persistence.core.api.provider.LayoutStorageProvider}.
 * <p>
 * <b>Scope of this type.</b> A {@code LayoutStorage} is a single-use I/O
 * session against a stored layout, not a persistent reference to one. Operations
 * that do not need an open stream - checking whether a layout exists without
 * reading it, enumerating stored layouts, deleting one - live on
 * {@link software.coley.bentofx.persistence.core.api.provider.LayoutStorageProvider}
 * rather than here. The split mirrors the JDK's
 * {@link java.nio.file.Files} static operations versus {@link InputStream} and
 * {@link OutputStream}: the location-level operations are keyed by identifier
 * on the provider, and this type represents the opened stream.
 *
 * @author Phil Bryant
 */
public interface LayoutStorage extends AutoCloseable {

	/**
	 * Returns {@code true} if the stored layout exists; otherwise, returns {@code false}.
	 * <p>
	 * {@code false} means there is no layout to read, not that the answer could
	 * not be obtained. Answering may cost a query or a filesystem call, and an
	 * implementation whose store is unreachable throws an unchecked exception
	 * rather than reporting the layout as missing.
	 *
	 * @return {@code true} if the stored layout exists; otherwise, returns {@code false}.
	 */
	boolean exists();

	/**
	 * Returns an opened {@link OutputStream} that is expected to be
	 * owned/managed by the caller.
	 * @return an opened {@link OutputStream} that is expected to be
	 * owned/managed by the caller.
	 * @throws IOException if an I/O error occurs.
	 */
	OutputStream openOutputStream() throws IOException;

	/**
	 * Returns an opened {@link InputStream} that is expected to be
	 * owned/managed by the caller.
	 * @return an opened {@link InputStream} that is expected to be
	 * owned/managed by the caller.
	 * @throws IOException if an I/O error occurs.
	 */
	InputStream openInputStream() throws IOException;

	/**
	 * Releases any resources this storage holds. Called by the owning
	 * saver or restorer; see the ownership note on this interface before
	 * calling it directly.
	 */
	@Override
	default void close() {
		// Default no-op. Implementations that own resources should override.
	}
}
