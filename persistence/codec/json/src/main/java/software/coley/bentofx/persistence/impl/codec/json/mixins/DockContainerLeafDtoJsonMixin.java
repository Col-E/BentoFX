package software.coley.bentofx.persistence.impl.codec.json.mixins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockableDto;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.DOCKABLE_LIST_ELEMENT_NAME;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.LEAF_ELEMENT_NAME;

/**
 * Jackson JSON mix-in for {@code DockContainerLeafDto}.
 *
 * @author Phil Bryant
 */
@JsonInclude(NON_NULL)
@JsonTypeName(LEAF_ELEMENT_NAME)
abstract class DockContainerLeafDtoJsonMixin {

	@JsonProperty(DOCKABLE_LIST_ELEMENT_NAME)
	@JsonInclude(NON_EMPTY)
	public @Nullable List<DockableDto> dockables;
}
