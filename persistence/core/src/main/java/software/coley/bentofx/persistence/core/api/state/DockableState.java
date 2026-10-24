package software.coley.bentofx.persistence.core.api.state;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableIconFactory;
import software.coley.bentofx.dockable.DockableMenuFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Represents the properties and factories necessary for constructing a
 * {@code Dockable}.
 *
 * @author Phil Bryant
 */
public class DockableState extends IdentifiableState {

	private final @Nullable Node dockableNode;
	private final @Nullable String title;
	private final @Nullable String tooltip;
	private final @Nullable DockableIconFactory dockableIconFactory;
	private final @Nullable DockableMenuFactory dockableMenuFactory;
	private final @Nullable Integer dragGroupMask;
	private final @Nullable Boolean isClosable;
	private final @Nullable Consumer<Dockable> dockableConsumer;

	// This is a read-only class whose attributes are set using the constructor.
	@SuppressWarnings("java:S107")
	private DockableState(
			final String identifier,
			final @Nullable Node dockableNode,
			final @Nullable String title,
			final @Nullable String tooltip,
			final @Nullable DockableIconFactory dockableIconFactory,
			final @Nullable DockableMenuFactory dockableMenuFactory,
			final @Nullable Integer dragGroupMask,
			final @Nullable Boolean isClosable,
			final @Nullable Consumer<Dockable> dockableConsumer
	) {
		super(identifier);
		this.dockableNode = dockableNode;
		this.title = title;
		this.tooltip = tooltip;
		this.dockableIconFactory = dockableIconFactory;
		this.dockableMenuFactory = dockableMenuFactory;
		this.dragGroupMask = dragGroupMask;
		this.isClosable = isClosable;
		this.dockableConsumer = dockableConsumer;
	}

	/**
	 * {@return an {@link Optional} containing the {@link Node} to display when
	 * the dockable is selected, an empty {@code Optional} when no
	 * {@link Node} was provided when building this {@link DockableState}.}
	 */
	public Optional<Node> getDockableNode() {
		return Optional.ofNullable(dockableNode);
	}

	/**
	 * {@return an {@link Optional} containing the text for the
	 * {@link Dockable}'s {@code Header}, an empty {@link Optional} when no text
	 * was provided when building this {@link DockableState}.}
	 */
	public Optional<String> getTitle() {
		return Optional.ofNullable(title);
	}

	/**
	 * {@return an {@link Optional} containing the text to display when hovering
	 * the mouse over the {@link Dockable}'s {@code Header}, an empty
	 * {@link Optional} when no tooltip text was provided when building this
	 * {@link DockableState}.}
	 */
	public Optional<String> getTooltipText() {
		return Optional.ofNullable(tooltip);
	}

	/**
	 * {@return an {@link Optional} containing the {@link DockableIconFactory}
	 * for creating the {@link Node} graphic in the {@link Dockable}'s
	 * {@code Header}, an empty {@code Optional} when no
	 * {@link DockableIconFactory} was provided when building this
	 * {@link DockableState}.}
	 */
	public Optional<DockableIconFactory> getDockableIconFactory() {
		return Optional.ofNullable(dockableIconFactory);
	}

	/**
	 * {@return an {@link Optional} containing {@link DockableMenuFactory} for
	 * creating the context menu for a {@link Dockable}, an empty {@link Optional}
	 * when no {@link DockableMenuFactory} was provided when building this
	 * {@link DockableState}.}
	 */
	public Optional<DockableMenuFactory> getDockableMenuFactory() {
		return Optional.ofNullable(dockableMenuFactory);
	}

	/**
	 * {@return an {@link Optional} containing the drag group mask for the
	 * {@link Dockable}, an empty {@code Optional} when no drag drop group mask
	 * was provided when building this {@link DockableState}.}
	 */
	public Optional<Integer> getDragGroupMask() {
		return Optional.ofNullable(dragGroupMask);
	}

	/**
	 * {@return an {@link Optional} containing {@code true} if the dockable is
	 * closable, {@code false} if not, and an empty {@link Optional} if
	 * closability was not specified when building this {@link DockableState}.}
	 */
	public Optional<Boolean> isClosable() {
		return Optional.ofNullable(isClosable);
	}

	/**
	 * {@return an {@link Optional} containing the {@link Consumer} to call when
	 * the {@link Dockable} has been added to the docking layout, an empty
	 * {@code Optional} when no {@link Consumer} was provided when building this
	 * {@link DockableState}.}
	 */
	public Optional<Consumer<Dockable>> getDockableConsumer() {
		return Optional.ofNullable(dockableConsumer);
	}

