package software.coley.bentofx.persistence.core.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.bentofx.Bento;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;

import java.util.List;
import java.util.Objects;

import static software.coley.bentofx.persistence.core.impl.PersistenceThreading.callOffFxThread;

/**
 * Automatically saves the layout of all {@link Bento}s into {@link BentoState},
 * encodes them via a {@link LayoutCodec}, and persists them via a
 * {@link LayoutStorage}.
 *
 * @author Phil Bryant
 */
public class DockingLayoutSaver extends AbstractAutoCloseableLayoutSaver {

	private static final Logger logger =
			LoggerFactory.getLogger(DockingLayoutSaver.class);

	private final DefaultBentoStateCapturer bentoLayoutStateCaptor;
	private final LayoutStateWriter layoutStateWriter;

	/**
	 * Creates a {@link DockingLayoutSaver}
	 *
	 * @param layoutCodec the {@link LayoutCodec} to use to encode the persisted
	 * layout.
	 * @param layoutStorage the {@link LayoutStorage} to use to write the
	 * persisted layout. This saver takes ownership of it
	 * and closes it from {@link #close()}, so the same
	 * instance must not also be given to a
	 * {@link software.coley.bentofx.persistence.core.api.LayoutRestorer}.
	 * @param bentoProvider the {@link BentoProvider} to use to get {@link Bento}
	 * instances from their identifiers.
	 */
	public DockingLayoutSaver(
			final LayoutCodec layoutCodec,
			final LayoutStorage layoutStorage,
			final BentoProvider bentoProvider
	) {
		this(layoutCodec, layoutStorage, bentoProvider, null);
	}

	/**
	 * Creates a {@link DockingLayoutSaver} that writes the supplied display
	 * name into the layout it saves.
	 *
	 * @param layoutCodec the {@link LayoutCodec} to use to encode the
	 * persisted layout.
	 * @param layoutStorage the {@link LayoutStorage} to use to write the
	 * persisted layout. This saver takes ownership of it
	 * and closes it from {@link #close()}.
	 * @param bentoProvider the {@link BentoProvider} to use to get
	 * {@link Bento} instances from their identifiers.
	 * @param displayName the human-readable name to store with the layout,
	 * or {@code null} for a layout saved without one,
	 * such as the session layout.
	 */
	public DockingLayoutSaver(
			final LayoutCodec layoutCodec,
			final LayoutStorage layoutStorage,
			final BentoProvider bentoProvider,
			final @Nullable String displayName
	) {
		this(layoutCodec, layoutStorage, bentoProvider, displayName, null);
	}

	/**
	 * Creates a {@link DockingLayoutSaver} that writes the supplied display
	 * name and group into the layout it saves.
	 *
	 * <p>Both are written on every save, so a layout saved over one that was
	 * in a group has to be handed that group again to stay in it.</p>
	 *
	 * @param layoutCodec the {@link LayoutCodec} to use to encode the
	 * persisted layout.
	 * @param layoutStorage the {@link LayoutStorage} to use to write the
	 * persisted layout. This saver takes ownership of it
	 * and closes it from {@link #close()}.
	 * @param bentoProvider the {@link BentoProvider} to use to get
	 * {@link Bento} instances from their identifiers.
	 * @param displayName the human-readable name to store with the layout,
	 * or {@code null} for a layout saved without one,
	 * such as the session layout.
	 * @param group the group to store the layout in, or {@code null} for no
	 * group.
	 */
	public DockingLayoutSaver(
			final LayoutCodec layoutCodec,
			final LayoutStorage layoutStorage,
			final BentoProvider bentoProvider,
			final @Nullable String displayName,
			final @Nullable String group
	) {
		this(
				bentoProvider,
				new DefaultBentoStateCapturer(bentoProvider),
				new LayoutStateWriter(
						displayName,
						group,
						layoutCodec,
						layoutStorage
				)
		);
	}

	DockingLayoutSaver(
			final BentoProvider bentoProvider,
			final DefaultBentoStateCapturer bentoLayoutStateCaptor,
			final LayoutStateWriter layoutStateWriter
	) {
		super(bentoProvider);
		this.bentoLayoutStateCaptor =
				Objects.requireNonNull(bentoLayoutStateCaptor);
		this.layoutStateWriter =
				Objects.requireNonNull(layoutStateWriter);
	}

	@Override
	protected void saveLayoutInternal() throws BentoStateException {
		saveLayout(PersistenceThreading.FX_CAPTURE_TIMEOUT_MILLIS);
	}

	@Override
	protected void saveLayoutForShutdown() throws BentoStateException {
		// A shorter budget while the application is exiting: the thread calling
		// close() may not be a daemon, so waiting the full capture budget could
		// hold up shutdown. Losing the final save beats wedging the exit.
		saveLayout(PersistenceThreading.FX_CLOSE_TIMEOUT_MILLIS);
	}

	/**
	 * Captures the layout on the JavaFX application thread, then encodes and
	 * writes it away from that thread.
	 *
	 * <p>A capture that found nothing is not written - see
	 * {@link #hasNothingToSave}.</p>
	 *
	 * @param fxTimeoutMillis how long to wait for the JavaFX application thread
	 * to capture the layout.
	 *
	 * @throws BentoStateException when capturing, encoding or writing fails.
	 */
	private void saveLayout(final long fxTimeoutMillis)
			throws BentoStateException {
		final List<BentoState> bentoStateList =
				PersistenceThreading.callOnFxThread(
						bentoLayoutStateCaptor::capture,
						fxTimeoutMillis
				);

		if (hasNothingToSave(bentoStateList)) {
			logger.debug(
					"Captured no root branches or drag/drop stages; leaving the " +
							"persisted layout as it is rather than overwriting it " +
							"with an empty one."
			);
			return;
		}

		callOffFxThread(() -> {
			layoutStateWriter.writeLayout(bentoStateList);
			return Boolean.TRUE;
		});
	}

	/**
	 * {@return {@code true} when the capture found no layout at all.}
	 *
	 * <p>A {@code DockContainerRootBranch} registers itself with its {@link Bento}
	 * only once it has a {@code Scene}, so a capture taken while nothing is
	 * attached yields an empty state - most easily between this module's own
	 * restore, which hands branches back unattached, and the application placing
	 * them. Writing that would truncate a good layout to nothing, and the next
	 * restore would come back empty.
	 *
	 * <p>Skipping is safe against the case it looks like it might get wrong, an
	 * application that legitimately closed everything: a running application
	 * showing anything at all has at least one attached root branch, so
	 * "absolutely nothing anywhere" means not-ready or shutting down rather than
	 * deliberately empty. The cost of being wrong is asymmetric too - a stale
	 * layout is recoverable, an erased one is not.
	 *
	 * @param bentoStateList the captured states.
	 */
	private static boolean hasNothingToSave(
			final List<BentoState> bentoStateList
	) {
		// allMatch on an empty list is true, which is the wanted answer for a
		// provider that reported no Bentos at all.
		return bentoStateList.stream().allMatch(bentoState ->
				bentoState.getRootBranchStates().isEmpty()
						&& bentoState.getDragDropStageStates().isEmpty()
		);
	}

	@Override
	public void close() {
		try {
			super.close();
		} finally {
			layoutStateWriter.close();
		}
	}
}
