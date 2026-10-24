package software.coley.bentofx.persistence.impl.codec.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.codec.PersistableLayout;
import software.coley.bentofx.persistence.impl.codec.common.mapper.BentoStateMapper;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockingLayoutDto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static software.coley.bentofx.persistence.impl.codec.json.mixins.ObjectMapperMixins.mixinsByDto;

/**
 * JSON implementation of {@link LayoutCodec}.
 *
 * @author Phil Bryant
 */
public final class JsonLayoutCodec implements LayoutCodec {

	/**
	 * Uniquely identifies this {@link LayoutCodec} to distinguish it from other
	 * {@link LayoutCodec}.
	 */
	public static final String CODEC_IDENTIFIER = "json";
	private final ObjectMapper mapper;

	/**
	 * Creates an {@code JsonLayoutCodec}, mapping mixins for reading and
	 * writing JSON.
	 */
	public JsonLayoutCodec() {
		// Unknown properties are skipped, so a layout written by a later version
		// that adds an optional property still loads here. A change older
		// versions cannot read bumps the schema version instead.
		this.mapper = new ObjectMapper()
				.enable(INDENT_OUTPUT)
				.disable(FAIL_ON_UNKNOWN_PROPERTIES);
		mixinsByDto().forEach(mapper::addMixIn);
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

			final DockingLayoutDto dockingLayoutDto =
					BentoStateMapper.toDto(layout);
			mapper.writeValue(outputStream, dockingLayoutDto);
		} catch (final IOException | RuntimeException e) {

			throw new BentoStateException(
					"Failed to encode layout as JSON",
					e
			);
		}
	}

	@Override
	public PersistableLayout decode(
			final InputStream inputStream
	) throws BentoStateException {
		try {
			final DockingLayoutDto dockingLayoutDto =
					mapper.readValue(
							inputStream,
							DockingLayoutDto.class
					);

			return BentoStateMapper.fromDto(dockingLayoutDto);
		} catch (final IOException | RuntimeException e) {

			// Catch both IOException and RuntimeException because a
			// malformed payload can fail anywhere in the mapper or
			// the state builders.
			throw new BentoStateException(
					"Failed to decode layout from JSON",
					e
			);
		}
	}
}
