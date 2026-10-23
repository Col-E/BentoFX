package software.coley.bentofx.persistence.core.api.state;

import javafx.geometry.Side;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Represents the layout state of a {@code DockContainerLeaf}.
 *
 * @author Phil Bryant
 */
public non-sealed class DockContainerLeafState extends DockContainerState {

	@Nullable
	private final Side side;
	@Nullable
	private final String selectedDockableIdentifier;
	@Nullable
	private final Boolean isResizableWithParent;
	@Nullable
	private final Boolean isCanSplit;
	@Nullable
	private final Double uncollapsedSizePx;
	@Nullable
	private final Boolean isCollapsed;

	// This is a read only class whose constructor is used to set all attributes.
	@SuppressWarnings("java:S107")
	private DockContainerLeafState(
			final String identifier,
			final @Nullable Boolean doPruneWhenEmpty,
			final List<DockableState> childDockableStates,
			final @Nullable Side side,
			final @Nullable String selectedDockableIdentifier,
			final @Nullable Boolean isResizableWithParent,
			final @Nullable Boolean isCanSplit,
			final @Nullable Double uncollapsedSizePx,
			final @Nullable Boolean isCollapsed
			) {
		super(
				identifier,
				doPruneWhenEmpty,
				childDockableStates
		);
		this.side = side;
		this.selectedDockableIdentifier = selectedDockableIdentifier;
		this.isResizableWithParent = isResizableWithParent;
		this.isCanSplit = isCanSplit;
		this.uncollapsedSizePx = uncollapsedSizePx;
		this.isCollapsed = isCollapsed;
	}

	/**
	 * {@return an {@link Optional} containing the {@link Side} the leaf displays
	 * its headers on, an empty {@link Optional} when the {@link Side} has not
	 * been specified or the leaf displays no headers.}
	 */
	public Optional<Side> getSide() {
		return Optional.ofNullable(side);
	}

	/**
	 * {@return an {@link Optional} containing the identifier of the selected
	 * {@code Dockable}, an empty {@link Optional} when no selection was
	 * specified.}
	 */
	public Optional<String> getSelectedDockableIdentifier() {
		return Optional.ofNullable(selectedDockableIdentifier);
	}

	/**
	 * {@return an {@link Optional} containing whether the leaf resizes with its
	 * parent {@code SplitPane}, an empty {@link Optional} when unspecified.}
	 */
	public Optional<Boolean> isResizableWithParent() {
		return Optional.ofNullable(isResizableWithParent);
	}

	/**
	 * {@return an {@link Optional} containing whether the leaf may be split, an
	 * empty {@link Optional} when unspecified.}
	 */
	public Optional<Boolean> isCanSplit() {
		return Optional.ofNullable(isCanSplit);
	}

	/**
	 * {@return an {@link Optional} containing the size in pixels the leaf
	 * returns to when it is expanded, an empty {@link Optional} when
	 * unspecified.}
	 *
	 * <p>Only meaningful for a leaf that displays headers: a leaf with no
	 * {@link Side} cannot be collapsed, so it has no uncollapsed size to
	 * record.</p>
	 */
	public Optional<Double> getUncollapsedSizePx() {
		return Optional.ofNullable(uncollapsedSizePx);
	}

	/**
	 * {@return an {@link Optional} containing whether the leaf is collapsed, an
	 * empty {@link Optional} when unspecified.}
	 */
	public Optional<Boolean> isCollapsed() {
		return Optional.ofNullable(isCollapsed);
	}

	/**
	 * Extends {@link DockContainerState#equals(Object)} with the side, the selected
	 * dockable identifier, the resize-with-parent and can-split flags, the
	 * uncollapsed size and the collapsed flag. See
	 * {@link IdentifiableState#equals(Object)} for the contract.
	 *
	 * @param o the object to compare against, may be {@code null}.
	 *
	 * @return {@code true} when {@code o} has exactly this runtime type and equal
	 * values for every persisted field.
	 */
	@Override
	public boolean equals(final @Nullable Object o) {
		if (this == o) {
			return true;
		}

		// The instanceof narrows the type for the compiler; super.equals settles the
		// exact-runtime-type check documented on IdentifiableState.equals.
		if (!(o instanceof final DockContainerLeafState that)
				|| !super.equals(o)) {
			return false;
		}

		return side == that.side
				&& Objects.equals(
						selectedDockableIdentifier,
						that.selectedDockableIdentifier
				)
				&& Objects.equals(isResizableWithParent, that.isResizableWithParent)
				&& Objects.equals(isCanSplit, that.isCanSplit)
				&& Objects.equals(uncollapsedSizePx, that.uncollapsedSizePx)
				&& Objects.equals(isCollapsed, that.isCollapsed);
	}

	/**
	 * {@return a hash code consistent with {@link #equals(Object)}.}
	 */
	@Override
	public int hashCode() {
		return Objects.hash(
				super.hashCode(),
				side,
				selectedDockableIdentifier,
				isResizableWithParent,
				isCanSplit,
				uncollapsedSizePx,
				isCollapsed
		);
	}

	/**
	 * Builds a {@link DockContainerLeafState}.
	 */
	public static class DockContainerLeafStateBuilder {

		private final String identifier;
		private final List<DockableState> childDockableStates = new ArrayList<>();
		private @Nullable Boolean pruneWhenEmpty;
		private @Nullable Side side;
		private @Nullable String selectedDockableStateIdentifier;
		private @Nullable Boolean isResizableWithParent;
		private @Nullable Boolean isCanSplit;
		private @Nullable Double uncollapsedSizePx;
		private @Nullable Boolean isCollapsed;

		/**
		 * Constructor.
		 * @param identifier the {@code DockContainerLeaf} identifier.
		 */
		public DockContainerLeafStateBuilder(
				final String identifier
		) {
			this.identifier = requireNonNull(identifier);
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param dockableState the {@link DockableState} to add.
		 */
		public DockContainerLeafStateBuilder addChildDockableState(final DockableState dockableState) {
			this.childDockableStates.add(requireNonNull(dockableState));
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param pruneWhenEmpty whether the leaf should be pruned when it holds
		 * no {@code Dockable}s, {@code null} leaves prune-when-empty
		 * unspecified.
		 */
		public DockContainerLeafStateBuilder setPruneWhenEmpty(final @Nullable Boolean pruneWhenEmpty) {
			this.pruneWhenEmpty = pruneWhenEmpty;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param side the {@link Side} the leaf displays its headers on,
		 * {@code null} leaves the {@link Side} unspecified.
		 */
		public DockContainerLeafStateBuilder setSide(final @Nullable Side side) {
			this.side = side;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param selectedDockableStateIdentifier the identifier of the selected
		 * {@code Dockable}, {@code null} leaves the selection unspecified.
		 */
		public DockContainerLeafStateBuilder setSelectedDockableStateIdentifier(
				final @Nullable String selectedDockableStateIdentifier
		) {
			this.selectedDockableStateIdentifier =
					selectedDockableStateIdentifier;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param isResizableWithParent whether the leaf resizes with its parent
		 * {@code SplitPane}, {@code null} leaves it unspecified.
		 */
		public DockContainerLeafStateBuilder setResizableWithParent(
				final @Nullable Boolean isResizableWithParent
		) {
			this.isResizableWithParent = isResizableWithParent;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param isCanSplit whether the leaf may be split, {@code null} leaves
		 * it unspecified.
		 */
		public DockContainerLeafStateBuilder setCanSplit(
				final @Nullable Boolean isCanSplit
		) {
			this.isCanSplit = isCanSplit;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param uncollapsedSizePx the size in pixels the leaf returns to when
		 * it is expanded, {@code null} leaves it unspecified. Only meaningful
		 * for a leaf that displays headers, since a leaf with no {@link Side}
		 * cannot be collapsed.
		 */
		public DockContainerLeafStateBuilder setUncollapsedSizePx(
				final @Nullable Double uncollapsedSizePx
		) {
			this.uncollapsedSizePx = uncollapsedSizePx;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param isCollapsed whether the leaf is collapsed, {@code null} leaves
		 * it unspecified.
		 */
		public DockContainerLeafStateBuilder setCollapsed(
				final @Nullable Boolean isCollapsed
		) {
			this.isCollapsed = isCollapsed;
			return this;
		}

		/**
		 * {@return the {@link DockContainerLeafState} built from this builder.}
		 */
		public DockContainerLeafState build() {

			return new DockContainerLeafState(
					identifier,
					pruneWhenEmpty,
					childDockableStates,
					side,
					selectedDockableStateIdentifier,
					isResizableWithParent,
					isCanSplit,
					uncollapsedSizePx,
					isCollapsed
			);
		}
	}
}
