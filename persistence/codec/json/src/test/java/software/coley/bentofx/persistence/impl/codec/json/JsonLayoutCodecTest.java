package software.coley.bentofx.persistence.impl.codec.json;

import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.codec.PersistableLayout;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.DockContainerBranchState;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState;
import software.coley.bentofx.persistence.core.api.state.DockContainerState;
import software.coley.bentofx.persistence.impl.codec.common.mapper.BentoStateMapper;
import software.coley.bentofx.persistence.impl.codec.common.mapper.dto.DockingLayoutDto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.coley.bentofx.persistence.impl.codec.common.mapper.ElementNames.*;
import static software.coley.bentofx.persistence.testfixtures.codec.dto.SampleDockingLayoutDtoFactory.createDockingLayoutDto;
import static software.coley.bentofx.persistence.testfixtures.codec.state.SampleBentoStateFactory.createBentoStates;

class JsonLayoutCodecTest {

	@Test
	void getIdentifierReturnsJson() {
		final JsonLayoutCodec codec = new JsonLayoutCodec();

		assertThat(codec.getIdentifier())
				.describedAs("codec identifier")
				.isEqualTo("json");
	}

	@Test
	void encodeWritesLayoutMetadata() throws Exception {
		final JsonLayoutCodec codec = new JsonLayoutCodec();
		final List<BentoState> states = createStates();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		codec.encode(PersistableLayout.of(states), out);

		final String json = out.toString(StandardCharsets.UTF_8);

		assertThat(json)
				.describedAs("encoded JSON schema version metadata")
				.contains("\"schemaVersion\" : " + DockingLayoutDto.getCurrentSchemaVersion());
	}

	@Test
	void encodeWritesTheLayoutWithoutAnEnclosingElement() throws Exception {
		final JsonLayoutCodec codec = new JsonLayoutCodec();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		codec.encode(PersistableLayout.of(createStates()), out);

		// JSON needs no wrapping element: the layout's own fields sit at the top
		// level, so the shared root element name should not appear at all.
		assertThat(out.toString(StandardCharsets.UTF_8))
				.describedAs("encoded JSON root")
				.doesNotContain(DOCKING_LAYOUT_ROOT_ELEMENT_NAME)
				.startsWith("{")
				.contains("\"metadata\"");
	}

	@Test
	void encodeOmitsEmptyCollections() throws Exception {
		final JsonLayoutCodec codec = new JsonLayoutCodec();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		codec.encode(PersistableLayout.of(emptyRootStates()), out);

		// The DTO list fields are never null, so only NON_EMPTY keeps a layout
		// with nothing in it from writing a line per empty list.
		assertThat(out.toString(StandardCharsets.UTF_8))
				.describedAs("encoded JSON for an empty root branch")
				.doesNotContain(DIVIDER_POSITION_LIST_ELEMENT_NAME)
				.doesNotContain(CHILD_DOCK_CONTAINER_LIST_ELEMENT_NAME)
				.doesNotContain(DRAG_DROP_STAGE_LIST_ELEMENT_NAME);
	}

	@Test
	void encodeWritesDividerPositionsInIndexOrder() throws Exception {
		final JsonLayoutCodec codec = new JsonLayoutCodec();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		codec.encode(PersistableLayout.of(scrambledDividerStates()), out);

		final String json = out.toString(StandardCharsets.UTF_8);

		// The state holds divider positions in an immutable map, whose iteration
		// order varies between JVM runs, so the encoded order has to be imposed.
		final List<Integer> encodedIndexes = new ArrayList<>();
		final Matcher matcher = Pattern.compile("\"index\" : (\\d+)").matcher(json);
		while (matcher.find()) {
			encodedIndexes.add(Integer.valueOf(matcher.group(1)));
		}

		assertThat(encodedIndexes)
				.describedAs("encoded divider indexes")
				.containsExactly(0, 1, 2, 3, 4);
	}

