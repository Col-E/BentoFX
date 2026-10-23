package software.coley.bentofx.persistence.core.impl;

import javafx.application.Platform;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import software.coley.bentofx.Bento;
import software.coley.bentofx.event.DockEvent;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;
import software.coley.bentofx.persistence.testfixtures.codec.InMemoryLayoutCodec;
import software.coley.bentofx.persistence.testfixtures.storage.InMemoryLayoutStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Concurrency coverage for the auto-save lifecycle.
 *
 * <p>{@code isAutoSaveEnabled}, {@code scheduler}, {@code scheduledSaveTask} and
 * {@code listenerBentos} are reached from whichever thread arms or disarms
 * auto-save, from the scheduler thread, and from whichever thread calls
 * {@code close()}. They were plain unsynchronized fields, so interleaved calls
 * could leave a scheduler running with no handle to cancel it, or leave the
 * listener set disagreeing with the registrations actually held on each
 * {@link Bento}'s event bus - leaking listeners past close.</p>
 *
 * <p>Races cannot be reproduced deterministically, so these tests hammer the
 * lifecycle from several threads at once and then assert the invariants that must
 * hold regardless of interleaving: no thread left alive, no listener left
 * registered, and no exception escaping. Asserting end-state invariants rather
 * than timing is what makes them reliable - in practice all three failed on every
 * one of six runs against the unsynchronized version, with
 * {@code ConcurrentModificationException}, five leaked scheduler threads, and
 * {@code RejectedExecutionException}, and passed six for six once guarded.</p>
 *
 * <p>The deadlock test matters just as much as the race tests. The obvious fix -
 * synchronizing every public method - would deadlock, because {@code close()}
 * saves, saving waits on the JavaFX application thread, and the JavaFX thread may
 * itself be calling {@code enableAutoSave}. That test fails by timing out rather
 * than by assertion.</p>
 *
 * @author Phil Bryant
 */
@ExtendWith(ApplicationExtension.class)
class LayoutSaverAutoSaveLifecycleITG {

	private static final int THREADS = 4;
	private static final int ITERATIONS = 40;
	private static final long TIMEOUT_SECONDS = 30L;

	/**
	 * How long a scheduler thread may take to notice its interrupt and exit before
	 * it is treated as leaked.
	 */
	private static final long SHUTDOWN_GRACE_SECONDS = 5L;

	/** Thread name used by the saver's auto-save executor. */
	private static final String AUTO_SAVE_THREAD_NAME = "bentofx-layout-auto-save";

	/**
	 * Concurrent enable/disable churn must leave the saver consistent and must not
	 * leak the daemon scheduler thread it starts. Each {@code enableAutoSave} spins
	 * up a new single-thread executor, so a lost handle shows up as a surviving
	 * thread named {@code bentofx-layout-auto-save}.
	 */
	@Test
	void concurrentEnableAndDisableLeavesNoSchedulerRunning() throws Exception {

		try (DockingLayoutSaver saver = newSaver(new DefaultBentoProvider())) {
			runConcurrently(index -> {
				for (int i = 0; i < ITERATIONS; i++) {
					if ((i + index) % 2 == 0) {
						saver.enableAutoSave(1L, TimeUnit.HOURS);
					} else {
						saver.disableAutoSave();
					}
				}
			});

			saver.disableAutoSave();

			assertThat(saver.isAutoSaveEnabled())
					.describedAs("saver.isAutoSaveEnabled() after churn and a final disable")
					.isFalse();
		}

		assertNoAutoSaveThreadsSurvive();
	}

