package software.coley.bentofx.persistence.core.api;

import java.util.function.Supplier;

/**
 * Restores a persisted BentoFX docking layout.
 *
 * @author Phil Bryant
 */
public interface LayoutRestorer extends AutoCloseable {

	/**
	 * Returns {@code true} if a stored layout exists; otherwise, returns {@code false}.
	 *
	 * @return {@code true} if a stored layout exists; otherwise, returns {@code false}.
	 */
	boolean doesLayoutExist();

	/**
	 * Returns the restored {@link DockingLayout}, if it exists and can be read
	 * without error. Otherwise, returns the {@link DockingLayout} returned by
	 * the {@code Supplier<DockingLayout>}.
	 *
	 * <p><b>The returned containers are not attached to a {@code Scene}, and must
	 * be attached before the next save.</b> A
	 * {@code DockContainerRootBranch} registers itself with its {@code Bento} only
	 * once it has a {@code Scene}, so until the application places them a capture
	 * sees no layout at all - which is why an auto-save that fires in that window
	 * has nothing to write. See {@link BentoLayout#getRootBranches()} and
	 * {@link LayoutSaver#saveLayout()}.</p>
	 *
	 * @param defaultLayoutSupplier the {@code Supplier<DockingLayout>} to use
	 *                              when the {@link DockingLayout} does not
	 *                              exist or cannot be restored without error.
	 * @return the restored {@link DockingLayout}, if it exists and can be read
	 * without error; otherwise, the {@link DockingLayout} returned by the
	 * {@code Supplier<DockingLayout>}.
	 */
	DockingLayout restoreLayout(
			final Supplier<DockingLayout> defaultLayoutSupplier
	);

	@Override
	default void close() {
		// Default no-op. Implementations that own resources should override.
	}
}
