package software.coley.bentofx.persistence.core.api;

import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.provider.DockContainerLeafMenuFactoryProvider;
import software.coley.bentofx.persistence.core.api.provider.DockableStateProvider;
import software.coley.bentofx.persistence.core.api.provider.StageIconImageProvider;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.impl.DefaultDockingLayoutRebuilder;

import java.util.List;
import java.util.Objects;

/**
 * Rebuilds a live {@link DockingLayout} from a list of serializable
 * {@link BentoState} records.
 *
 * <p>The builder side of the restore pipeline, extracted as a public API so
 * a custom {@link software.coley.bentofx.persistence.core.api.provider.DockingLayoutPersistenceProvider}
 * can reuse it. The default {@link LayoutRestorer} chains a
 * {@link software.coley.bentofx.persistence.core.api.storage.LayoutStorage} and
 * a {@link software.coley.bentofx.persistence.core.api.codec.LayoutCodec} to
 * produce the input list; a provider that reads structured relational storage
 * assembles the list from its own schema and calls {@link #rebuild} on it.</p>
 *
 * <p>JavaFX Application Thread only. {@link #rebuild} creates
 * {@link software.coley.bentofx.control.DragDropStage} instances and assigns
 * {@code Scene} objects to them. The default {@link LayoutRestorer} runs it on
 * the FX thread for you; a caller using this interface directly is responsible
 * for the same.</p>
 *
 * @author Phil Bryant
 */
public interface DockingLayoutRebuilder {

	/**
	 * {@return a {@link DockingLayoutRebuilder} backed by the framework's default
	 * builder implementation.}
	 *
	 * @param bentoProvider supplies the {@code Bento} instances the rebuilt
	 * state will be applied to.
	 * @param dockableStateProvider resolves each persisted dockable identifier
	 * to the runtime state the application wants to show for it.
	 * @param stageIconImageProvider supplies icons for restored
	 * {@link software.coley.bentofx.control.DragDropStage} instances, or
	 * {@code null} to restore them without icons.
	 * @param leafMenuFactoryProvider supplies the per-leaf menu factory, or
	 * {@code null} to restore leaves without a factory.
	 * @throws NullPointerException when {@code bentoProvider} or
	 * {@code dockableStateProvider} is {@code null}.
	 */
	static DockingLayoutRebuilder create(
			final BentoProvider bentoProvider,
			final DockableStateProvider dockableStateProvider,
			final @Nullable StageIconImageProvider stageIconImageProvider,
			final @Nullable DockContainerLeafMenuFactoryProvider leafMenuFactoryProvider
	) {
		return new DefaultDockingLayoutRebuilder(
				Objects.requireNonNull(bentoProvider, "bentoProvider"),
				Objects.requireNonNull(
						dockableStateProvider,
						"dockableStateProvider"
				),
				stageIconImageProvider,
				leafMenuFactoryProvider
		);
	}

	/**
	 * {@return a {@link DockingLayout} rebuilt from the supplied state.}
	 *
	 * <p>The returned layout's containers are not yet attached to a
	 * {@code Scene}, matching what {@link LayoutRestorer#restoreLayout} returns.
	 * The caller is expected to attach them before the next save; see
	 * {@link LayoutSaver#saveLayout()} for the window where an auto-save would
	 * find nothing to capture.</p>
	 *
	 * <p>A dockable the {@link DockableStateProvider} cannot resolve is skipped
	 * rather than failing the rebuild - matching the framework's existing
	 * tolerance for a persisted layout that outlived the application component
	 * it names.</p>
	 *
	 * <p>Must be called on the JavaFX Application Thread.</p>
	 *
	 * @param bentoStates the state to rebuild.
	 * @throws NullPointerException when {@code bentoStates} is {@code null}.
	 */
	DockingLayout rebuild(List<BentoState> bentoStates);
}
