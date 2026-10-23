package software.coley.bentofx.persistence.core.api.state;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState.DockContainerLeafStateBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Equality for {@link DockContainerLeafState}, including the fields it inherits from
 * {@link DockContainerState} and {@link IdentifiableState}.
 */
class DockContainerLeafStateTest {

	@Test
	void leafStateHonoursTheEqualsContract() {
		StateVerifiers.configured()
				.forClass(DockContainerLeafState.class)
				// Inherited, and never null: the constructors apply
				// Objects.requireNonNull and List.copyOf. Every field this class
				// declares is genuinely nullable and stays under null test.
				.withNonnullFields("identifier", "childDockableStates")
				.verify();
	}

	@Test
	void unspecifiedFieldIsNotEqualToExplicitlySetField() {
		final DockContainerLeafState unspecified =
				new DockContainerLeafStateBuilder("leaf-1").build();

		final DockContainerLeafState expanded =
				new DockContainerLeafStateBuilder("leaf-1")
						.setCollapsed(false)
						.build();

		// An absent value restores differently from a present false: the restorer
		// reads these through Optional and skips what is absent.
		assertThat(unspecified)
				.describedAs("leaf state with no collapsed value versus collapsed=false")
				.isNotEqualTo(expanded);
	}
}
