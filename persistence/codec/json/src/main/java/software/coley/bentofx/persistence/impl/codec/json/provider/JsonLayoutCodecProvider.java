package software.coley.bentofx.persistence.impl.codec.json.provider;

import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.provider.LayoutCodecProvider;
import software.coley.bentofx.persistence.impl.codec.json.JsonLayoutCodec;

/**
 * JSON (JavaScript Object Notation) implementation of the
 * {@link LayoutCodecProvider} Service Provider Interface.
 *
 * @author Phil Bryant
 */
public class JsonLayoutCodecProvider implements LayoutCodecProvider {

	/**
	 * Creates a {@link LayoutCodecProvider} for JSON.
	 */
	public JsonLayoutCodecProvider() {
		// This empty constructor exists merely to support Javadoc and its
		// recommended practice for providing the comment for the default
		// constructor.
	}

	@Override
	public String getIdentifier() {
		return JsonLayoutCodec.CODEC_IDENTIFIER;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * JSON is the preferred default codec when more than one codec is present
	 * and the application has not selected one, matching the file storage
	 * provider's role as the preferred default storage. Keeps the active
	 * codec stable against dependency drift that adds another codec module.
	 */
	@Override
	public boolean isDefault() {
		return true;
	}

	@Override
	public LayoutCodec getLayoutCodec() {
		return new JsonLayoutCodec();
	}
}
