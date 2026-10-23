package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import software.coley.bentofx.Bento;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.DockContainerBranchState;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState;
import software.coley.bentofx.persistence.core.api.state.DockContainerState;
import software.coley.bentofx.persistence.core.api.state.DragDropStageState;
import software.coley.bentofx.persistence.core.api.state.IdentifiableState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Compares stored layout state with what each {@link Bento} is showing.
 *
 * <p>Compares against state rather than against a built layout, so that
 * asking never builds containers, dockables or their content, however heavy
 * an application's content is.</p>
 *
 * <p>An arrangement is the containers and their nesting, each branch's
 * orientation, and each leaf's side, collapsed state, dockables in order, and
 * selected dockable. Divider positions are left out: they move with the
 * window's size, so an arrangement nobody changed would stop matching when the
 * window was resized.</p>
 *
 * <p>Reads the scene graph, so must be called on the JavaFX Application
 * Thread.</p>
 *
 * @author Phil Bryant
 */
final class LayoutArrangements {

	private LayoutArrangements() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * {@return {@code true} when every {@link Bento} shows the arrangement the
	 * state describes for it, and nothing else; otherwise, {@code false}.}
	 *
	 * @param bentoStates the stored arrangement to look for.
	 * @param bentoProvider the {@link Bento} instances whose root containers are
	 * what is showing.
	 */
	static boolean isShowing(
			final List<BentoState> bentoStates,
			final BentoProvider bentoProvider
	) {
		final Collection<Bento> bentos = bentoProvider.getAllBentos();

		for (final BentoState bentoState : bentoStates) {
			if (bentoProvider.getBento(bentoState.getIdentifier()).isEmpty()
					&& !getDockContainerRootBranchStates(bentoState).isEmpty()) {
				return false;
			}
		}

		for (final Bento bento : bentos) {
			final List<DockContainerRootBranchState> expectedRoots =
					bentoStates.stream()
							.filter(bentoState -> bentoState.getIdentifier().equals(bento.getIdentifier()))
							.flatMap(bentoState -> getDockContainerRootBranchStates(bentoState).stream())
							.toList();

			if (!matches(expectedRoots, bento.getRootContainers())) {
				return false;
			}
		}

		return true;
	}

	/**
	 * {@return the state's root branches, including those of its drag/drop
	 * stages.}
	 *
	 * @param bentoState the state to read.
	 */
	private static List<DockContainerRootBranchState> getDockContainerRootBranchStates(
			final BentoState bentoState
	) {
		final List<DockContainerRootBranchState> roots =
				new ArrayList<>(bentoState.getRootBranchStates());

		for (final DragDropStageState dragDropStageState : bentoState.getDragDropStageStates()) {
			dragDropStageState.getDockContainerRootBranchState().ifPresent(roots::add);
		}

		return roots;
	}

	/**
	 * {@return {@code true} when each expected root has its own matching actual
	 * root, in any order; otherwise, {@code false}.}
	 *
	 * <p>Unordered because a {@link Bento} lists its roots in the order they
	 * were registered, which depends on how the layout was applied rather than
	 * on what it holds.</p>
	 *
	 * @param expectedRoots the roots the state describes.
	 * @param actualRoots the roots showing now.
	 */
	private static boolean matches(
			final List<DockContainerRootBranchState> expectedRoots,
			final List<DockContainerRootBranch> actualRoots
	) {
		if (expectedRoots.size() != actualRoots.size()) {
			return false;
		}

		final List<DockContainerRootBranch> unmatched =
				new ArrayList<>(actualRoots);

		for (final DockContainerRootBranchState expectedRoot : expectedRoots) {
			if (!removeFirstMatch(unmatched, expectedRoot)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Removes the first root arranged as the expected one describes.
	 *
	 * @param candidates the roots not yet matched.
	 * @param expectedRoot the root to find a match for.
	 *
	 * @return {@code true} when a match was found and removed; otherwise,
	 * {@code false}.
	 */
	private static boolean removeFirstMatch(
			final List<DockContainerRootBranch> candidates,
			final DockContainerRootBranchState expectedRoot
	) {
		final Iterator<DockContainerRootBranch> iterator = candidates.iterator();

		while (iterator.hasNext()) {
			if (isSameArrangement(expectedRoot, iterator.next())) {
				iterator.remove();
				return true;
			}
		}

		return false;
	}

	/**
	 * {@return {@code true} when the container, and everything below it, is
	 * arranged as the state describes; otherwise, {@code false}.}
	 *
	 * @param expected the stored state of the container.
	 * @param actual the container showing now.
	 */
	private static boolean isSameArrangement(
			final DockContainerState expected,
			final DockContainer actual
	) {
		return switch (expected) {
			case DockContainerBranchState expectedBranch ->
					actual instanceof DockContainerBranch actualBranch
							&& expectedBranch.getOrientation()
									.equals(Optional.of(actualBranch.getOrientation()))
							&& isSameArrangement(
									expectedBranch.getChildDockContainerStates(),
									actualBranch.getChildContainers()
							);
			case DockContainerLeafState expectedLeaf ->
					actual instanceof DockContainerLeaf actualLeaf
							&& expectedLeaf.getSide()
									.equals(Optional.ofNullable(actualLeaf.getSide()))
							&& expectedLeaf.isCollapsed().orElse(false) == actualLeaf.isCollapsed()
							&& expectedLeaf.getChildDockableStates().stream()
									.map(IdentifiableState::getIdentifier)
									.toList()
									.equals(actualLeaf.getDockables().stream()
											.map(Dockable::getIdentifier)
											.toList())
							&& expectedLeaf.getSelectedDockableIdentifier()
									.equals(Optional.ofNullable(actualLeaf.getSelectedDockable())
											.map(Dockable::getIdentifier));
		};
	}

	/**
	 * {@return {@code true} when the actual children are arranged as the stored
	 * ones describe, in the same order; otherwise, {@code false}.}
	 *
	 * @param expectedChildren the stored states of the children.
	 * @param actualChildren the children showing now.
	 */
	private static boolean isSameArrangement(
			final List<DockContainerState> expectedChildren,
			final List<DockContainer> actualChildren
	) {
		if (expectedChildren.size() != actualChildren.size()) {
			return false;
		}

		for (int i = 0; i < expectedChildren.size(); i++) {
			if (!isSameArrangement(expectedChildren.get(i), actualChildren.get(i))) {
				return false;
			}
		}

		return true;
	}
}
