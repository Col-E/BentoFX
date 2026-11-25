package software.coley.bentofx.persistence.impl.codec.xml.provider;

import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.provider.LayoutCodecProvider;
import software.coley.bentofx.persistence.impl.codec.xml.XmlLayoutCodec;

/**
 * XML (eXtensible Markup Language) implementation of the
 * {@link LayoutCodecProvider} Service Provider Interface.
 *
 * @author Phil Bryant
 */
public class XmlLayoutCodecProvider implements LayoutCodecProvider {

	/**
	 * Creates a {@link LayoutCodecProvider} for XML.
	 */
	public XmlLayoutCodecProvider() {
		// This empty constructor exists merely to support Javadoc and its
		// recommended practice for providing the comment for the default
		// constructor.
	}

	@Override
	public String getIdentifier() {
		return XmlLayoutCodec.CODEC_IDENTIFIER;
	}

	@Override
	public LayoutCodec getLayoutCodec() {
		return new XmlLayoutCodec();
	}
}
