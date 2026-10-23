package software.coley.bentofx.persistence.impl.codec.common.mapper;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.codec.PersistableLayout;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.BentoState.BentoStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerBranchState;
import software.coley.bentofx.persistence.core.api.state.DockContainerBranchState.DockContainerBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState.DockContainerLeafStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState.DockContainerRootBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerState;
import software.coley.bentofx.persistence.core.api.state.DockableState;
import software.coley.bentofx.persistence.core.api.state.DockableState.DockableStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DragDropStageState;
import software.coley.bentofx.persistence.core.api.state.DragDropStageState.DragDropStageStateBuilder;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.BentoStateDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerBranchDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerLeafDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockContainerRootBranchDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockingLayoutDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DragDropStageDto;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.LayoutMetadataDto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.*;

class BentoStateMapperTest {
	// This test follows the "Single Act Rule" and only exercises the unit
	// under test (BentoStateMapper) once each: for mapping to and from a DTO.
	// As such, we are suppressing the warning for the number of assertions
	// ("Single Assert Rule").
	@SuppressWarnings("java:S5961")
	@Test
	void testFullMappingFromDomainToDtoAndBack() throws BentoStateException {

		final String expectedBentoIdentifier = "bento-1";
		final String expectedDockableIdentifier = "dockable-1";
		final String expectedLeafIdentifier = "leaf-1";
		final String expectedBranchIdentifier = "branch-1";
		final String expectedRootIdentifier = "root-1";
		final String expectedStageTitle = "Stage Title";

		// Set up the Domain Objects
		final DockableState dockableState = new DockableStateBuilder(expectedDockableIdentifier).build();

		final DockContainerLeafState leafState = new DockContainerLeafStateBuilder(expectedLeafIdentifier)
				.setPruneWhenEmpty(TRUE)
				.setCanSplit(TRUE)
				.addChildDockableState(dockableState)
				.build();

		final DockContainerBranchState branchState = new DockContainerBranchStateBuilder(expectedBranchIdentifier)
				.setPruneWhenEmpty(FALSE)
				.addDockContainerState(leafState)
				.build();

		final DockContainerRootBranchState rootState = new DockContainerRootBranchStateBuilder(expectedRootIdentifier)
				.setOrientation(javafx.geometry.Orientation.HORIZONTAL)
				.setPruneWhenEmpty(TRUE)
				.addDockContainerState(branchState)
				.build();

		final DragDropStageState stageState = new DragDropStageStateBuilder(TRUE)
				.setTitle(expectedStageTitle)
				.setDockContainerRootBranchState(rootState)
				.build();

		final BentoState bentoState = new BentoStateBuilder(expectedBentoIdentifier)
				.addRootBranchState(rootState)
				.addDragDropStageState(stageState)
				.build();

		final List<BentoState> bentoStates = new ArrayList<>();
		bentoStates.add(bentoState);

		// Perform Domain to DTO Mapping
		final DockingLayoutDto dto =
				BentoStateMapper.toDto(PersistableLayout.of(bentoStates));

		// Validate the DTO
		assertThat(dto)
				.describedAs("dto")
				.isNotNull();

		assertThat(dto.metadata)
				.describedAs("layout metadata")
				.isNotNull();
		assertThat(dto.metadata.schemaVersion)
				.describedAs("schema version")
				.isEqualTo(DockingLayoutDto.getCurrentSchemaVersion());

		assertThat(dto.bentoStates)
				.describedAs("dto.bentoStates")
				.hasSize(1);
		BentoStateDto bentoStateDto = dto.bentoStates.getFirst();
		assertThat(bentoStateDto.identifier)
				.describedAs("bentoStateDto.identifier")
				.isEqualTo(expectedBentoIdentifier);
		assertThat(bentoStateDto.rootBranches)
				.describedAs("bentoStateDto.rootBranches")
				.hasSize(1);
		assertThat(bentoStateDto.dragDropStages)
				.describedAs("bentoStateDto.dragDropStages")
				.hasSize(1);

		final DockContainerRootBranchDto rootBranchDto = bentoStateDto.rootBranches.getFirst();
		assertThat(rootBranchDto.identifier)
				.describedAs("rootBranchDto.identifier")
				.isEqualTo(expectedRootIdentifier);
		assertThat(rootBranchDto.childDockContainers)
				.describedAs("rootBranchDto.childDockContainers")
				.hasSize(1);
		assertThat(rootBranchDto.pruneWhenEmpty)
				.describedAs("rootBranchDto.pruneWhenEmpty")
				.isTrue();

		final DockContainerBranchDto branchDto =
				(DockContainerBranchDto) rootBranchDto.childDockContainers.getFirst();
		assertThat(branchDto.identifier)
				.describedAs("branchDto.identifier")
				.isEqualTo(expectedBranchIdentifier);
		assertThat(branchDto.pruneWhenEmpty)
				.describedAs("branchDto.pruneWhenEmpty")
				.isFalse();
		assertThat(branchDto.childDockContainers)
				.describedAs("branchDto.childDockContainers")
				.hasSize(1);

		final DockContainerLeafDto leafDto = (DockContainerLeafDto) branchDto.childDockContainers.getFirst();
		assertThat(leafDto.identifier)
				.describedAs("leafDto.identifier")
				.isEqualTo(expectedLeafIdentifier);
		assertThat(leafDto.pruneWhenEmpty)
				.describedAs("leafDto.pruneWhenEmpty")
				.isTrue();
		assertThat(leafDto.dockables.getFirst().identifier)
				.describedAs("leafDto.dockables.getFirst().identifier")
				.isEqualTo(expectedDockableIdentifier);

		final DragDropStageDto stageDto = bentoStateDto.dragDropStages.getFirst();
		assertThat(stageDto.autoCloseWhenEmpty)
				.describedAs("stageDto.autoCloseWhenEmpty")
				.isTrue();
		assertThat(stageDto.title)
				.describedAs("stageDto.title")
				.isEqualTo(expectedStageTitle);
		assertThat(stageDto.dockContainerRootBranchDto)
				.describedAs("stageDto.dockContainerRootBranchDto")
				.isNotNull();
		assertThat(stageDto.dockContainerRootBranchDto.identifier)
				.describedAs("stageDto.dockContainerRootBranchDto.identifier")
				.isEqualTo(expectedRootIdentifier);

		// Perform DTO to Domain Mapping
		final List<BentoState> deserializedBentoStates =
				BentoStateMapper.fromDto(dto).bentoStates();

		// Validate the Round-tripped Result
		assertThat(deserializedBentoStates)
				.describedAs("deserializedBentoStates")
				.isNotNull().hasSize(1);

		final BentoState deserializedBentoState =
				deserializedBentoStates.getFirst();
		assertThat(deserializedBentoState.getIdentifier())
				.describedAs("deserializedBentoState.getIdentifier()")
				.isEqualTo(expectedBentoIdentifier);
		assertThat(deserializedBentoState.getRootBranchStates())
				.describedAs("deserializedBentoState.getRootBranchStates()")
				.hasSize(1);
		assertThat(deserializedBentoState.getDragDropStageStates())
				.describedAs("deserializedBentoState.getDragDropStageStates()")
				.hasSize(1);

		final DockContainerRootBranchState deserializedRoot =
				deserializedBentoState.getRootBranchStates().getFirst();
		assertThat(deserializedRoot.getIdentifier())
				.describedAs("deserializedRoot.getIdentifier()")
				.isEqualTo(expectedRootIdentifier);
		assertThat(deserializedRoot.doPruneWhenEmpty())
				.describedAs("deserializedRoot.doPruneWhenEmpty()")
				.contains(TRUE);
		assertThat(deserializedRoot.getChildDockContainerStates())
				.describedAs("deserializedRoot.getChildDockContainerStates()")
				.hasSize(1);

		final DockContainerBranchState deserializedBranchState =
				(DockContainerBranchState) deserializedRoot.getChildDockContainerStates().getFirst();
		assertThat(deserializedBranchState.getIdentifier())
				.describedAs("deserializedBranchState.getIdentifier()")
				.isEqualTo(expectedBranchIdentifier);
		assertThat(deserializedBranchState.doPruneWhenEmpty())
				.describedAs("deserializedBranchState.doPruneWhenEmpty()")
				.contains(FALSE);

		final DockContainerLeafState deserializedLeafState =
				(DockContainerLeafState) deserializedBranchState.getChildDockContainerStates().getFirst();
		assertThat(deserializedLeafState.getIdentifier())
				.describedAs("deserializedLeafState.getIdentifier()")
				.isEqualTo(expectedLeafIdentifier);
		assertThat(deserializedLeafState.doPruneWhenEmpty())
				.describedAs("deserializedLeafState.doPruneWhenEmpty()")
				.contains(TRUE);
		assertThat(deserializedLeafState.getChildDockableStates())
				.describedAs("deserializedLeafState.getChildDockableStates()")
				.hasSize(1);

		final DockableState deserializedDockableState =
				deserializedLeafState.getChildDockableStates().getFirst();
		assertThat(deserializedDockableState.getIdentifier())
				.describedAs("deserializedDockableState.getIdentifier()")
				.isEqualTo(expectedDockableIdentifier);
	}
	@Test
	void mixedRootChildrenRoundTripInOrder() throws BentoStateException {
		// A root branch holding a leaf, then a branch, then another leaf
		final DockContainerRootBranchState rootState =
				new DockContainerRootBranchStateBuilder("root-1")
						.addDockContainerState(
								new DockContainerLeafStateBuilder("leaf-A").build())
						.addDockContainerState(
								new DockContainerBranchStateBuilder("branch-B").build())
						.addDockContainerState(
								new DockContainerLeafStateBuilder("leaf-C").build())
						.build();

		final BentoState bentoState = new BentoStateBuilder("bento-1")
				.addRootBranchState(rootState)
				.build();

		final List<BentoState> roundTripped = BentoStateMapper.fromDto(
				BentoStateMapper.toDto(
						PersistableLayout.of(List.of(bentoState))
				)
		).bentoStates();

		assertThat(roundTripped.getFirst()
				.getRootBranchStates().getFirst()
				.getChildDockContainerStates())
				.describedAs("round-tripped root branch child dock containers, in order")
				.extracting(DockContainerState::getIdentifier)
				.containsExactly("leaf-A", "branch-B", "leaf-C");
	}

