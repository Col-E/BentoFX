package software.coley.bentofx.persistence.impl.codec.json.mixins;

import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.BentoStateDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DividerPositionDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerBranchDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerLeafDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerRootBranchDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockableDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockingLayoutDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DragDropStageDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.LayoutMetadataDto;

import java.util.Map;

import static java.util.Map.entry;

/**
 * The DTO Jackson JSON mix-ins, for a mapper to register.
 *
 * @author Phil Bryant
 */
public final class ObjectMapperMixins {

	/**
	 * Each DTO and the mix-in that annotates it as a map rather than a run of
	 * {@code addMixIn} calls so a test can read the pairing back.
	 */
	static final Map<Class<?>, Class<?>> MIXINS_BY_DTO = Map.ofEntries(
			entry(BentoStateDto.class, BentoStateDtoJsonMixin.class),
			entry(DividerPositionDto.class, DividerPositionDtoJsonMixin.class),
			entry(DockableDto.class, DockableDtoJsonMixin.class),
			entry(DockContainerDto.class, DockContainerDtoJsonMixin.class),
			entry(DockContainerBranchDto.class, DockContainerBranchDtoJsonMixin.class),
			entry(DockContainerLeafDto.class, DockContainerLeafDtoJsonMixin.class),
			entry(DockContainerRootBranchDto.class, DockContainerRootBranchDtoJsonMixin.class),
			entry(DockingLayoutDto.class, DockingLayoutDtoJsonMixin.class),
			entry(DragDropStageDto.class, DragDropStageDtoJsonMixin.class),
			entry(LayoutMetadataDto.class, LayoutMetadataDtoJsonMixin.class)
	);

	private ObjectMapperMixins() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * {@return each DTO and the mix-in that annotates it.}
	 *
	 * <p>Classes rather than a configured mapper, so that nothing here puts a
	 * Jackson type in a signature a caller can see.</p>
	 */
	public static Map<Class<?>, Class<?>> mixinsByDto() {
		return MIXINS_BY_DTO;
	}
}
