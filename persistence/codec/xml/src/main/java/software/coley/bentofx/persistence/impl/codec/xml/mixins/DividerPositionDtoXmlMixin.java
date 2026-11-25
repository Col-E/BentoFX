package software.coley.bentofx.persistence.impl.codec.xml.mixins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jspecify.annotations.Nullable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Jackson XML mix-in for {@code DividerPositionDto}.
 *
 * @author Phil Bryant
 */
@JsonInclude(NON_NULL)
abstract class DividerPositionDtoXmlMixin {

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Integer index;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Double position;
}