	@Test
	void aBranchNestedInsideAnotherBranchRoundTripsCorrectly() throws BentoStateException {
		// A branch whose only child is itself a branch, rather than a leaf.
		final DockContainerBranchState innerBranchState =
				new DockContainerBranchStateBuilder("inner-branch")
						.addDockContainerState(
								new DockContainerLeafStateBuilder("leaf-in-inner").build())
						.build();

		final DockContainerBranchState outerBranchState =
				new DockContainerBranchStateBuilder("outer-branch")
						.addDockContainerState(innerBranchState)
						.build();

		final DockContainerBranchDto outerBranchDto =
				BentoStateMapper.toDto(outerBranchState);

		assertThat(outerBranchDto.childDockContainers)
				.describedAs("outerBranchDto.childDockContainers")
				.hasSize(1);
		assertThat(outerBranchDto.childDockContainers.getFirst())
				.describedAs("outerBranchDto's nested child")
				.isInstanceOf(DockContainerBranchDto.class);

		final DockContainerRootBranchState rootState =
				new DockContainerRootBranchStateBuilder("root-1")
						.addDockContainerState(outerBranchState)
						.build();
		final BentoState bentoState = new BentoStateBuilder("bento-1")
				.addRootBranchState(rootState)
				.build();

		final List<BentoState> roundTripped = BentoStateMapper.fromDto(
				BentoStateMapper.toDto(PersistableLayout.of(List.of(bentoState)))
		).bentoStates();

		final DockContainerBranchState roundTrippedOuter =
				(DockContainerBranchState) roundTripped.getFirst()
						.getRootBranchStates().getFirst()
						.getChildDockContainerStates().getFirst();
		assertThat(roundTrippedOuter.getIdentifier())
				.describedAs("round-tripped outer branch identifier")
				.isEqualTo("outer-branch");

		final DockContainerBranchState roundTrippedInner =
				(DockContainerBranchState) roundTrippedOuter
						.getChildDockContainerStates().getFirst();
		assertThat(roundTrippedInner.getIdentifier())
				.describedAs("round-tripped inner branch identifier")
				.isEqualTo("inner-branch");
	}

