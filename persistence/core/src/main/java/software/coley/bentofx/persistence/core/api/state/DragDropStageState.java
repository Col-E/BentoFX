package software.coley.bentofx.persistence.core.api.state;

import javafx.stage.Modality;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Represents the layout state of a {@code DragDropStage}.
 *
 * @author Phil Bryant
 */
public class DragDropStageState {

	private final @Nullable String title;
	private final @Nullable Double x;
	private final @Nullable Double y;
	private final @Nullable Double width;
	private final @Nullable Double height;
	private final @Nullable Modality modality;
	private final @Nullable Double opacity;
	private final @Nullable Boolean isIconified;
	private final @Nullable Boolean isFullScreen;
	private final @Nullable Boolean isMaximized;
	private final @Nullable Boolean isAlwaysOnTop;
	private final @Nullable Boolean isResizable;
	private final @Nullable Boolean isShowing;
	private final @Nullable Boolean isFocused;
	private final Boolean isAutoClosedWhenEmpty;
	private final @Nullable DockContainerRootBranchState dockContainerRootBranchState;

	// Ignore the number or constructor parameters; this is a read-only class
	// whose member attributes must be set using the constructor.
	@SuppressWarnings("java:S107")
	private DragDropStageState(
			final @Nullable String title,
			final @Nullable Double x,
			final @Nullable Double y,
			final @Nullable Double width,
			final @Nullable Double height,
			final @Nullable Modality modality,
			final @Nullable Double opacity,
			final @Nullable Boolean isIconified,
			final @Nullable Boolean isFullScreen,
			final @Nullable Boolean isMaximized,
			final @Nullable Boolean isAlwaysOnTop,
			final @Nullable Boolean isResizable,
			final @Nullable Boolean isShowing,
			final @Nullable Boolean isFocused,
			final Boolean isAutoClosedWhenEmpty,
			final @Nullable DockContainerRootBranchState dockContainerRootBranchState
	) {
		this.title = title;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.modality = modality;
		this.opacity = opacity;
		this.isIconified = isIconified;
		this.isFullScreen = isFullScreen;
		this.isMaximized = isMaximized;
		this.isAlwaysOnTop = isAlwaysOnTop;
		this.isResizable = isResizable;
		this.isShowing = isShowing;
		this.isFocused = isFocused;
		this.isAutoClosedWhenEmpty = requireNonNull(isAutoClosedWhenEmpty);
		this.dockContainerRootBranchState = dockContainerRootBranchState;
	}

	/**
	 * {@return an {@link Optional} containing the stage title, an empty
	 * {@link Optional} when the title was not specified.}
	 */
	public Optional<String> getTitle() {
		return Optional.ofNullable(title);
	}

	/**
	 * {@return an {@link Optional} containing the stage x-coordinate, an empty
	 * {@link Optional} when the x-coordinate was not specified.}
	 */
	public Optional<Double> getX() {
		return Optional.ofNullable(x);
	}

	/**
	 * {@return an {@link Optional} containing the stage y-coordinate, an empty
	 * {@link Optional} when the y-coordinate was not specified.}
	 */
	public Optional<Double> getY() {
		return Optional.ofNullable(y);
	}

	/**
	 * {@return an {@link Optional} containing the stage x-width, an empty
	 * {@link Optional} when the width was not specified.}
	 */
	public Optional<Double> getWidth() {
		return Optional.ofNullable(width);
	}

	/**
	 * {@return an {@link Optional} containing the stage height, an empty
	 * {@link Optional} when the height was not specified.}
	 */
	public Optional<Double> getHeight() {
		return Optional.ofNullable(height);
	}

	/**
	 * {@return an {@link Optional} containing the stage {@link Modality}, an
	 * empty {@link Optional} when the {@link Modality} was not specified.}
	 */
	public Optional<Modality> getModality() {
		return Optional.ofNullable(modality);
	}

	/**
	 * {@return an {@link Optional} containing the stage opacity, an empty
	 * {@link Optional} when the opacity was not specified.}
	 */
	public Optional<Double> getOpacity() {
		return Optional.ofNullable(opacity);
	}

	/**
	 * {@return an {@link Optional} specifying whether the stage should be
	 * iconified, an empty {@link Optional} when iconification was not
	 * specified.}
	 */
	public Optional<Boolean> isIconified() {
		return Optional.ofNullable(isIconified);
	}

	/**
	 * {@return an {@link Optional} specifying whether the stage should be
	 * shown full screen, an empty {@link Optional} when unspecified.}
	 */
	public Optional<Boolean> isFullScreen() {
		return Optional.ofNullable(isFullScreen);
	}

