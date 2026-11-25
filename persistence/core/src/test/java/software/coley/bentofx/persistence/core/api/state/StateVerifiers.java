package software.coley.bentofx.persistence.core.api.state;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.api.ConfiguredEqualsVerifier;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState.DockContainerLeafStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState.DockContainerRootBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockableState.DockableStateBuilder;

import static javafx.geometry.Side.BOTTOM;
import static javafx.geometry.Side.TOP;

/**
 * Shared EqualsVerifier configuration for the state types in this package.
 *
 * <p>Only configuration that {@link ConfiguredEqualsVerifier} actually offers lives
 * here. Per-class options such as {@code withNonnullFields} and
 * {@code withPrefabValuesForField} exist only on
 * {@code SingleTypeEqualsVerifierApi}, which is what {@code forClass} returns, so
 * each test applies those itself after calling {@link #configured()}.</p>
 */
final class StateVerifiers {

	private StateVerifiers() {
		throw new UnsupportedOperationException(
				"Utility classes should not be instantiated."
		);
	}

	/**
	 * {@return EqualsVerifier configured the way every state type in this package
	 * needs.}
	 *
	 * <p>{@code usingGetClass} is the significant one: these types compare by exact
	 * runtime type rather than {@code instanceof}, so without it EqualsVerifier
	 * would test for the {@code instanceof} contract and report the deliberate
	 * behaviour as a defect.</p>
	 *
	 * <p>Prefab values cover the nested state types EqualsVerifier cannot build for
	 * itself: {@link DockContainerState}, which is {@code abstract sealed} and
	 * appears as a list element type; {@link DockContainerRootBranchState}, which
	 * {@link DragDropStageState} nests; and {@link DockableState}, whose
	 * {@code javafx.scene.Node} field otherwise sends EqualsVerifier recursing
	 * through the JavaFX scene-graph object graph until it gives up. A test whose
	 * subject <em>is</em> {@link DockableState} cannot lean on that prefab and has to
	 * supply a {@code Node} value itself.</p>
	 */
	static ConfiguredEqualsVerifier configured() {
		return EqualsVerifier
				.configure()
				.usingGetClass()
				.withPrefabValues(
						DockContainerState.class,
						leafState("prefab:leaf:red", TOP),
						leafState("prefab:leaf:black", BOTTOM)
				)
				.withPrefabValues(
						DockContainerRootBranchState.class,
						rootBranchState("prefab:root:red"),
						rootBranchState("prefab:root:black")
				)
				.withPrefabValues(
						DockableState.class,
						dockableState("prefab:dockable:red"),
						dockableState("prefab:dockable:black")
				);
	}

	private static DockContainerLeafState leafState(
			final String identifier,
			final javafx.geometry.Side side
	) {
		return new DockContainerLeafStateBuilder(identifier)
				.setSide(side)
				.build();
	}

	private static DockContainerRootBranchState rootBranchState(
			final String identifier
	) {
		return new DockContainerRootBranchStateBuilder(identifier).build();
	}

	private static DockableState dockableState(final String identifier) {
		return new DockableStateBuilder(identifier).build();
	}
}
