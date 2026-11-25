package software.coley.bentofx.persistence.impl.codec.xml.mixins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javafx.geometry.Side;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockableDto;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.DOCKABLE_ELEMENT_NAME;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.DOCKABLE_LIST_ELEMENT_NAME;

/**
 * Jackson XML mix-in for {@code DockContainerLeafDto}.
 *
 * @author Phil Bryant
 */
@JsonInclude(NON_NULL)
abstract class DockContainerLeafDtoXmlMixin {

	@JacksonXmlElementWrapper(localName = DOCKABLE_LIST_ELEMENT_NAME)
	@JacksonXmlProperty(localName = DOCKABLE_ELEMENT_NAME)
	@JsonInclude(NON_EMPTY)
	public @Nullable List<DockableDto> dockables;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable String identifier;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Boolean pruneWhenEmpty;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable String selectedDockableIdentifier;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Side side;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Boolean isResizableWithParent;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Boolean isCanSplit;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Double uncollapsedSizePx;

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable Boolean isCollapsed;
}
