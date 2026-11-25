package software.coley.bentofx.persistence.impl.codec.xml;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.codec.PersistableLayout;
import software.coley.bentofx.persistence.impl.codec.common.mapper.BentoStateMapper;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockingLayoutDto;
import software.coley.bentofx.persistence.impl.codec.xml.mixins.XmlMapperMixins;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * XML implementation of {@link LayoutCodec} using Jackson XML and external
 * mix-ins.
 *
 * @author Phil Bryant
 */
public final class XmlLayoutCodec implements LayoutCodec {

	/**
	 * Uniquely identifies this {@link LayoutCodec} to distinguish it from other
	 * {@link LayoutCodec}.
	 */
	public static final String CODEC_IDENTIFIER = "xml";

	private final XmlMapper mapper;

	/**
	 * Creates an {@code XmlLayoutCodec}, mapping mixins for reading and writing
	 * XML.
	 */
	public XmlLayoutCodec() {
		// Unknown elements and attributes are skipped, so a layout written by a
		// later version that adds an optional one still loads here. A change
		// older versions cannot read bumps the schema version instead.
		this.mapper = XmlMapper.builder()
				.enable(SerializationFeature.INDENT_OUTPUT)
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.build();
		XmlMapperMixins.mixinsByDto().forEach(mapper::addMixIn);
	}

	@Override
	public String getIdentifier() {
		return CODEC_IDENTIFIER;
	}

	@Override
	public void encode(
			final PersistableLayout layout,
			final OutputStream outputStream
	) throws BentoStateException {
		try {
			final DockingLayoutDto dto = BentoStateMapper.toDto(layout);
			mapper.writeValue(outputStream, dto);
		} catch (final IOException | RuntimeException e) {
			throw new BentoStateException("Failed to encode layout as XML", e);
		}
	}

	@Override
	public PersistableLayout decode(
			final InputStream inputStream
	) throws BentoStateException {
		try {
			final DockingLayoutDto dockingLayoutDto =
					mapper.readValue(inputStream, DockingLayoutDto.class);

			return BentoStateMapper.fromDto(dockingLayoutDto);
		} catch (final IOException | RuntimeException e) {
			throw new BentoStateException(
					"Failed to decode layout from XML",
					e
			);
		}
	}
}
