package software.coley.bentofx.persistence.impl.codec.xml.provider;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.impl.codec.xml.XmlLayoutCodec;

import static org.assertj.core.api.Assertions.assertThat;

class XmlLayoutCodecProviderTest {

	@Test
	void exposesXmlIdentifierAndIsNotDefault() {
		final XmlLayoutCodecProvider provider = new XmlLayoutCodecProvider();

		assertThat(provider.getIdentifier())
				.describedAs("provider.getIdentifier()")
				.isEqualTo(XmlLayoutCodec.CODEC_IDENTIFIER);
		assertThat(provider.isDefault())
				.describedAs("provider.isDefault()")
				.isFalse();
	}

	@Test
	void getLayoutCodecReturnsXmlLayoutCodec() {
		final XmlLayoutCodecProvider provider = new XmlLayoutCodecProvider();

		final LayoutCodec codec = provider.getLayoutCodec();

		assertThat(codec)
				.describedAs("codec")
				.isNotNull()
				.isInstanceOf(XmlLayoutCodec.class);
		assertThat(codec.getIdentifier())
				.describedAs("codec.getIdentifier()")
				.isEqualTo(XmlLayoutCodec.CODEC_IDENTIFIER);
	}
}
