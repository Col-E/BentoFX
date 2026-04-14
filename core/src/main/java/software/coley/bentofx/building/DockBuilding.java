package software.coley.bentofx.building;

import org.jspecify.annotations.NonNull;
import software.coley.bentofx.Bento;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;

import java.util.Random;

/**
 * Builders for {@link DockContainer} and {@link Dockable} instances.
 *
 * @author Matt Coley
 */
public class DockBuilding {
	private static final Random RANDOM = new Random();
	private final Bento bento;

	/**
	 * @param bento
	 * 		Parent bento instance.
	 */
	public DockBuilding(@NonNull Bento bento) {
		this.bento = bento;
	}

	/**
	 * @return New dockable.
	 */
	@NonNull
	public Dockable dockable() {
		return dockable(uid("dockable"));
	}

	/**
	 * @param identifier
	 * 		Identifier to assign to the created dockable.
	 *
	 * @return New dockable.
	 */
	@NonNull
	public Dockable dockable(@NonNull String identifier) {
		return new Dockable(bento, identifier);
	}

	/**
	 * @return New root container.
	 *
	 * @see Bento#registerRoot(DockContainerRootBranch)
	 * @see Bento#unregisterRoot(DockContainerRootBranch)
	 */
	@NonNull
	public DockContainerRootBranch root() {
		return root(uid("croot"));
	}


	/**
	 * @param identifier
	 * 		Identifier to assign to the created container.
	 *
	 * @return New root container.
	 *
	 * @see Bento#registerRoot(DockContainerRootBranch)
	 * @see Bento#unregisterRoot(DockContainerRootBranch)
	 */
	@NonNull
	public DockContainerRootBranch root(@NonNull String identifier) {
		return new DockContainerRootBranch(bento, identifier);
	}

	/**
	 * @return New branch container.
	 */
	@NonNull
	public DockContainerBranch branch() {
		return branch(uid("cbranch"));
	}

	/**
	 * @param identifier
	 * 		Identifier to assign to the created container.
	 *
	 * @return New branch container.
	 */
	@NonNull
	public DockContainerBranch branch(@NonNull String identifier) {
		return new DockContainerBranch(bento, identifier);
	}

	/**
	 * @return New leaf container.
	 */
	@NonNull
	public DockContainerLeaf leaf() {
		return leaf(uid("cleaf"));
	}

	/**
	 * @param identifier
	 * 		Identifier to assign to the created container.
	 *
	 * @return New branch container.
	 */
	@NonNull
	public DockContainerLeaf leaf(@NonNull String identifier) {
		return new DockContainerLeaf(bento, identifier);
	}

	@NonNull
	private static String uid(@NonNull String prefix) {
		StringBuilder suffix = new StringBuilder(8);
		for (int i = 0; i < 8; i++)
			suffix.append((char) RANDOM.nextInt('A', 'Z'));
		return prefix + ":" + suffix;
	}
}