	@Test
	void fromDtoRejectsADtoWithNoMetadata() {
		final DockingLayoutDto dto = new DockingLayoutDto();

		assertThatThrownBy(() -> BentoStateMapper.fromDto(dto))
				.describedAs("mapping a DTO with no metadata object")
				.isInstanceOf(BentoStateException.class)
				.hasMessageContaining("declares no schema version");
	}

	@Test
	void fromDtoReadsTheDisplayNameFromMetadata() throws BentoStateException {
		final DockingLayoutDto dto = new DockingLayoutDto();
		dto.metadata = new LayoutMetadataDto();
		dto.metadata.schemaVersion = DockingLayoutDto.getCurrentSchemaVersion();
		dto.metadata.displayName = "Multi-Monitor";

		assertThat(BentoStateMapper.fromDto(dto).displayName())
				.describedAs("display name read from metadata")
				.isEqualTo("Multi-Monitor");
	}

	@Test
	void fromDtoReadsTheGroupFromMetadata() throws BentoStateException {
		final DockingLayoutDto dto = new DockingLayoutDto();
		dto.metadata = new LayoutMetadataDto();
		dto.metadata.schemaVersion = DockingLayoutDto.getCurrentSchemaVersion();
		dto.metadata.group = "Debugging";
		dto.metadata.groups = List.of("Debugging", "Presentation");

		final PersistableLayout layout = BentoStateMapper.fromDto(dto);

		assertThat(layout.group())
				.describedAs("group read from metadata")
				.isEqualTo("Debugging");
		assertThat(layout.groups())
				.describedAs("group catalog read from metadata")
				.containsExactly("Debugging", "Presentation");
	}