	/**
	 * The listener-leak invariant. {@code listenerBentos} is the only record of
	 * which event buses hold a registration, so if concurrent arming and disarming
	 * can corrupt it, a listener survives the close and the saver keeps observing
	 * a {@link Bento} it has released.
	 */
	@Test
	void concurrentEnableAndDisableLeavesNoListenerRegistered() throws Exception {
		final Bento bento = new Bento("bento-lifecycle-listeners");
		final BentoProvider bentoProvider = new DefaultBentoProvider(bento);

		// A counting subclass, so the test can see whether the saver is still
		// receiving events after close rather than inferring it from side effects.
		try (EventCountingSaver saver = new EventCountingSaver(bentoProvider)) {

			runConcurrently(index -> {
				for (int i = 0; i < ITERATIONS; i++) {
					if ((i + index) % 2 == 0) {
						saver.enableAutoSave(1L, TimeUnit.HOURS);
					} else {
						saver.disableAutoSave();
					}
				}
			});

			// Closed explicitly rather than left to the resource block, because the
			// assertions below are about the post-close state and must observe it.
			saver.close();

			final int eventsBeforeProbe = saver.eventCount();

			// Nothing should be subscribed now. A leaked registration means this
			// event still reaches the saver.
			bento.events().fire(
					new DockEvent.RootContainerAdded(
							bento.dockBuilding().root("root-lifecycle-listeners")
					)
			);

			assertThat(saver.eventCount())
					.describedAs("dock events received after close (0 delta means unsubscribed)")
					.isEqualTo(eventsBeforeProbe);
			assertThat(saver.isAutoSaveEnabled())
					.describedAs("saver.isAutoSaveEnabled() after close")
					.isFalse();
		}
	}

	/**
	 * Concurrent {@code close()} against lifecycle churn must not throw, and must
	 * still end with auto-save off. This is the interleaving that could previously
	 * have a scheduler armed after teardown had already run, because the
	 * closed-flag check and the state it protects were not read together.
	 */
	@Test
	void concurrentCloseAndEnableEndsDisabled() throws Exception {
		final CountDownLatch startLine = new CountDownLatch(1);

		// The saver is closed by one of the workers below; the outer
		// try-with-resources is a backstop for the paths that do not reach it, and
		// is harmless because close() is documented idempotent.
		try (DockingLayoutSaver saver = newSaver(new DefaultBentoProvider())) {

			try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
				final List<Future<?>> futures = List.of(
						executor.submit(() -> {
							awaitStart(startLine);
							saver.close();
						}),
						executor.submit(() -> {
							awaitStart(startLine);
							for (int i = 0; i < ITERATIONS; i++) {
								try {
									saver.enableAutoSave(1L, TimeUnit.HOURS);
								} catch (final IllegalStateException expected) {
									// Enabling after close is a documented
									// rejection, not a race symptom.
								}
							}
						}),
						executor.submit(() -> {
							awaitStart(startLine);
							for (int i = 0; i < ITERATIONS; i++) {
								saver.disableAutoSave();
							}
						})
				);

				startLine.countDown();

				for (final Future<?> future : futures) {
					future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
				}
			}

			assertThat(saver.isAutoSaveEnabled())
					.describedAs("saver.isAutoSaveEnabled() after concurrent close and churn")
					.isFalse();
		}

