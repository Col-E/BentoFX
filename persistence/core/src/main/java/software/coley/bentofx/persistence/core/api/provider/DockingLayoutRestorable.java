package software.coley.bentofx.persistence.core.api.provider;

import software.coley.bentofx.persistence.core.api.DockingLayout;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.ui.layoutsmenu.LayoutsMenu;

import java.util.function.Supplier;

/**
 * An application whose docking layout can be replaced with another.
 *
 * <p>Offered for applications to implement, but nothing in the persistence
 * framework calls it during a save or a restore. It is intended for use by
 * application user controls such as the {@link LayoutsMenu}.</p>
 *
 * @author Phil Bryant
 */
public interface DockingLayoutRestorable {

	/**
	 * {@return the layout the application builds for itself.}
	 *
	 * <p>What a restore falls back to when nothing has been saved, and what the
	 * {@code Default} menu item switches to.</p>
	 */
	DockingLayout getDefaultDockingLayout();

	/**
	 * {@return the identified {@link DockingLayout} when one has been saved, or
	 * the fallback supplier's layout when none has or it cannot be read.}
	 *
	 * @param layoutPersistenceProfile identifies the layout to restore.
	 * @param fallbackLayoutSupplier supplies the layout to use instead when
	 * nothing can be restored. Startup passes the default layout; a switch
	 * between saved layouts passes an empty one, so that a failure shows up as
	 * nothing to apply rather than as the default layout arriving unasked.
	 *
	 * @see #getDefaultDockingLayout()
	 */
	DockingLayout getDockingLayout(
			LayoutPersistenceProfile layoutPersistenceProfile,
			Supplier<DockingLayout> fallbackLayoutSupplier
	);

	/**
	 * {@return {@code true} when the supplied layout was applied; otherwise,
	 * {@code false}, meaning the layout on screen was left alone.}
	 *
	 * <p>Telling the user about a switch that did not happen is left to the
	 * caller, which is where the wording for a restore that could not be
	 * carried out belongs.</p>
	 *
	 * @param dockingLayoutSupplier supplies the layout to switch to.
	 */
	boolean switchToLayout(Supplier<DockingLayout> dockingLayoutSupplier);

	/**
	 * {@return the provider this application reads and writes layouts through.}
	 *
	 * <p>The application's own instance rather than a fresh
	 * {@code DockingLayoutPersistence.provider()}: that call resolves the
	 * service afresh every time, and is meant to be made once at start-up and
	 * held.</p>
	 */
	DockingLayoutPersistenceProvider getPersistenceProvider();

	/**
	 * {@return the provider naming the {@code Bento}s whose layouts this
	 * application saves and restores.}
	 *
	 * <p>A registry of live objects that only the application can supply, and
	 * the same one it hands to a restorer, so that what a control saves is what
	 * the application would have saved itself.</p>
	 */
	BentoProvider getBentoProvider();
}
