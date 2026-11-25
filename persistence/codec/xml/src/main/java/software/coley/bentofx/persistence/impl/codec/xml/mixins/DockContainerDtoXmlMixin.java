package software.coley.bentofx.persistence.impl.codec.xml.mixins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerBranchDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerLeafDto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.BRANCH_ELEMENT_NAME;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.LEAF_ELEMENT_NAME;

/**
 * Jackson XML mix-in for {@code DockContainerDto}.
 *
 * @author Phil Bryant
 */
@JsonInclude(NON_NULL)
@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
@JsonSubTypes({
		@JsonSubTypes.Type(value = DockContainerBranchDto.class, name = BRANCH_ELEMENT_NAME),
		@JsonSubTypes.Type(value = DockContainerLeafDto.class, name = LEAF_ELEMENT_NAME)
})
abstract class DockContainerDtoXmlMixin {

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable String identifier;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Boolean pruneWhenEmpty;
}
