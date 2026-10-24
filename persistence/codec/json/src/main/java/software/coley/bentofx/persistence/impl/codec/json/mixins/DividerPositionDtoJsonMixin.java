package software.coley.bentofx.persistence.impl.codec.json.mixins;

import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Jackson JSON mix-in for {@code DividerPositionDto}.
 *
 * @author Phil Bryant
 */
@JsonInclude(NON_NULL)
abstract class DividerPositionDtoJsonMixin {
}
