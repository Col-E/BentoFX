package software.coley.bentofx.persistence.core.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DockingLayoutBuilderTest {

	private static final String FIRST_BENTO_IDENTIFIER = "bento-1";
	private static final String SECOND_BENTO_IDENTIFIER = "bento-2";

	private static final String LAYOUT_GET_BENTOLAYOUTS_DESCRIPTION = "layout.getBentoLayouts()";

	@Test
	void dockingLayoutExposesImmutableSnapshotOfBuiltLayouts() {
		BentoLayout first = emptyBentoLayout(FIRST_BENTO_IDENTIFIER);
		BentoLayout second = emptyBentoLayout(SECOND_BENTO_IDENTIFIER);

		DockingLayout layout = new DockingLayout.DockingLayoutBuilder()
				.addBentoLayout(first)
				.addBentoLayout(second)
				.build();

		assertThat(layout.getBentoLayouts())
				.describedAs(LAYOUT_GET_BENTOLAYOUTS_DESCRIPTION)
				.extracting(BentoLayout::getIdentifier)
				.containsExactly(FIRST_BENTO_IDENTIFIER, SECOND_BENTO_IDENTIFIER);

		final List<BentoLayout> bentoLayouts = layout.getBentoLayouts();
		assertThatThrownBy(() -> bentoLayouts.add(first))
				.describedAs("exception thrown by () -> bentoLayouts.add(first)")
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void builtLayoutIsNotAffectedByLaterBuilderMutation() {
		BentoLayout first = emptyBentoLayout(FIRST_BENTO_IDENTIFIER);
		BentoLayout second = emptyBentoLayout(SECOND_BENTO_IDENTIFIER);

		DockingLayout.DockingLayoutBuilder builder =
				new DockingLayout.DockingLayoutBuilder()
						.addBentoLayout(first);

		DockingLayout layout = builder.build();

		builder.addBentoLayout(second);

		assertThat(layout.getBentoLayouts())
				.describedAs(LAYOUT_GET_BENTOLAYOUTS_DESCRIPTION)
				.containsExactly(first);
	}

	/**
	 * These tests are about {@link DockingLayout}, and only need a
	 * {@link BentoLayout} with an identifier to put in one. Built through the
	 * public builder rather than by reflecting on the private constructor, which
	 * is how they were written: that made them break whenever a field was added
	 * to {@link BentoLayout}, with a {@code NoSuchMethodException} pointing at the
	 * fixture instead of at anything under test.
	 */
	private static BentoLayout emptyBentoLayout(final String bentoIdentifier) {
		return new BentoLayout.BentoLayoutBuilder(bentoIdentifier).build();
	}
}
