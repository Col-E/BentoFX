package software.coley.bentofx.persistence.impl.codec.xml.mixins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jspecify.annotations.Nullable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Jackson XML mix-in for {@code DockableDto}.
 *
 * @author Phil Bryant
 */
@JsonInclude(NON_NULL)
abstract class DockableDtoXmlMixin {

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable String identifier;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable String title;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable String tooltipText;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Integer dragGroupMask;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Boolean isClosable;
}
