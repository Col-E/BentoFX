package software.coley.bentofx.persistence.core.api.provider;

import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;

/**
 * Supplies the {@link LayoutCodec} that decides the format a docking layout is
 * written in.
 *
 * <p>Discovered at runtime, so an application changes format by changing which
 * codec implementation it depends on - see
 * {@link LayoutPersistenceComponentProvider}. The provider is the discoverable
 * type; the {@link LayoutCodec} it returns does not need to be discoverable
 * itself.</p>
 *
 * @author Phil Bryant
 */
public interface LayoutCodecProvider extends LayoutPersistenceComponentProvider {

	/**
	 * Returns a {@link LayoutCodec}.
	 * @return a {@link LayoutCodec}
	 */
	LayoutCodec getLayoutCodec();
}