		assertNoAutoSaveThreadsSurvive();
	}

	/**
	 * Guards against the naive fix. Synchronizing the whole of {@code close()}
	 * would deadlock: the close saves, the save waits on the JavaFX application
	 * thread, and here the JavaFX thread is simultaneously calling
	 * {@code enableAutoSave} - which would need the same lock. Failure mode is a
	 * timeout, not an assertion.
	 */
	@Test
	void closeDoesNotDeadlockAgainstFxThreadEnablingAutoSave() throws Exception {
		final Bento bento = new Bento("bento-lifecycle-deadlock");
		final CountDownLatch startLine = new CountDownLatch(1);
		final AtomicReference<@Nullable Throwable> failure =
				new AtomicReference<>();

		try (DockingLayoutSaver saver = newSaver(new DefaultBentoProvider(bento))) {

			// Give close() real work to do, so it actually reaches the JavaFX
			// thread rather than short-circuiting on "no dock events received".
			saver.markLayoutDirty(
					new DockEvent.RootContainerAdded(
							bento.dockBuilding().root("root-lifecycle-deadlock")
					)
			);

			final Thread fxChurn = new Thread(() -> {
				awaitStart(startLine);
				for (int i = 0; i < ITERATIONS; i++) {
					try {
						Platform.runLater(() -> {
							try {
								saver.enableAutoSave(1L, TimeUnit.HOURS);
							} catch (final IllegalStateException expected) {
								// Enabling after close is documented behavior.
							} catch (final RuntimeException e) {
								failure.compareAndSet(null, e);
							}
						});
					} catch (final IllegalStateException toolkitGone) {
						return;
					}
				}
			}, "fx-lifecycle-churn");
			fxChurn.setDaemon(true);

			final Thread closer = new Thread(() -> {
				awaitStart(startLine);
				try {
					saver.close();
				} catch (final RuntimeException e) {
					failure.compareAndSet(null, e);
				}
			}, "lifecycle-closer");
			closer.setDaemon(true);

			fxChurn.start();
			closer.start();
			startLine.countDown();

			closer.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
			fxChurn.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));

			if (closer.isAlive()) {
				fail("close() did not complete within " + TIMEOUT_SECONDS
						+ "s while the JavaFX thread was enabling auto-save - "
						+ "the auto-save lock is most likely held across a save");
			}
		}

		assertThat(failure.get())
				.describedAs("unexpected exception during concurrent close/enable")
				.isNull();
	}

	/**
	 * Runs {@code body} on several threads that all start together, and rethrows
	 * whatever any of them threw.
	 */
	private static void runConcurrently(final IndexedTask body) throws Exception {
		final CountDownLatch startLine = new CountDownLatch(1);

		try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
			final List<Future<?>> futures = new ArrayList<>();
			for (int index = 0; index < THREADS; index++) {
				final int threadIndex = index;
				futures.add(executor.submit(() -> {
					awaitStart(startLine);
					body.run(threadIndex);
				}));
			}

			startLine.countDown();

			for (final Future<?> future : futures) {
				future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			}
		}
	}

	/**
	 * Asserts no auto-save scheduler thread outlives the saver. Each
	 * {@code enableAutoSave} creates a fresh single-thread executor, so a handle
	 * lost to a race leaves its thread alive with nothing able to shut it down.
	 *
	 * <p>The saver's teardown calls {@code shutdownNow}, which only <em>requests</em>
	 * termination, so a thread can still be briefly alive here for legitimate
	 * reasons. Rather than sample repeatedly and hope, this joins each surviving
	 * thread: a thread being shut down ends promptly, while a genuinely leaked one
	 * never does and the join expires. That turns the check into a wait on the
	 * actual event instead of a poll for its absence.</p>
	 */
	private static void assertNoAutoSaveThreadsSurvive() throws InterruptedException {
		final long deadline =
				System.nanoTime() + TimeUnit.SECONDS.toNanos(SHUTDOWN_GRACE_SECONDS);

		for (final Thread thread : findAutoSaveThreads()) {
			final long remainingNanos = deadline - System.nanoTime();
			if (remainingNanos <= 0) {
				break;
			}

			thread.join(TimeUnit.NANOSECONDS.toMillis(remainingNanos) + 1L);
		}

		assertThat(findAutoSaveThreads())
				.describedAs("surviving bentofx-layout-auto-save threads")
				.isEmpty();
	}

	private static List<Thread> findAutoSaveThreads() {
		return Thread.getAllStackTraces().keySet().stream()
				.filter(Thread::isAlive)
				.filter(thread -> AUTO_SAVE_THREAD_NAME.equals(thread.getName()))
				.toList();
	}

	private static void awaitStart(final CountDownLatch startLine) {
		try {
			if (!startLine.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Threads never released from the start line");
			}
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	private static DockingLayoutSaver newSaver(
			final BentoProvider bentoProvider
	) {
		return new DockingLayoutSaver(
				new InMemoryLayoutCodec(),
				new InMemoryLayoutStorage(),
				bentoProvider
		);
	}

	@FunctionalInterface
	private interface IndexedTask {
		void run(int index);
	}

	/**
	 * A saver that counts the dock events delivered to it, so a test can tell
	 * whether it is still subscribed to a {@link Bento}'s event bus.
	 */
	private static final class EventCountingSaver extends DockingLayoutSaver {

		private final AtomicInteger events = new AtomicInteger();

		private EventCountingSaver(final BentoProvider bentoProvider) {
			super(
					new InMemoryLayoutCodec(),
					new InMemoryLayoutStorage(),
					bentoProvider
			);
		}

		@Override
		void markLayoutDirty(final DockEvent event) {
			events.incrementAndGet();
			super.markLayoutDirty(event);
		}

		private int eventCount() {
			return events.get();
		}
	}
}