	/**
	 * {@return an {@link Optional} specifying whether the stage should be
	 * shown maximized, an empty {@link Optional} when unspecified.}
	 */
	public Optional<Boolean> isMaximized() {
		return Optional.ofNullable(isMaximized);
	}

	/**
	 * {@return an {@link Optional} specifying whether the stage should be
	 * shown on top, an empty {@link Optional} when unspecified.}
	 */
	public Optional<Boolean> isAlwaysOnTop() {
		return Optional.ofNullable(isAlwaysOnTop);
	}

	/**
	 * {@return an {@link Optional} specifying whether the stage should be
	 * resizable, an empty {@link Optional} when unspecified.}
	 */
	public Optional<Boolean> isResizable() {
		return Optional.ofNullable(isResizable);
	}

	/**
	 * {@return an {@link Optional} specifying whether the stage should be
	 * shown, an empty {@link Optional} when unspecified.}
	 */
	public Optional<Boolean> isShowing() {
		return Optional.ofNullable(isShowing);
	}

	/**
	 * {@return an {@link Optional} specifying whether the stage should be
	 * focused, an empty {@link Optional} when unspecified.}
	 */
	public Optional<Boolean> isFocused() {
		return Optional.ofNullable(isFocused);
	}

	/**
	 * {@return whether the stage should close automatically when empty.}
	 *
	 * <p>Mandatory field and is {@code requireNonNull}-checked, so it has
	 * no "unspecified" case to represent.</p>
	 */
	public Boolean isAutoClosedWhenEmpty() {
		return isAutoClosedWhenEmpty;
	}

	/**
	 * {@return an {@link Optional} containing the root branch state for the
	 * drag/drop stage, and empty {@link Optional} when unspecified.}
	 */
	public Optional<DockContainerRootBranchState> getDockContainerRootBranchState() {
		return Optional.ofNullable(dockContainerRootBranchState);
	}

	/**
	 * Compares this state to another by exact runtime type and by every
	 * persisted field, including the nested root branch state.
	 *
	 * <p>Two states are equal when they would restore the same stage and the
	 * same layout inside it, which makes this the check for "did the layout
	 * actually change" without encoding either side.</p>
	 *
	 * <p>Exact runtime type rather than {@code instanceof}, because this class
	 * is not {@code final} and comparing exact types keeps the relation
	 * symmetric and transitive against a subclass that adds fields.</p>
	 *
	 * <p>Callable from any thread: this state is immutable and holds no
	 * scene-graph reference.</p>
	 *
	 * @param o the object to compare against, may be {@code null}.
	 *
	 * @return {@code true} when {@code o} has exactly this runtime type and
	 * equal values for every persisted field.
	 */
	// EqualsGetClass: deliberate, for the reason given above - this class is not
	// final, and instanceof cannot keep the relation symmetric against a subclass
	// that adds fields.
	@SuppressWarnings("EqualsGetClass")
	@Override
	public boolean equals(final @Nullable Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final DragDropStageState that = (DragDropStageState) o;

		return modality == that.modality
				&& isAutoClosedWhenEmpty.equals(that.isAutoClosedWhenEmpty)
				&& Objects.equals(title, that.title)
				&& Objects.equals(x, that.x)
				&& Objects.equals(y, that.y)
				&& Objects.equals(width, that.width)
				&& Objects.equals(height, that.height)
				&& Objects.equals(opacity, that.opacity)
				&& Objects.equals(isIconified, that.isIconified)
				&& Objects.equals(isFullScreen, that.isFullScreen)
				&& Objects.equals(isMaximized, that.isMaximized)
				&& Objects.equals(isAlwaysOnTop, that.isAlwaysOnTop)
				&& Objects.equals(isResizable, that.isResizable)
				&& Objects.equals(isShowing, that.isShowing)
				&& Objects.equals(isFocused, that.isFocused)
				&& Objects.equals(
						dockContainerRootBranchState,
						that.dockContainerRootBranchState
				);
	}

	/**
	 * {@return a hash code consistent with {@link #equals(Object)}.}
	 */
	@Override
	public int hashCode() {
		return Objects.hash(
				title,
				x,
				y,
				width,
				height,
				modality,
				opacity,
				isIconified,
				isFullScreen,
				isMaximized,
				isAlwaysOnTop,
				isResizable,
				isShowing,
				isFocused,
				isAutoClosedWhenEmpty,
				dockContainerRootBranchState
		);
	}

	/**
	 * Builds a {@link DragDropStageState}.
	 */
	public static class DragDropStageStateBuilder {