	/**
	 * Extends {@link IdentifiableState#equals(Object)} with every field this class
	 * carries. See that method for the contract.
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
		if (!(o instanceof final DockableState that) || !super.equals(o)) {
			return false;
		}

		return Objects.equals(dockableNode, that.dockableNode)
				&& Objects.equals(dockableIconFactory, that.dockableIconFactory)
				&& Objects.equals(dockableMenuFactory, that.dockableMenuFactory)
				&& Objects.equals(dockableConsumer, that.dockableConsumer)
				&& Objects.equals(title, that.title)
				&& Objects.equals(tooltip, that.tooltip)
				&& Objects.equals(dragGroupMask, that.dragGroupMask)
				&& Objects.equals(isClosable, that.isClosable);
	}

	/**
	 * {@return a hash code consistent with {@link #equals(Object)}.}
	 */
	@Override
	public int hashCode() {
		return Objects.hash(
				super.hashCode(),
				dockableNode,
				dockableIconFactory,
				dockableMenuFactory,
				dockableConsumer,
				title,
				tooltip,
				dragGroupMask,
				isClosable
		);
	}

	/**
	 * Builds a {@link DockableState}.
	 */
	public static class DockableStateBuilder {

		private final String identifier;
		private @Nullable Node dockableNode;
		private @Nullable String title;
		private @Nullable String tooltipText;
		private @Nullable DockableIconFactory dockableIconFactory;
		private @Nullable DockableMenuFactory dockableMenuFactory;
		private @Nullable Integer dragGroupMask;
		private @Nullable Boolean isClosable;
		private @Nullable Consumer<Dockable> dockableConsumer;

		/**
		 * Constructor.
		 * @param identifier the {@link Dockable} identifier.
		 */
		public DockableStateBuilder(
				final String identifier
		) {
			this.identifier = requireNonNull(identifier);
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param dockableNode {@link Node} to display when the dockable is
		 * selected, {@code null} when no {@link Node} is selected.
		 * @see Dockable#setNode(Node)
		 */
		public DockableStateBuilder setDockableNode(
				final @Nullable Node dockableNode
		) {
			this.dockableNode = dockableNode;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param title the text for the {@link Dockable}'s {@code Header},
		 * {@code null} when no text should be used for the {@link Dockable}.
		 * @see Dockable#setTitle(String)
		 */
		public DockableStateBuilder setTitle(
				final @Nullable String title
		) {
			this.title = title;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param tooltipText the text to display when hovering the mouse over
		 * the {@link Dockable}'s {@code Header}, {@code null} when no tooltip
		 * should be displayed.
		 * @see Tooltip#setText(String)
		 * @see Dockable#setTooltip(Tooltip)
		 */
		public DockableStateBuilder setTooltipText(
				final @Nullable String tooltipText
		) {
			this.tooltipText = tooltipText;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param dockableIconFactory {@link DockableIconFactory} for creating a
		 * {@link Node} graphic, {@code null} when no {@link DockableIconFactory}
		 * is persisted.
		 * @see Dockable#setIconFactory(DockableIconFactory)
		 */
		public DockableStateBuilder setDockableIconFactory(
				final @Nullable DockableIconFactory dockableIconFactory
		) {
			this.dockableIconFactory = dockableIconFactory;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param dockableMenuFactory {@link DockableMenuFactory} for creating
		 * the context menu for a {@link Dockable}, {@code null} when no
		 * {@link DockableMenuFactory} is persisted.
		 * @see Dockable#setContextMenuFactory(DockableMenuFactory)
		 */
		public DockableStateBuilder setDockableMenuFactory(
				final @Nullable DockableMenuFactory dockableMenuFactory
		) {
			this.dockableMenuFactory = dockableMenuFactory;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param dragGroupMask drag group mask, {@code null} when no drag group
		 * mask is to be set.
		 * @see Dockable#setDragGroupMask(int)
		 */
		public DockableStateBuilder setDragGroupMask(
				final @Nullable Integer dragGroupMask
		) {
			this.dragGroupMask = dragGroupMask;
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param isClosable {@code true} if the dockable is closable,
		 * {@code false} if not closable, and {@code null} when closability is
		 * not to be specified.
		 * @see Dockable#setClosable(boolean)
		 */
		public DockableStateBuilder setClosable(
				@Nullable Boolean isClosable
		) {
			this.isClosable = isClosable;
			return this;
		}

		/**
		 * Specifies the {@link Consumer} to call when the {@link Dockable}
		 * represented by the {@link DockableState} has been added to the
		 * docking layout.
		 * @param dockableConsumer the {@link Consumer} to call when the
		 * {@link Dockable} represented by the {@link DockableState} has been
		 * added to the docking layout, {@code null} when no {@link Consumer}
		 * is to be called.
		 * @return this builder for chaining method calls.
		 */
		public DockableStateBuilder setDockableConsumer(
				@Nullable Consumer<Dockable> dockableConsumer
		) {
			this.dockableConsumer = dockableConsumer;
			return this;
		}

		/**
		 * {@return a {@link DockableState}.}
		 */
		public DockableState build() {
			return new DockableState(
					identifier,
					dockableNode,
					title,
					tooltipText,
					dockableIconFactory,
					dockableMenuFactory,
					dragGroupMask,
					isClosable,
					dockableConsumer
			);
		}
	}
}
