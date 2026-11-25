package software.coley.bentofx.persistence.core.impl;

import javafx.application.Platform;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.bentofx.Bento;
import software.coley.bentofx.control.DragDropStage;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerLeafMenuFactory;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.BentoStateTimeoutException;
import software.coley.bentofx.persistence.core.api.DockingLayout;
import software.coley.bentofx.persistence.core.api.DockingLayoutRebuilder;
import software.coley.bentofx.persistence.core.api.LayoutRestorer;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.provider.DockContainerLeafMenuFactoryProvider;
import software.coley.bentofx.persistence.core.api.provider.DockableStateProvider;
import software.coley.bentofx.persistence.core.api.provider.StageIconImageProvider;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Restores persisted {@link DockingLayout}s.
 *
 * @author Phil Bryant
 */
public class DockingLayoutRestorer implements LayoutRestorer {

	private static final Logger logger =
			LoggerFactory.getLogger(DockingLayoutRestorer.class);

	private final LayoutStateReader layoutStateReader;
	private final DockingLayoutRebuilder defaultDockingLayoutRebuilder;

	/**
	 * Constructs a {@code DockingLayoutRestorer}.
	 *
	 * @param layoutCodec the {@link LayoutCodec} to use to decode the persisted
	 * layout.
	 * @param layoutStorage the {@link LayoutStorage} to use to read the
	 * persisted layout. This restorer takes ownership of it and closes it from
	 * {@link #close()}, so the same instance must not also be given to a
	 * {@code LayoutSaver}.
	 * @param bentoProvider the {@link BentoProvider} to use to get {@link Bento}
	 * instances from their identifier.
	 * @param dockableStateProvider the {@link DockableStateProvider} to use to
	 * get {@link software.coley.bentofx.dockable.Dockable} instances from their
	 * identifier.
	 * @param stageIconImageProvider the {@link StageIconImageProvider} to use
	 * to get icons for restored {@link DragDropStage} instances.
	 * @param dockContainerLeafMenuFactoryProvider the
	 * {@link DockContainerLeafMenuFactoryProvider} to use to get
	 * {@link DockContainerLeafMenuFactory} for restored
	 * {@link DockContainerLeaf} instances.
	 */
	public DockingLayoutRestorer(
			final LayoutCodec layoutCodec,
			final LayoutStorage layoutStorage,
			final BentoProvider bentoProvider,
			final DockableStateProvider dockableStateProvider,
			final @Nullable StageIconImageProvider stageIconImageProvider,
			final @Nullable DockContainerLeafMenuFactoryProvider dockContainerLeafMenuFactoryProvider
	) {
		this(
				new LayoutStateReader(layoutCodec, layoutStorage),
				new DefaultDockingLayoutRebuilder(
						bentoProvider,
						dockableStateProvider,
						stageIconImageProvider,
						dockContainerLeafMenuFactoryProvider
				)
		);
	}

	DockingLayoutRestorer(
			final LayoutStateReader layoutStateReader,
			final DefaultDockingLayoutRebuilder defaultDockingLayoutRebuilder
	) {
		this.layoutStateReader = Objects.requireNonNull(layoutStateReader);
		this.defaultDockingLayoutRebuilder = Objects.requireNonNull(defaultDockingLayoutRebuilder);
	}

	@Override
	public boolean doesLayoutExist() {
		return layoutStateReader.layoutExists();
	}

	@Override
	public DockingLayout restoreLayout(
			final Supplier<DockingLayout> defaultLayoutSupplier
	) {

		try {
			// The existence check is storage work too - a database query for
			// some storages - so it goes off the JavaFX thread with the read.
			final Optional<List<BentoState>> bentoStateList =
					PersistenceThreading.callOffFxThread(() ->
							layoutStateReader.layoutExists()
									? Optional.of(layoutStateReader.readLayoutState())
									: Optional.empty()
					);

			if (bentoStateList.isEmpty()) {
				return getDefaultLayout(defaultLayoutSupplier);
			}

			return restoreOnFxThread(bentoStateList.get());

		} catch (final BentoStateTimeoutException e) {
			// Deliberately not handled like the failure below. A timeout means
			// the JavaFX thread never ran the restore, not that the persisted
			// layout is bad. Substituting the default layout here would discard
			// a layout that is very likely fine, and the next automatic save
			// would then write that default over the saved copy. Fail loudly
			// instead and leave the persisted layout untouched.
			throw new IllegalStateException(
					"Timed out restoring the docking layout",
					e
			);
		} catch (final BentoStateException e) {
			logger.warn(
					"An error occurred while attempting to restore the layout",
					e
			);

			return getDefaultLayout(defaultLayoutSupplier);
		}
	}

	/**
	 * Builds the decoded layout on the JavaFX application thread.
	 *
	 * <p>A restore can outlive the caller's wait: once it is running on the
	 * JavaFX thread a timeout cannot stop it, and it goes on to register its
	 * drag/drop stage roots for a layout nobody will apply. Whichever runs
	 * second - the restore finishing, or the cleanup queued when the wait gave
	 * up - discards it. Both run on the JavaFX thread, so they cannot
	 * interleave, and {@code unclaimed} hands the layout to exactly one of
	 * them.</p>
	 *
	 * @param bentoStateList the decoded states.
	 *
	 * @return the restored layout.
	 *
	 * @throws BentoStateException when the restore fails or times out.
	 */
	private DockingLayout restoreOnFxThread(
			final List<BentoState> bentoStateList
	) throws BentoStateException {
		final AtomicBoolean abandoned = new AtomicBoolean();
		final AtomicReference<@Nullable DockingLayout> unclaimed =
				new AtomicReference<>();

		try {
			return PersistenceThreading.callOnFxThread(() -> {
				final DockingLayout dockingLayout =
						defaultDockingLayoutRebuilder.rebuild(bentoStateList);

				if (abandoned.get()) {
					DefaultDockingLayoutRebuilder.discard(dockingLayout);
				} else {
					unclaimed.set(dockingLayout);
				}

				return dockingLayout;
			});
		} catch (final BentoStateTimeoutException e) {
			abandoned.set(true);
			Platform.runLater(() -> {
				final DockingLayout lateLayout = unclaimed.getAndSet(null);
				if (lateLayout != null) {
					DefaultDockingLayoutRebuilder.discard(lateLayout);
				}
			});
			throw e;
		}
	}

	/**
	 * Gets the fallback/default layout on the JavaFX application thread.
	 *
	 * @param defaultLayoutSupplier default layout supplier.
	 *
	 * @return default layout.
	 */
	private DockingLayout getDefaultLayout(
			final Supplier<DockingLayout> defaultLayoutSupplier
	) {
		try {
			return PersistenceThreading.callOnFxThread(defaultLayoutSupplier::get);
		} catch (final BentoStateException e) {
			throw new IllegalStateException(
					"Could not create default docking layout",
					e
			);
		}
	}

	@Override
	public void close() {
		layoutStateReader.close();
	}
}
