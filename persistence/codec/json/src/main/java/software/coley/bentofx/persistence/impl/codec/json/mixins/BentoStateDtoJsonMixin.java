package software.coley.bentofx.persistence.impl.codec.json.mixins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerRootBranchDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DragDropStageDto;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.DRAG_DROP_STAGE_LIST_ELEMENT_NAME;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.ROOT_BRANCH_LIST_ELEMENT_NAME;

/**
 * Jackson JSON mix-in for {@code BentoStateDto}.
 *
 * @author Phil Bryant
 */
@JsonInclude(NON_NULL)
abstract class BentoStateDtoJsonMixin {

	@JsonProperty(ROOT_BRANCH_LIST_ELEMENT_NAME)
	@JsonInclude(NON_EMPTY)
	public @Nullable List<DockContainerRootBranchDto> rootBranches;

	@JsonProperty(DRAG_DROP_STAGE_LIST_ELEMENT_NAME)
	@JsonInclude(NON_EMPTY)
	public @Nullable List<DragDropStageDto> dragDropStages;
}