	/**
	 * A layout that carries neither the group nor the group catalog has to decode
	 * rather than fail, since both are optional in the stored form. Written out as
	 * a literal {@code 1} on purpose: reading the constant would assert the code
	 * against itself and pass however far the version moves.
	 */
	@Test
	void fromDtoReadsASchemaVersionOneLayoutAsUngrouped()
			throws BentoStateException {
		final DockingLayoutDto dto = new DockingLayoutDto();
		dto.metadata = new LayoutMetadataDto();
		dto.metadata.schemaVersion = 1;
		dto.metadata.displayName = "Multi-Monitor";
		dto.metadata.group = null;

		final PersistableLayout layout = BentoStateMapper.fromDto(dto);

		assertThat(layout.displayName())
				.describedAs("display name from a version 1 layout")
				.isEqualTo("Multi-Monitor");
		assertThat(layout.group())
				.describedAs("group from a version 1 layout")
				.isNull();
		assertThat(layout.groups())
				.describedAs("catalog from a version 1 layout")
				.isEmpty();
	}

	/**
	 * Pinned to a literal so that moving it is a deliberate edit here as well.
	 * Version 1 is the only version there has been, and a bump makes every layout
	 * this framework writes unreadable by the version before it.
	 */
	@Test
	void writesSchemaVersionOne() {
		assertThat(DockingLayoutDto.getCurrentSchemaVersion())
				.describedAs("schema version this framework writes")
				.isEqualTo(1);
	}

