package software.coley.bentofx.persistence.core.api.codec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PersistableLayoutTest {

	@Test
	void findDisplayNameReturnsTheDisplayNameWhenPresent() {
		final PersistableLayout layout =
				new PersistableLayout("Multi-Monitor", List.of());

		assertThat(layout.findDisplayName())
				.describedAs("findDisplayName() with a display name")
				.contains("Multi-Monitor");
	}

	@Test
	void findDisplayNameIsEmptyWhenTheLayoutHasNoDisplayName() {
		final PersistableLayout layout = PersistableLayout.of(List.of());

		assertThat(layout.findDisplayName())
				.describedAs("findDisplayName() with no display name")
				.isEmpty();
	}
}
