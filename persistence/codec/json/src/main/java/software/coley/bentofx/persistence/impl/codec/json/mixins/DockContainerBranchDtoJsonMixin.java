package software.coley.bentofx.persistence.impl.codec.json.mixins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DividerPositionDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerDto;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.*;

/**
 * Jackson JSON mix-in for {@code DockContainerBranchDto}.
 *
 * @author Phil Bryant
 */
@JsonInclude(NON_NULL)
@JsonTypeName(BRANCH_ELEMENT_NAME)
abstract class DockContainerBranchDtoJsonMixin {

	@JsonProperty(DIVIDER_POSITION_LIST_ELEMENT_NAME)
	@JsonInclude(NON_EMPTY)
	public @Nullable List<DividerPositionDto> dividerPositions;

	@JsonProperty(CHILD_DOCK_CONTAINER_LIST_ELEMENT_NAME)
	@JsonInclude(NON_EMPTY)
	public @Nullable List<DockContainerDto> childDockContainers;
}
