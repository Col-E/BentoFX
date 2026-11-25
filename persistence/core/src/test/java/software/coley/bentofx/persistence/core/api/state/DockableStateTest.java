package software.coley.bentofx.persistence.core.api.state;

import javafx.scene.Group;
import javafx.scene.Node;
import org.junit.jupiter.api.Test;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.persistence.core.api.state.DockableState.DockableStateBuilder;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Equality for {@link DockableState}, whose node and functional fields have no value
 * equality of their own.
 */
class DockableStateTest {

	@Test
	void dockableStateHonoursTheEqualsContract() {
		StateVerifiers.configured()
				.forClass(DockableState.class)
				// EqualsVerifier has to build the node field itself here, and left to
				// its own devices it recurses through the JavaFX scene-graph object
				// graph. Group is the cheapest Node that constructs without a
				// started toolkit.
				.withPrefabValues(Node.class, new Group(), new Group())
				.withNonnullFields("identifier")
				.verify();
	}

	@Test
	void functionalFieldsCompareByIdentity() {
		final Consumer<Dockable> sharedConsumer = dockable -> {
			// Intentionally empty: only this lambda's identity matters here.
		};

		final DockableState first = new DockableStateBuilder("dockable-1")
				.setTitle("Explorer")
				.setDockableConsumer(sharedConsumer)
				.build();

		final DockableState sameConsumer = new DockableStateBuilder("dockable-1")
				.setTitle("Explorer")
				.setDockableConsumer(sharedConsumer)
				.build();

		final DockableState equivalentButDistinctConsumer =
				new DockableStateBuilder("dockable-1")
						.setTitle("Explorer")
						.setDockableConsumer(dockable -> {
							// Behaves like sharedConsumer but is a separate object.
						})
						.build();

		assertThat(first)
				.describedAs("dockable states sharing one consumer instance")
				.isEqualTo(sameConsumer)
				.hasSameHashCodeAs(sameConsumer)
				.describedAs("dockable states with equivalent but distinct consumers")
				.isNotEqualTo(equivalentButDistinctConsumer);
	}

	@Test
	void capturedDockableStatesCompareByIdentifierAlone() {
		// What DefaultBentoStateCapturer actually produces: identifier only. Change
		// detection over captured layouts therefore never meets the
		// identity-compared fields.
		assertThat(new DockableStateBuilder("dockable-1").build())
				.describedAs("captured dockable state, same identifier")
				.isEqualTo(new DockableStateBuilder("dockable-1").build());

		assertThat(new DockableStateBuilder("dockable-1").build())
				.describedAs("captured dockable state, different identifier")
				.isNotEqualTo(new DockableStateBuilder("dockable-2").build());
	}
}
