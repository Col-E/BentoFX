/**
 * Provider interfaces for the persistence API. They fall into two groups, and which
 * group an interface belongs to tells you who is expected to write it.
 *
 * <p><b>The application writes these</b> and passes them to {@code getLayoutSaver}
 * or {@code getLayoutRestorer}, because they describe things only the application
 * knows - which {@code Bento}s exist, what belongs inside a restored dockable, how a
 * window should look:
 * {@link software.coley.bentofx.persistence.core.api.provider.BentoProvider},
 * {@link software.coley.bentofx.persistence.core.api.provider.DockableStateProvider},
 * {@link software.coley.bentofx.persistence.core.api.provider.StageIconImageProvider} and
 * {@link software.coley.bentofx.persistence.core.api.provider.DockContainerLeafMenuFactoryProvider}.
 * </p>
 * <p>{@link software.coley.bentofx.persistence.core.api.provider.DockableMenuFactoryProvider}
 * is one step further out: the framework never receives one, and it exists only as a
 * shape a {@code DockableStateProvider} implementation can use internally.
 * </p>
 * <p><b>A dependency supplies these</b>, and the application selects between them
 * without writing any code. They are the replaceable parts - the format a layout is
 * written in and where it is written to - and they are the only two interfaces here
 * extending
 * {@link software.coley.bentofx.persistence.core.api.provider.LayoutPersistenceComponentProvider}:
 * {@link software.coley.bentofx.persistence.core.api.provider.LayoutCodecProvider} and
 * {@link software.coley.bentofx.persistence.core.api.provider.LayoutStorageProvider}. Pick
 * between several with
 * {@link software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile}, or depend
 * on one and let it be chosen automatically.</p>
 *
 * <p>{@link software.coley.bentofx.persistence.core.api.provider.DockingLayoutPersistenceProvider}
 * belongs to neither group. It is the entry point that produces savers and restorers,
 * and it is obtained from
 * {@link software.coley.bentofx.persistence.core.api.DockingLayoutPersistence#provider()}.</p>
 *
 * @author Phil Bryant
 */
package software.coley.bentofx.persistence.core.api.provider;