	@Test
	void validateSupportedMetadataRejectsMissingMetadata() {
		assertThatThrownBy(() ->
				BentoStateMapper.validateSupportedMetadata(null)
		)
				.describedAs("missing metadata validation")
				.isInstanceOf(BentoStateException.class)
				.hasMessageContaining("declares no schema version");
	}

	@Test
	void validateSupportedMetadataRejectsMissingSchemaVersion() {
		final LayoutMetadataDto metadata = new LayoutMetadataDto();

		assertThatThrownBy(() ->
				BentoStateMapper.validateSupportedMetadata(metadata)
		)
				.describedAs("missing schema version validation")
				.isInstanceOf(BentoStateException.class)
				.hasMessageContaining("declares no schema version");
	}

	@Test
	void validateSupportedMetadataAllowsCurrentSchemaVersion() {
		final LayoutMetadataDto metadata = new LayoutMetadataDto();
		metadata.schemaVersion = DockingLayoutDto.getCurrentSchemaVersion();

		assertThatCode(() ->
				BentoStateMapper.validateSupportedMetadata(metadata)
		)
				.describedAs("current schema version validation")
				.doesNotThrowAnyException();
	}

	@Test
	void validateSupportedMetadataRejectsInvalidSchemaVersion() {
		final LayoutMetadataDto metadata = new LayoutMetadataDto();
		metadata.schemaVersion = 0;

		assertThatThrownBy(() ->
				BentoStateMapper.validateSupportedMetadata(metadata)
		)
				.describedAs("invalid schema version validation")
				.isInstanceOf(BentoStateException.class)
				.hasMessageContaining("Unsupported BentoFX docking layout schema version: 0");
	}

	@Test
	void validateSupportedMetadataRejectsFutureSchemaVersion() {
		final LayoutMetadataDto metadata = new LayoutMetadataDto();
		metadata.schemaVersion = DockingLayoutDto.getCurrentSchemaVersion() + 1;

		assertThatThrownBy(() ->
				BentoStateMapper.validateSupportedMetadata(metadata)
		)
				.describedAs("future schema version validation")
				.isInstanceOf(BentoStateException.class)
				.hasMessageContaining("Unsupported BentoFX docking layout schema version");
	}

	@Test
	// Suppress warnings for passing null argument to parameter annotated as
	// non-null; that's what we're testing.
	@SuppressWarnings({"ConstantConditions", "NullAway"})
	void everyToDtoOverloadNamesTheArgumentItRejects() {
		assertThatThrownBy(() -> BentoStateMapper.toDto((DragDropStageState) null))
				.describedAs("toDto(DragDropStageState) null argument")
				.isInstanceOf(NullPointerException.class)
				.hasMessage("stageState");

		assertThatThrownBy(() -> BentoStateMapper.toDto((DockableState) null))
				.describedAs("toDto(DockableState) null argument")
				.isInstanceOf(NullPointerException.class)
				.hasMessage("dockableState");

		assertThatThrownBy(() -> BentoStateMapper.toDto((DockContainerBranchState) null))
				.describedAs("toDto(DockContainerBranchState) null argument")
				.isInstanceOf(NullPointerException.class)
				.hasMessage("branchState");

		assertThatThrownBy(() -> BentoStateMapper.toDto((DockContainerLeafState) null))
				.describedAs("toDto(DockContainerLeafState) null argument")
				.isInstanceOf(NullPointerException.class)
				.hasMessage("leafState");
	}

	/**
	 * A utility class with only static members has no reason to be
	 * instantiated; the private constructor exists to say so rather than to
	 * silently allow it.
	 */
	@Test
	void utilityClassConstructorThrowsIllegalStateException() throws Exception {
		final Constructor<BentoStateMapper> constructor =
				BentoStateMapper.class.getDeclaredConstructor();
		constructor.setAccessible(true);

		assertThatThrownBy(constructor::newInstance)
				.describedAs("reflective instantiation of the utility class")
				.isInstanceOf(InvocationTargetException.class)
				.cause()
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Utility class");
	}
}
