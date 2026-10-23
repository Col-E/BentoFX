package software.coley.bentofx.persistence.impl.codec.json.mixins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.BentoStateDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.LayoutMetadataDto;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.BENTO_LIST_ELEMENT_NAME;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.METADATA_ELEMENT_NAME;

/**
 * Jackson JSON mix-in for {@code DockingLayoutDto}.
 *
 * @author Phil Bryant
 */
@JsonInclude(NON_NULL)
abstract class DockingLayoutDtoJsonMixin {

	@JsonProperty(METADATA_ELEMENT_NAME)
	public @Nullable LayoutMetadataDto metadata;

	@JsonProperty(BENTO_LIST_ELEMENT_NAME)
	@JsonInclude(NON_EMPTY)
	public @Nullable List<BentoStateDto> bentoStates;
}
