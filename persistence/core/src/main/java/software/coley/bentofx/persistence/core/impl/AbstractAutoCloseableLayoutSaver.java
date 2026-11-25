package software.coley.bentofx.persistence.core.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.bentofx.Bento;
import software.coley.bentofx.event.DockEvent;
import software.coley.bentofx.event.DockEventListener;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.LayoutSaver;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;

import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract {@link LayoutSaver} that automatically saves the docking layout at
 * scheduled intervals and can be called when the application exits. To do so
 * efficiently, this class listens for {@link DockEvent}s to track whether changes have been made
 * to the layout and only saves when changes have actually been made. Because
 * this class implements {@link AutoCloseable}, it can be used in a
 * try-with-resources block to automatically call {@link #close()} to save the
 * docking layout when the try block exits.
 *
 * <p><b>Auto-save must be started explicitly</b>, by calling
 * {@link #enableAutoSave(long, TimeUnit)} once the instance is fully
 * constructed. It is deliberately not started by the constructor: doing so
 * handed {@code this} to a scheduler thread and registered it on every
 * {@link Bento}'s event bus before subclass fields had been assigned, so a save
 * firing in that window could observe a half-built object. Subclasses cannot fix
 * that themselves - the superclass constructor always runs first. Instances
 * obtained from
 * {@link software.coley.bentofx.persistence.core.api.provider.DockingLayoutPersistenceProvider}
 * already have auto-save running.</p>
 *
 * <p><b>Thread safety.</b> {@link #enableAutoSave(long, TimeUnit)},
 * {@link #disableAutoSave()} and {@link #close()} may be called from any thread
 * and are mutually exclusive; the auto-save lifecycle state they share is
 * guarded by a private lock. {@link #close()} is idempotent. What is
 * <em>not</em> serialized is saving itself: {@link #close()} stops the timer
 * under that lock but deliberately performs its final save outside it, because
 * a save waits on the JavaFX application thread and that thread may itself be
 * calling in here.
 * A subclass implementing {@link #saveLayoutInternal()} or overriding
 * {@link #saveLayoutForShutdown()} must therefore assume it can be entered from
 * the scheduler thread and from a caller of {@link #close()}, and make its own
 * state safe accordingly.</p>
 *
 * @author Phil Bryant
 */
public abstract class AbstractAutoCloseableLayoutSaver
		implements LayoutSaver {

	private static final Logger logger =
			LoggerFactory.getLogger(AbstractAutoCloseableLayoutSaver.class);

	/**
	 * The interval {@link #startAutoSave(AbstractAutoCloseableLayoutSaver)}
	 * uses when no explicit interval is supplied.
	 *
	 * <p>Public so a subclass that configures auto-save by hand (via
	 * {@link #enableAutoSave(long, TimeUnit)}) can see what the framework
	 * default is, as a reference point for its own choice. The value is a
	 * {@link Duration} rather than a bare {@code long} so the unit is carried
	 * with the value and cannot be paired with the wrong {@link TimeUnit}.</p>
	 */
	public static final Duration DEFAULT_AUTO_SAVE_INTERVAL = Duration.ofMinutes(5);

	private final AtomicBoolean wasDockEventReceived =
			new AtomicBoolean(false);
	private final AtomicBoolean closed = new AtomicBoolean();

	/**
	 * Listener registered on each {@link Bento}'s event bus.
	 *
	 * <p>Held in a field, not created per call: {@code removeEventListener} is a
	 * list removal, so add and remove must be handed the same instance or the
	 * registration leaks past {@link #close()}.</p>
	 */
	private final DockEventListener dockEventListener = this::markLayoutDirty;

	/**
	 * Guards the auto-save lifecycle: {@link #isAutoSaveEnabled},
	 * {@link #scheduler}, {@link #scheduledSaveTask} and {@link #listenerBentos}.
	 *
	 * <p>These are touched from at least three threads - whichever thread arms or
	 * disarms auto-save, the scheduler thread, and whichever thread calls
	 * {@link #close()} - so enabling, disabling and closing have to be mutually
	 * exclusive. Without it, interleaved calls could leave a scheduler running
	 * with no handle to cancel it, or leave {@link #listenerBentos} out of step
	 * with the registrations actually held on each {@link Bento}'s event bus,
	 * leaking listeners past close.</p>
	 *
	 * <p><b>A save must never run while this lock is held.</b> Saving hands work
	 * to the JavaFX application thread and waits for it, and the JavaFX thread may
	 * itself call {@link #enableAutoSave(long, TimeUnit)} or {@link #close()} -
	 * holding the lock across a save would deadlock the two against each other.
	 * {@link #close()} therefore saves first and only then takes the lock to tear
	 * down.</p>
	 */
	private final Object autoSaveLock = new Object();

	/** Guarded by {@link #autoSaveLock}. */
	private final Set<Bento> listenerBentos = new HashSet<>();

	/**
	 * Written under {@link #autoSaveLock}; {@code volatile} so
	 * {@link #isAutoSaveEnabled()} can read it without contending for the lock,
	 * which matters because that reader may be the JavaFX thread.
	 */
	private volatile boolean isAutoSaveEnabled;

	/** Guarded by {@link #autoSaveLock}. */
	private @Nullable ScheduledExecutorService scheduler;

	/** Guarded by {@link #autoSaveLock}. */
	private @Nullable ScheduledFuture<?> scheduledSaveTask;

	/**
	 * The {@link BentoProvider} containing the {@link Bento} instances whose
	 * {@link DockEvent}s will be used to determine whether the docking layout
	 * should be saved.
	 */
	protected final BentoProvider bentoProvider;


	/**
	 * Constructs an {@code AbstractAutoCloseableLayoutSaver}.
	 *
	 * <p>Auto-save is <b>not</b> started here - see the class documentation for
	 * why. Call {@link #enableAutoSave(long, TimeUnit)} once construction is
	 * complete, or use {@link #startAutoSave(AbstractAutoCloseableLayoutSaver)}
	 * which does that for you.</p>
	 *
	 * @param bentoProvider The {@link BentoProvider} containing the
	 *                      {@link Bento} instances whose {@link DockEvent}s
	 *                      will be used to determine whether the docking layout
	 *                      should be saved.
	 */
	protected AbstractAutoCloseableLayoutSaver(
			final BentoProvider bentoProvider
	) {

		this.bentoProvider = Objects.requireNonNull(bentoProvider);
	}

	/**
	 * Performs the actual save. Called by {@link #saveLayout()} and (via
	 * {@link #autoSave}) by scheduled saves and {@link #close()}.
	 *
	 * <p>Subclasses implement this instead of overriding
	 * {@link #saveLayout()} so the "dirty" flag's clear-on-success invariant
	 * stays in one place.</p>
	 *
	 * @throws BentoStateException when the layout cannot be saved.
	 */
	protected abstract void saveLayoutInternal() throws BentoStateException;

	/**
	 * Starts auto-save on a fully constructed saver at the default interval and
	 * returns it, so a factory can build and arm an instance in one expression.
	 *
	 * <p>This is the safe counterpart to enabling auto-save from the constructor:
	 * by the time this runs, every constructor in the hierarchy has completed, so
	 * the scheduler thread and the {@link Bento} event buses only ever see a
	 * fully initialized object.</p>
	 *
	 * @param saver the saver to start auto-saving.
	 * @param <T> the saver type, preserved so callers do not have to cast.
	 * @return the supplied saver, with auto-save running.
	 */
	public static <T extends AbstractAutoCloseableLayoutSaver> T startAutoSave(
			final T saver
	) {
		Objects.requireNonNull(saver);
		saver.enableAutoSave(
				DEFAULT_AUTO_SAVE_INTERVAL.toMinutes(),
				TimeUnit.MINUTES
		);
		return saver;
	}

	/**
	 * Returns {@code true} when auto save is enabled; otherwise, returns
	 * {@code false}.
	 *
	 * @return {@code true} when auto save is enabled; otherwise, returns
	 * {@code false}.
	 */
	public boolean isAutoSaveEnabled() {
		return isAutoSaveEnabled;
	}

	/**
	 * Enables functionality to automatically save the docking layout at the
	 * specified interval.
	 *
	 * @param autoSaveInterval the interval for which automatic saving will be
	 *                         scheduled.
	 * @param autoSaveTimeUnit the unit of time in which the
	 *                         {@code autoSaveInterval} will be scheduled.
	 * @see #disableAutoSave() to disable automatic saving.
	 */
	public void enableAutoSave(
			final long autoSaveInterval,
			final TimeUnit autoSaveTimeUnit
	) {
		if (autoSaveInterval <= 0) {
			throw new IllegalArgumentException("autoSaveInterval must be greater than zero");
		}

		final TimeUnit requestedTimeUnit =
				Objects.requireNonNull(autoSaveTimeUnit);

		synchronized (autoSaveLock) {
			// Checked inside the lock, together with the state it protects. Read
			// outside, a close running concurrently could set the flag after the
			// check and have its teardown undone by the scheduler started below.
			if (closed.get()) {
				throw new IllegalStateException("Cannot enable auto-save after saver has been closed");
			}

			disableAutoSaveInternal();

			final ScheduledExecutorService newScheduler =
					Executors.newSingleThreadScheduledExecutor(
							new AutoSaveThreadFactory()
					);
			this.scheduler = newScheduler;
			this.scheduledSaveTask = newScheduler.scheduleAtFixedRate(
					() -> autoSave(false),
					autoSaveInterval,
					autoSaveInterval,
					requestedTimeUnit
			);
			addListeners();

			// Set last: the flag says auto-save is fully armed, so nothing should
			// observe it as true while the scheduler or listeners are half set up.
			this.isAutoSaveEnabled = true;
		}
	}

	/**
	 * Disables functionality to automatically save the docking layout.
	 *
	 * @see #enableAutoSave(long, TimeUnit) to enable automatic saving.
	 */
	public void disableAutoSave() {
		synchronized (autoSaveLock) {
			disableAutoSaveInternal();
		}
	}

	/**
	 * Tears down the scheduler and listeners.
	 *
	 * <p>Must be called holding {@link #autoSaveLock}. Exists separately from
	 * {@link #disableAutoSave()} so {@link #enableAutoSave(long, TimeUnit)} can
	 * reset state within a single critical section rather than releasing the lock
	 * between disabling and re-arming - a gap in which another thread could
	 * observe auto-save as neither on nor off, or interleave its own teardown.</p>
	 *
	 * <p>Returns {@code void} rather than the stopped scheduler so callers that
	 * only need the teardown do not surface it as an {@code AutoCloseable} with
	 * no owner. {@link #close()} captures {@link #scheduler} directly before
	 * calling in; both reads happen under {@code autoSaveLock}.</p>
	 */
	private void disableAutoSaveInternal() {

		this.isAutoSaveEnabled = false;

		final ScheduledFuture<?> currentScheduledSaveTask = scheduledSaveTask;
		scheduledSaveTask = null;

		if (currentScheduledSaveTask != null) {
			currentScheduledSaveTask.cancel(false);
		}

		final ScheduledExecutorService currentScheduler = scheduler;
		scheduler = null;

		if (currentScheduler != null) {
			currentScheduler.shutdownNow();
		}

		removeListeners();
	}

	/**
	 * Waits, briefly, for a scheduler just shut down to finish the save it may
	 * have been running.
	 *
	 * <p>Called without holding {@link #autoSaveLock}: the save being waited on
	 * never takes that lock, but it may be waiting on the JavaFX application
	 * thread, which may itself be waiting for the lock. Shutting the scheduler
	 * down interrupted that save, so the wait is normally short; the bound is
	 * {@link PersistenceThreading#FX_CLOSE_TIMEOUT_MILLIS}, the budget closing
	 * already allows itself.</p>
	 *
	 * @param stoppedScheduler the scheduler that was shut down, or {@code null}
	 * when auto-save was not running.
	 */
	private static void awaitScheduledSave(
			final @Nullable ScheduledExecutorService stoppedScheduler
	) {
		if (stoppedScheduler == null) {
			return;
		}

		try {
			if (!stoppedScheduler.awaitTermination(
					PersistenceThreading.FX_CLOSE_TIMEOUT_MILLIS,
					TimeUnit.MILLISECONDS
			)) {
				logger.warn(
						"A scheduled save was still running when the saver " +
								"closed; the final save may miss its changes."
				);
			}
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.warn(
					"Interrupted waiting for a scheduled save to finish; " +
							"the final save may miss its changes.",
					e
			);
		}
	}

	/**
	 * Marks the layout as changed, so the next auto-save has something to do.
	 *
	 * <p>Package-private rather than public: this is what the event bus calls
	 * through {@link #dockEventListener}, not part of this saver's surface.</p>
	 *
	 * @param event the {@link DockEvent} that changed the layout.
	 */
	void markLayoutDirty(final DockEvent event) {
		logger.trace("Dock event received: {}", event);
		this.wasDockEventReceived.set(true);
	}

	@Override
	public void close() {

		if (!closed.compareAndSet(false, true)) {
			return;
		}

		// Stop the timer first, and wait for a scheduled save already under way.
		// Such a save has already cleared the "changed" flag; stopping it
		// interrupts it, its failure puts the flag back, and only then is the
		// flag a true answer to whether the final save below has work to do.
		final ScheduledExecutorService stoppedScheduler;
		synchronized (autoSaveLock) {
			// Capture under the lock, before the field is set to null, so the
			// following wait runs outside the lock on the already-shut-down
			// handle.
			stoppedScheduler = scheduler;
			disableAutoSaveInternal();
		}
		awaitScheduledSave(stoppedScheduler);

		try {
			// Deliberately not gated on isAutoSaveEnabled. Saving on close and
			// saving on a timer are independent concerns, and the class
			// documentation promises try-with-resources will save on exit. Gating
			// meant disableAutoSave() silently also disabled save-on-exit, and
			// now that the constructor no longer starts auto-save it would also
			// mean a directly constructed saver never flushed at all. autoSave
			// still short-circuits when no dock events have been received, so a
			// close with nothing to save remains cheap.
			//
			// Note this runs OUTSIDE autoSaveLock, on purpose. The save waits on
			// the JavaFX application thread, which may itself be calling into
			// enableAutoSave or close; holding the lock across it would deadlock.
			// The compareAndSet above already makes this branch run once, so no
			// second close can save concurrently, and disableAutoSaveInternal
			// below is what actually needs the lock.
			autoSave(true);
		} finally {
			disableAutoSave();
		}
	}

	/**
	 * Saves the layout, then clears the "dirty" flag so a subsequent
	 * {@link #close()} and its {@link #autoSave} short-circuit when no
	 * {@link DockEvent} has arrived since. This matters when a caller
	 * saves explicitly and then closes: without the clear, {@code close()}
	 * would see the flag still set and save a second time.
	 *
	 * <p>Subclasses implement {@link #saveLayoutInternal()} to perform the
	 * actual save; this method is {@code final} so the clear-on-success
	 * invariant stays here. If {@code saveLayoutInternal()} throws, the flag is
	 * left as it was, so the next {@code close()} or scheduled save still has
	 * work to do.</p>
	 *
	 * @throws BentoStateException when the layout cannot be saved.
	 */
	@Override
	public final void saveLayout() throws BentoStateException {
		saveLayoutInternal();
		wasDockEventReceived.set(false);
	}

	/**
	 * Saves the layout while the application is shutting down.
	 *
	 * <p>Separate from {@link #saveLayout()} so an implementation can apply a
	 * tighter budget to any wait it performs on another thread. The thread
	 * calling {@link #close()} may not be a daemon, so a long wait here can
	 * delay or block application exit. The default implementation simply
	 * delegates to {@link #saveLayout()}.</p>
	 *
	 * @throws BentoStateException when the layout cannot be saved.
	 */
	protected void saveLayoutForShutdown() throws BentoStateException {
		saveLayout();
	}

	/**
	 * Called when the scheduled interval to automatically save the docking
	 * layout expires, this method ensures the layout is only saved when
	 * {@link DockEvent}s have occurred since the last save, indicating
	 * changes have been made that need to be saved.
	 *
	 * @param isShuttingDown {@code true} when called from {@link #close()}, which
	 * routes the save through {@link #saveLayoutForShutdown()} so a tighter
	 * budget can apply while the application is exiting.
	 */
	private void autoSave(final boolean isShuttingDown) {
		if (!wasDockEventReceived.getAndSet(false)) {
			logger.debug(
					"No dock events have been received; " +
							"will not attempt to save layout."
			);
			return;
		}

		logger.debug(
				"Dock events have been received; " +
						"attempting to save layout."
		);

		try {
			if (isShuttingDown) {
				saveLayoutForShutdown();
			} else {
				saveLayout();
			}
		} catch (final BentoStateException | RuntimeException e) {
			// The change was not saved, so it is still pending: the next
			// scheduled save, or close(), has to try again. Leaving the flag
			// cleared would treat an unsaved layout as saved.
			wasDockEventReceived.set(true);

			logger.error(
					"Could not auto-save docking layout",
					e
			);
		}
	}

	/**
	 * Registers this saver on the event bus of every available {@link Bento}.
	 *
	 * <p>Must be called holding {@link #autoSaveLock}: {@code listenerBentos} is
	 * the only record of which buses hold a registration, so a concurrent
	 * add/remove could leave it disagreeing with reality and leak a listener.</p>
	 */
	private void addListeners() {
		for (final Bento bento : bentoProvider.getAllBentos()) {
			if (listenerBentos.add(bento)) {
				bento.events().addEventListener(dockEventListener);
			}
		}
	}

	/**
	 * Unregisters this saver from every {@link Bento} it registered with.
	 *
	 * <p>Must be called holding {@link #autoSaveLock} - see
	 * {@link #addListeners()}.</p>
	 */
	private void removeListeners() {
		for (final Bento bento : listenerBentos) {
			bento.events().removeEventListener(dockEventListener);
		}

		listenerBentos.clear();
	}

	private static final class AutoSaveThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread(final Runnable runnable) {
			final Thread thread = new Thread(runnable, "bentofx-layout-auto-save");
			thread.setDaemon(true);
			return thread;
		}
	}
}
