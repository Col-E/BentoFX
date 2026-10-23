package software.coley.bentofx.persistence.core.api;

import software.coley.bentofx.Identifiable;
import software.coley.bentofx.control.DragDropStage;
import software.coley.bentofx.layout.container.DockContainerRootBranch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Defines the layout of a {@code Bento} for persisting it.
 *
 * @author Phil Bryant
 */
public class BentoLayout implements Identifiable {

	private final String identifier;
	private final List<DockContainerRootBranch> rootBranches;
	private final List<DragDropStage> dragDropStages;
	private final Set<DragDropStage> showingDragDropStages;

	private BentoLayout(
			final String identifier,
			final List<DockContainerRootBranch> rootBranches,
			final List<DragDropStage> allDragDropStages,
			final Set<DragDropStage> showingDragDropStages
	) {
		this.identifier = Objects.requireNonNull(identifier);
		this.rootBranches = List.copyOf(rootBranches);
		this.dragDropStages = List.copyOf(allDragDropStages);

		// Identity, not equality. The question this set answers is "was *this*
		// stage showing", and DragDropStage is public and non-final, so a
		// subclass overriding equals would otherwise make two distinct stages
		// collapse into one entry. Set.copyOf would use equals.
		final Set<DragDropStage> showingCopy =
				Collections.newSetFromMap(new IdentityHashMap<>());
		showingCopy.addAll(showingDragDropStages);
		this.showingDragDropStages = Collections.unmodifiableSet(showingCopy);
	}

	/**
	 * {@return the identifier of the {@code Bento} this layout belongs to.}
	 */
	@Override
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * {@return an immutable {@link List} of the restored
	 * {@link DockContainerRootBranch}es, in the order they were added.}
	 *
	 * <p>These are not attached to a {@code Scene}. The application is
	 * responsible for placing them, and must do so before the next save - a
	 * {@link DockContainerRootBranch} only registers itself with its
	 * {@code Bento} once it has a {@code Scene}, so an unattached branch is
	 * invisible to a capture.</p>
	 */
	public List<DockContainerRootBranch> getRootBranches() {
		return rootBranches;
	}

	/**
	 * {@return an immutable {@link List} of the restored
	 * {@link DragDropStage}s, in the order they were added.}
	 *
	 * <p>These are not shown. Whether a restored stage should be visible is the
	 * application's decision - see {@link #wasShowing(DragDropStage)} for what
	 * the persisted layout recorded.</p>
	 */
	public List<DragDropStage> getDragDropStages() {
		return dragDropStages;
	}

	/**
	 * {@return {@code true} when the persisted layout recorded this
	 * {@link DragDropStage} as showing.}
	 *
	 * <p>This module never shows a stage, so this is the value an application
	 * needs in order to restore visibility along with the rest of the layout:</p>
	 *
	 * {@snippet lang = java:
	 * for (final DragDropStage stage : bentoLayout.getDragDropStages()) {
	 *     if (bentoLayout.wasShowing(stage)) {
	 *         stage.show();
	 *     }
	 * }
	 *}
	 *
	 * <p>A stage the persisted state said nothing about counts as showing. The
	 * captor always records the flag, so an absent value means either a
	 * hand-built state or a layout written before this was honoured, and showing
	 * it matches what those layouts already did - the alternative would silently
	 * stop restoring detached windows.</p>
	 *
	 * @param dragDropStage the stage to ask about, compared by identity.
	 */
	public boolean wasShowing(final DragDropStage dragDropStage) {
		return showingDragDropStages.contains(dragDropStage);
	}

	/**
	 * Builds a {@link BentoLayout}.
	 */
	public static class BentoLayoutBuilder {

		private final String bentoId;
		private final List<DockContainerRootBranch> rootBranches =
				new ArrayList<>();
		private final List<DragDropStage> dragDropStages =
				new ArrayList<>();
		private final Set<DragDropStage> showingDragDropStages =
				Collections.newSetFromMap(new IdentityHashMap<>());

		/**
		 * Constructor.
		 * @param bentoId the identifier of the {@code Bento} this layout
		 * belongs to.
		 */
		public BentoLayoutBuilder(final String bentoId) {
			this.bentoId = Objects.requireNonNull(bentoId);
		}

		/**
		 * {@return this builder for chaining method calls.}
		 * @param rootBranch the {@link DockContainerRootBranch} to add.
		 */
		public BentoLayoutBuilder addRootBranch(
				final DockContainerRootBranch rootBranch
		) {
			rootBranches.add(Objects.requireNonNull(rootBranch));
			return this;
		}

		/**
		 * {@return this builder for chaining method calls.}
		 *
		 * <p>There is deliberately no overload without {@code wasShowing}: the
		 * flag was previously captured and then dropped on the floor, and an
		 * overload that defaulted it would let a caller reintroduce that silently.
		 * </p>
		 *
		 * @param dragDropStage the {@link DragDropStage} to add.
		 * @param wasShowing whether the persisted layout recorded the stage as
		 * showing - see {@link BentoLayout#wasShowing(DragDropStage)}.
		 */
		public BentoLayoutBuilder addDragDropStage(
				final DragDropStage dragDropStage,
				final boolean wasShowing
		) {
			dragDropStages.add(Objects.requireNonNull(dragDropStage));

			if (wasShowing) {
				showingDragDropStages.add(dragDropStage);
			}
			return this;
		}

		/**
		 * {@return the {@link BentoLayout} built from this builder.}
		 */
		public BentoLayout build() {
			return new BentoLayout(
					bentoId,
					rootBranches,
					dragDropStages,
					showingDragDropStages
			);
		}
	}
}