	@Test
	void encodeWrapsAnIOExceptionAsBentoStateException() throws Exception {
		final JsonLayoutCodec codec = new JsonLayoutCodec();
		final IOException writeFailure = new IOException("disk full");
		final OutputStream failingOutputStream = new OutputStream() {
			@Override
			public void write(final int b) throws IOException {
				throw writeFailure;
			}
		};
		final PersistableLayout layout = PersistableLayout.of(createStates());

		assertThatThrownBy(() -> codec.encode(layout, failingOutputStream))
				.describedAs("encode with a failing output stream")
				.isInstanceOf(BentoStateException.class)
				.hasMessageContaining("Failed to encode layout as JSON")
				.cause()
				.isSameAs(writeFailure);
	}

	@Test
	void decodeRejectsFutureSchemaVersion() {
		final JsonLayoutCodec codec = new JsonLayoutCodec();
		final int futureSchemaVersion =
				DockingLayoutDto.getCurrentSchemaVersion() + 1;
		final String json = """
				{
				  "metadata": {
					"schemaVersion": %d
				  },
				  "bentos": []
				}
				""".formatted(futureSchemaVersion);

		assertThatThrownBy(() ->
				codec.decode(new ByteArrayInputStream(
						json.getBytes(StandardCharsets.UTF_8)))
						.bentoStates()
		)
				.describedAs("future JSON schema version validation")
				.isInstanceOf(BentoStateException.class)
				.hasMessageContaining("Unsupported BentoFX docking layout schema version");
	}

	@Test
	void decodeReportsMissingBentoIdentifierAsBentoStateException() {
		final JsonLayoutCodec codec = new JsonLayoutCodec();
		final String json = """
				{
				  "metadata": {
					"schemaVersion": %d
				  },
				  "bentos": [ { "rootBranches": [] } ]
				}
				""".formatted(DockingLayoutDto.getCurrentSchemaVersion());

		assertThatThrownBy(() ->
				codec.decode(new ByteArrayInputStream(
						json.getBytes(StandardCharsets.UTF_8)))
						.bentoStates()
		)
				.describedAs("missing Bento identifier validation")
				.isInstanceOf(BentoStateException.class)
				.hasMessageContaining("no identifier");
	}

	@Test
	void decodeReportsAContainerWithNoIdentifier() {
		final JsonLayoutCodec codec = new JsonLayoutCodec();

		// Two anonymous siblings used to take their element's name and collide
		// with one another.
		final String json = """
				{
				  "metadata": {
					"schemaVersion": %d
				  },
				  "bentos": [ {
					"identifier": "bento-1",
					"rootBranches": [ {
					  "childDockContainers": [
						{ "type": "leaf" },
						{ "type": "leaf" }
					  ]
					} ]
				  } ]
				}
				""".formatted(DockingLayoutDto.getCurrentSchemaVersion());

		assertThatThrownBy(() ->
				codec.decode(new ByteArrayInputStream(
						json.getBytes(StandardCharsets.UTF_8)))
						.bentoStates()
		)
				.describedAs("missing container identifier validation")
				.isInstanceOf(BentoStateException.class)
				.hasMessageContaining("no identifier");
	}

	@Test
	void decodeWrapsAnUncheckedFailureAsBentoStateException() {
		final JsonLayoutCodec codec = new JsonLayoutCodec();

		// A null Bento list, which Jackson maps straight onto the DTO field and
		// the mapper then iterates.
		final String json = """
				{
				  "metadata": {
					"schemaVersion": %d
				  },
				  "bentos": null
				}
				""".formatted(DockingLayoutDto.getCurrentSchemaVersion());

		assertThatThrownBy(() ->
				codec.decode(new ByteArrayInputStream(
						json.getBytes(StandardCharsets.UTF_8)))
						.bentoStates()
		)
				.describedAs("unchecked decode failure reporting")
				.isInstanceOf(BentoStateException.class)
				.hasMessageContaining("Failed to decode layout from JSON")
				.cause()
				.isInstanceOf(NullPointerException.class);
	}

