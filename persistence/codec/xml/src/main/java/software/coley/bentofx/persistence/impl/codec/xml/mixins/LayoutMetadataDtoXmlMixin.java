package software.coley.bentofx.persistence.impl.codec.xml.mixins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.*;

/**
 * Jackson XML mix-in for {@code LayoutMetadataDto}.
 *
 * <p>{@code NON_NULL} is what keeps an unnamed layout from writing an empty
 * {@code <displayName/>}, which XML reads back as {@code ""} rather than
 * {@code null}. The same applies to a layout in no group.</p>
 *
 * @author Phil Bryant
 */
@JsonInclude(NON_NULL)
abstract class LayoutMetadataDtoXmlMixin {

	@JacksonXmlProperty(localName = SCHEMA_VERSION_ELEMENT_NAME)
	public @Nullable Integer schemaVersion;

	@JacksonXmlProperty(localName = DISPLAY_NAME_ELEMENT_NAME)
	public @Nullable String displayName;

	@JacksonXmlProperty(localName = GROUP_ELEMENT_NAME)
	public @Nullable String group;

	@JacksonXmlElementWrapper(localName = GROUP_LIST_ELEMENT_NAME)
	@JacksonXmlProperty(localName = GROUP_NAME_ELEMENT_NAME)
	@JsonInclude(NON_EMPTY)
	public @Nullable List<String> groups;
}
