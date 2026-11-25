package software.coley.bentofx.persistence.impl.codec.xml.mixins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerRootBranchDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DragDropStageDto;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.*;

/**
 * Jackson XML mix-in for {@code BentoStateDto}.
 *
 * @author Phil Bryant
 */
@JsonInclude(NON_NULL)
abstract class BentoStateDtoXmlMixin {

	@JacksonXmlProperty(isAttribute = true)
	public @Nullable String identifier;

	@JacksonXmlElementWrapper(localName = ROOT_BRANCH_LIST_ELEMENT_NAME)
	@JacksonXmlProperty(localName = ROOT_BRANCH_ELEMENT_NAME)
	@JsonInclude(NON_EMPTY)
	public @Nullable List<DockContainerRootBranchDto> rootBranches;

	@JacksonXmlElementWrapper(localName = DRAG_DROP_STAGE_LIST_ELEMENT_NAME)
	@JacksonXmlProperty(localName = DRAG_DROP_STAGE_ELEMENT_NAME)
	@JsonInclude(NON_EMPTY)
	public @Nullable List<DragDropStageDto> dragDropStages;
}