	/**
	 * A layout written by a later version that added an optional property still
	 * loads, rather than failing and being replaced by the default layout.
	 */
	@Test
	void decodeIgnoresUnrecognizedProperties() throws Exception {
		final JsonLayoutCodec codec = new JsonLayoutCodec();
		final List<BentoState> original = createBentoStates();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		codec.encode(PersistableLayout.of(original), out);

		final String encoded = out.toString(StandardCharsets.UTF_8);
		final String withUnknownProperties = encoded
				.replaceFirst(
						"\"metadata\"\\s*:",
						"\"unexpected\" : { \"nested\" : [ 1, 2 ] }, $0"
				)
				.replaceFirst(
						"\"metadata\"\\s*:\\s*\\{",
						"$0 \"unexpected\" : true,"
				);

		assertThat(withUnknownProperties)
				.describedAs("JSON with unrecognized properties added")
				.contains("\"nested\"", "\"unexpected\" : true");

		final List<BentoState> restored = codec.decode(
				new ByteArrayInputStream(
						withUnknownProperties.getBytes(StandardCharsets.UTF_8))
		).bentoStates();

		assertThat(restored)
				.describedAs("layout restored from JSON with unrecognized properties")
				.usingRecursiveComparison()
				.isEqualTo(original);
	}

	@Test
	void encodeThenDecodeRoundTripsTheWholeLayout() throws Exception {
		// This codec cannot require a started JavaFX runtime: an external tool
		// converting layouts from another docking framework has none.
		assertThatThrownBy(() -> Platform.runLater(() -> { }))
				.describedAs("JavaFX runtime state for this suite")
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Toolkit not initialized");

		final JsonLayoutCodec codec = new JsonLayoutCodec();
		final List<BentoState> original = createBentoStates();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		codec.encode(PersistableLayout.of(original), out);

		final List<BentoState> restored = codec.decode(
				new ByteArrayInputStream(out.toByteArray())
		).bentoStates();

		assertThat(restored)
				.describedAs("layout restored from JSON")
				.usingRecursiveComparison()
				.isEqualTo(original);
	}

	@Test
	void encodeThenDecodeRoundTripsTheDisplayName() throws Exception {
		final JsonLayoutCodec codec = new JsonLayoutCodec();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		codec.encode(
				new PersistableLayout("Multi-Monitor", createBentoStates()),
				out
		);

		assertThat(codec.decode(new ByteArrayInputStream(out.toByteArray()))
				.displayName())
				.describedAs("display name restored from JSON")
				.isEqualTo("Multi-Monitor");
	}

	@Test
	void aLayoutWithNoDisplayNameRoundTripsWithoutOne() throws Exception {
		final JsonLayoutCodec codec = new JsonLayoutCodec();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		codec.encode(PersistableLayout.of(createBentoStates()), out);

		assertThat(codec.decode(new ByteArrayInputStream(out.toByteArray()))
				.displayName())
				.describedAs("display name from a layout saved without one")
				.isNull();
	}

	/**
	 * The group is metadata of its own, so a display name holding the character
	 * that used to separate the two is nothing but a name.
	 */
	@Test
	void encodeThenDecodeRoundTripsTheGroupSeparatelyFromTheDisplayName()
			throws Exception {
		final JsonLayoutCodec codec = new JsonLayoutCodec();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		codec.encode(
				new PersistableLayout(
						"TCP/IP Debug",
						createBentoStates(),
						"Debugging",
						List.of()
				),
				out
		);

		final PersistableLayout restored =
				codec.decode(new ByteArrayInputStream(out.toByteArray()));

		assertThat(restored.displayName())
				.describedAs("display name restored from JSON")
				.isEqualTo("TCP/IP Debug");
		assertThat(restored.group())
				.describedAs("group restored from JSON")
				.isEqualTo("Debugging");
	}

	@Test
	void aLayoutInNoGroupWritesNoGroupAtAll() throws Exception {
		final JsonLayoutCodec codec = new JsonLayoutCodec();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		codec.encode(PersistableLayout.of(createBentoStates()), out);

		assertThat(out.toString(StandardCharsets.UTF_8))
				.describedAs("JSON for a layout in no group")
				.doesNotContain("\"group\"")
				.doesNotContain("\"groups\"");
		assertThat(codec.decode(new ByteArrayInputStream(out.toByteArray()))
				.group())
				.describedAs("group from a layout saved without one")
				.isNull();
	}

