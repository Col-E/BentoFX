package software.coley.bentofx.persistence.impl.codec.xml.mixins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javafx.geometry.Orientation;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DividerPositionDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerDto;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.DIVIDER_ELEMENT_NAME;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.DIVIDER_POSITION_LIST_ELEMENT_NAME;

/**
 * Jackson XML mix-in for {@code DockContainerBranchDto}.
 *
 * @author Phil Bryant
 */
@JsonInclude(NON_NULL)
abstract class DockContainerBranchDtoXmlMixin {

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable String identifier;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Boolean pruneWhenEmpty;

	@JacksonXmlElementWrapper(localName = DIVIDER_POSITION_LIST_ELEMENT_NAME)
	@JacksonXmlProperty(localName = DIVIDER_ELEMENT_NAME)
	@JsonInclude(NON_EMPTY)
	public @Nullable List<DividerPositionDto> dividerPositions;

	@JacksonXmlElementWrapper(useWrapping = false)
	@JsonInclude(NON_EMPTY)
	public @Nullable List<DockContainerDto> childDockContainers;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Orientation orientation;
}
