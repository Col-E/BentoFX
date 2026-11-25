package software.coley.bentofx.persistence.core.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Defines the BentoFX docking layout for persistence.
 *
 * @author Phil Bryant
 */
public class DockingLayout {

	private final List<BentoLayout> bentoLayouts;

	private DockingLayout(
			final List<BentoLayout> bentoLayouts
	) {
		this.bentoLayouts = List.copyOf(bentoLayouts);
	}

	/**
	 * {@return an immutable {@link List} of the {@link BentoLayout}s making up
	 * this docking layout, one per {@code Bento}, in the order they were added.}
	 */
	public List<BentoLayout> getBentoLayouts() {
		return bentoLayouts;
	}


	/**
	 * Builds a {@link DockingLayout}.
	 */
	public static class DockingLayoutBuilder {

		private final List<BentoLayout> bentoLayouts =
				new ArrayList<>();

		/**
		 * {@return this builder for chaining method calls.}
		 * @param bentoLayout the {@link BentoLayout} to add.
		 */
		public DockingLayoutBuilder addBentoLayout(
				final BentoLayout bentoLayout
		) {
			bentoLayouts.add(Objects.requireNonNull(bentoLayout));
			return this;
		}

		/**
		 * {@return the {@link DockingLayout} built from this builder.}
		 */
		public DockingLayout build() {
			return new DockingLayout(bentoLayouts);
		}
	}
}