		private final Boolean isAutoClosedWhenEmpty;
		private @Nullable DockContainerRootBranchState dockContainerRootBranchState;
		private @Nullable String title;
		private @Nullable Double x;
		private @Nullable Double y;
		private @Nullable Double width;
		private @Nullable Double height;
		private @Nullable Modality modality;
		private @Nullable Double opacity;
		private @Nullable Boolean isIconified;
		private @Nullable Boolean isFullScreen;
		private @Nullable Boolean isMaximized;
		private @Nullable Boolean isAlwaysOnTop;
		private @Nullable Boolean isResizable;
		private @Nullable Boolean isShowing;
		private @Nullable Boolean isFocused;

		/**
		 * Constructor.
		 * @param isAutoClosedWhenEmpty whether the stage closes itself once it
		 * holds no {@code Dockable}s. Required, unlike every other property on
		 * this builder.
		 */
		public DragDropStageStateBuilder(
				final Boolean isAutoClosedWhenEmpty
		) {
			this.isAutoClosedWhenEmpty =
					requireNonNull(isAutoClosedWhenEmpty);
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param dockContainerRootBranchState the value to persist, {@code null}
		 * leaves the root branch state unspecified.
		 */
		public DragDropStageStateBuilder setDockContainerRootBranchState(
				final @Nullable DockContainerRootBranchState dockContainerRootBranchState
		) {
			this.dockContainerRootBranchState = dockContainerRootBranchState;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param title the value to persist, {@code null} leaves the title
		 * unspecified.
		 */
		public DragDropStageStateBuilder setTitle(
				final @Nullable String title
		) {
			this.title = title;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param x the value to persist, {@code null} leaves the x-coordinate
		 * unspecified.
		 */
		public DragDropStageStateBuilder setX(
				final @Nullable Double x
		) {
			this.x = x;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param y the value to persist, {@code null} leaves the y-coordinate
		 * unspecified.
		 */
		public DragDropStageStateBuilder setY(
				final @Nullable Double y
		) {
			this.y = y;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param width the value to persist, {@code null} leaves the width
		 * unspecified.
		 */
		public DragDropStageStateBuilder setWidth(
				final @Nullable Double width
		) {
			this.width = width;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param height the value to persist, {@code null} leaves the height
		 * unspecified.
		 */
		public DragDropStageStateBuilder setHeight(
				final @Nullable Double height
		) {
			this.height = height;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param modality the value to persist, {@code null} leaves the
		 * {@link Modality} unspecified.
		 */
		public DragDropStageStateBuilder setModality(
				final @Nullable Modality modality
		) {
			this.modality = modality;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param opacity the value to persist, {@code null} leaves the opacity
		 * unspecified.
		 */
		public DragDropStageStateBuilder setOpacity(
				final @Nullable Double opacity
		) {
			this.opacity = opacity;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param isIconified the value to persist, {@code null} leaves the
		 * iconification unspecified.
		 */
		public DragDropStageStateBuilder setIconified(
				final @Nullable Boolean isIconified
		) {
			this.isIconified = isIconified;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param isFullScreen the value to persist, {@code null} leaves the
		 * screen fullness unspecified.
		 */
		public DragDropStageStateBuilder setFullScreen(
				final @Nullable Boolean isFullScreen
		) {
			this.isFullScreen = isFullScreen;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param isMaximized the value to persist, {@code null} leaves
		 * maximization unspecified.
		 */
		public DragDropStageStateBuilder setMaximized(
				final @Nullable Boolean isMaximized
		) {
			this.isMaximized = isMaximized;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param isAlwaysOnTop the value to persist, {@code null} leaves the
		 * being on top unspecified.
		 */
		public DragDropStageStateBuilder setAlwaysOnTop(
				final @Nullable Boolean isAlwaysOnTop
		) {
			this.isAlwaysOnTop = isAlwaysOnTop;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param isResizable the value to persist, {@code null} leaves the
		 * resizability unspecified.
		 */
		public DragDropStageStateBuilder setResizable(
				final @Nullable Boolean isResizable
		) {
			this.isResizable = isResizable;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param isShowing the value to persist, {@code null} leaves showing
		 * unspecified.
		 */
		public DragDropStageStateBuilder setShowing(
				final @Nullable Boolean isShowing
		) {
			this.isShowing = isShowing;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param isFocused the value to persist, {@code null} to leave the
		 * focused state unspecified.
		 */
		public DragDropStageStateBuilder setFocused(
				final @Nullable Boolean isFocused
		) {
			this.isFocused = isFocused;
			return this;
		}

		/**
		 * {@return the {@link DragDropStageState} built from this builder.}
		 */
		public DragDropStageState build() {
			return new DragDropStageState(
					title,
					x,
					y,
					width,
					height,
					modality,
					opacity,
					isIconified,
					isFullScreen,
					isMaximized,
					isAlwaysOnTop,
					isResizable,
					isShowing,
					isFocused,
					isAutoClosedWhenEmpty,
					dockContainerRootBranchState
			);
		}
	}
}