	@Test
	void encodeThenDecodeRoundTripsTheGroupCatalog() throws Exception {
		final JsonLayoutCodec codec = new JsonLayoutCodec();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		codec.encode(
				PersistableLayout.ofGroups(List.of("Debugging", "Presentation")),
				out
		);

		final PersistableLayout restored =
				codec.decode(new ByteArrayInputStream(out.toByteArray()));

		assertThat(restored.groups())
				.describedAs("group catalog restored from JSON")
				.containsExactly("Debugging", "Presentation");
		assertThat(restored.bentoStates())
				.describedAs("state in a group catalog")
				.isEmpty();
	}

	/**
	 * A layout written before groups existed declares schema version 1 and
	 * carries neither field. It has to keep restoring, which is the whole reason
	 * the two are optional.
	 */
	@Test
	void decodesASchemaVersionOneLayoutAsUngrouped() throws Exception {
		final String versionOneLayout = """
				{
				  "metadata" : {
					"schemaVersion" : 1,
					"displayName" : "Multi-Monitor"
				  },
				  "bentos" : [ ]
				}
				""";

		final PersistableLayout restored = new JsonLayoutCodec().decode(
				new ByteArrayInputStream(
						versionOneLayout.getBytes(StandardCharsets.UTF_8)
				)
		);

		assertThat(restored.displayName())
				.describedAs("display name from a version 1 layout")
				.isEqualTo("Multi-Monitor");
		assertThat(restored.group())
				.describedAs("group from a version 1 layout")
				.isNull();
		assertThat(restored.groups())
				.describedAs("catalog from a version 1 layout")
				.isEmpty();
	}

	@Test
	void encodeThenDecodePreservesMixedRootChildOrder() throws Exception {
		final JsonLayoutCodec codec = new JsonLayoutCodec();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		codec.encode(PersistableLayout.of(mixedRootStates()), out);

		final List<BentoState> restored = codec.decode(
				new ByteArrayInputStream(out.toByteArray())
		).bentoStates();

		assertThat(restored.getFirst()
				.getRootBranchStates().getFirst()
				.getChildDockContainerStates())
				.describedAs("decoded root branch child dock containers, in order")
				.extracting(DockContainerState::getIdentifier)
				.containsExactly("leaf-A", "branch-B", "leaf-C");
	}

	private static List<BentoState> createStates() throws BentoStateException {
		return BentoStateMapper.fromDto(createDockingLayoutDto()).bentoStates();
	}

	/**
	 * {@return one Bento holding an empty root branch and nothing else.}
	 */
	private static List<BentoState> emptyRootStates() {
		return List.of(
				new BentoState.BentoStateBuilder("bento-1")
						.addRootBranchState(
								new DockContainerRootBranchState
										.DockContainerRootBranchStateBuilder("root-1")
										.build()
						)
						.build()
		);
	}

	/**
	 * {@return one Bento whose root branch has five divider positions, added in
	 * an order that is neither ascending nor descending.}
	 */
	private static List<BentoState> scrambledDividerStates() {
		final DockContainerRootBranchState rootState =
				new DockContainerRootBranchState.DockContainerRootBranchStateBuilder("root-1")
						.addDividerPosition(3, 0.4)
						.addDividerPosition(0, 0.1)
						.addDividerPosition(4, 0.5)
						.addDividerPosition(1, 0.2)
						.addDividerPosition(2, 0.3)
						.build();

		return List.of(
				new BentoState.BentoStateBuilder("bento-1")
						.addRootBranchState(rootState)
						.build()
		);
	}

	/**
	 * {@return one Bento whose root branch holds a leaf, a branch, and a second
	 * leaf, in that order.}
	 */
	private static List<BentoState> mixedRootStates() {
		final DockContainerRootBranchState rootState =
				new DockContainerRootBranchState.DockContainerRootBranchStateBuilder("root-1")
						.addDockContainerState(
								new DockContainerLeafState.DockContainerLeafStateBuilder("leaf-A").build())
						.addDockContainerState(
								new DockContainerBranchState.DockContainerBranchStateBuilder("branch-B").build())
						.addDockContainerState(
								new DockContainerLeafState.DockContainerLeafStateBuilder("leaf-C").build())
						.build();

		return List.of(
				new BentoState.BentoStateBuilder("bento-1")
						.addRootBranchState(rootState)
						.build()
		);
	}
}
