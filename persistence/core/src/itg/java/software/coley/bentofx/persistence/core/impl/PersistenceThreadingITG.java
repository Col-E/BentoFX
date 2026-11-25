package software.coley.bentofx.persistence.core.impl;

import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.BentoStateTimeoutException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.*;
import static software.coley.bentofx.persistence.core.impl.PersistenceThreading.OFF_FX_EXECUTOR_THREAD_NAME;

@ExtendWith(ApplicationExtension.class)
class PersistenceThreadingITG {

	private static final String RAN_ON_FX_THREAD = "ranOnFxThread";
	private static final String RAN_ON_FX_THREAD_GET_DESCRIPTION = "ranOnFxThread.get()";

	/**
	 * Budget used by the timeout tests. Small enough to keep them fast, large
	 * enough that a loaded continuous integration machine will not trip it
	 * spuriously in the tests that expect success.
	 */
	private static final long SHORT_TIMEOUT_MILLIS = 250L;

	/**
	 * How long to wait for a latch that is expected to be released promptly.
	 * Generous, because exceeding it fails the test rather than slowing it.
	 */
	private static final long LATCH_TIMEOUT_SECONDS = 10L;

	/**
	 * How long the JavaFX application thread is deliberately held in the one test
	 * that expects a late task to still succeed. Long enough that the call under
	 * test submits its task while the thread is still busy, and far shorter than
	 * the budget that call allows.
	 */
	private static final long FX_BUSY_MILLIS = 50L;

	@Test
	void callOnFxThreadRunsImmediatelyWhenAlreadyOnFxThread(FxRobot robot) {
		final AtomicReference<Boolean> ranOnFxThread = new AtomicReference<>();

		robot.interact(() -> {
			try {
				ranOnFxThread.set(
						PersistenceThreading.callOnFxThread(
								Platform::isFxApplicationThread
						)
				);
			} catch (final BentoStateException e) {
				throw new AssertionError(e);
			}
		});

		assertThat(ranOnFxThread.get())
				.describedAs(RAN_ON_FX_THREAD_GET_DESCRIPTION)
				.isTrue();
	}

	@Test
	void callOnFxThreadDispatchesWhenCalledOffFxThread() throws BentoStateException {
		final Boolean ranOnFxThread = PersistenceThreading.callOnFxThread(
				Platform::isFxApplicationThread
		);

		assertThat(ranOnFxThread)
				.describedAs(RAN_ON_FX_THREAD)
				.isTrue();
	}

	@Test
	void callOffFxThreadDispatchesWhenCalledOnFxThread(FxRobot robot) {
		final AtomicReference<Boolean> ranOnFxThread = new AtomicReference<>();

		robot.interact(() -> {
			try {
				ranOnFxThread.set(
						PersistenceThreading.callOffFxThread(
								Platform::isFxApplicationThread
						)
				);
			} catch (final BentoStateException e) {
				throw new AssertionError(e);
			}
		});

		assertThat(ranOnFxThread.get())
				.describedAs(RAN_ON_FX_THREAD_GET_DESCRIPTION)
				.isFalse();
	}

	/**
	 * Successive hand-offs must reuse one shared worker rather than create a
	 * thread per call. Also pins the naming, which is what makes the thread
	 * identifiable in a dump of a frozen UI.
	 */
	@Test
	void callOffFxThreadReusesOneSharedThreadAcrossCalls(FxRobot robot) {

		final AtomicReference<Thread> firstThread = new AtomicReference<>();
		final AtomicReference<Thread> secondThread = new AtomicReference<>();

		robot.interact(() -> {
			try {
				firstThread.set(PersistenceThreading.callOffFxThread(Thread::currentThread));
				secondThread.set(PersistenceThreading.callOffFxThread(Thread::currentThread));
			} catch (final BentoStateException e) {
				throw new AssertionError(e);
			}
		});

		assertThat(firstThread.get())
				.describedAs("thread running the first hand-off")
				.isNotNull()
				.isSameAs(secondThread.get());
		assertThat(firstThread.get().getName())
				.describedAs("worker thread name")
				.isEqualTo(OFF_FX_EXECUTOR_THREAD_NAME);
		assertThat(firstThread.get().isDaemon())
				.describedAs("worker thread is a daemon")
				.isTrue();
	}

	/*
	 * The following tests use unbounded wait on the JavaFX application thread
	 * to hang the caller forever. The production trigger is calling close()
	 * after the JavaFX toolkit has shut down: Platform.runLater then accepts a
	 * task and silently never runs it, and JavaFX exposes no public way to ask
	 * whether the toolkit is alive. These tests cannot shut the toolkit down -
	 * it is not restartable, and this suite shares one JVM, and doing so would
	 * break every test that follows.
	 *
	 * Instead, they reproduce the condition the hang depends on, which is the
	 * part that actually matters: a task accepted by runLater that does not run
	 * within its budget. Occupying the JavaFX thread with a latch produces
	 * exactly that state and is fully reversible.
	 */

	@Test
	void callOnFxThreadTimesOutInsteadOfHangingWhenFxThreadNeverRunsTask()
			throws InterruptedException {
		final CountDownLatch fxThreadOccupied = new CountDownLatch(1);
		final CountDownLatch releaseFxThread = new CountDownLatch(1);

		occupyFxThread(fxThreadOccupied, releaseFxThread);

		try {
			assertThat(fxThreadOccupied.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS))
					.describedAs("JavaFX thread became occupied")
					.isTrue();

			// Before the fix this call never returned.
			assertThatThrownBy(() ->
					PersistenceThreading.callOnFxThread(
							() -> Boolean.TRUE,
							SHORT_TIMEOUT_MILLIS
					)
			)
					.describedAs("callOnFxThread with an occupied JavaFX thread")
					.isInstanceOf(BentoStateTimeoutException.class)
					.hasMessageContaining(String.valueOf(SHORT_TIMEOUT_MILLIS));
		} finally {
			releaseFxThread.countDown();
		}

		drainFxQueue();
	}

	/**
	 * A timed-out task must be canceled, not merely abandoned. It is still
	 * queued on the JavaFX thread at that point, so without the cancel it runs
	 * once the thread frees up and mutates JavaFX state on behalf of a caller
	 * that already gave up and, in the restore case, already fell back to a
	 * different layout.
	 */
	@Test
	void callOnFxThreadCancelsTimedOutTaskSoItNeverRuns()
			throws InterruptedException {

		final CountDownLatch fxThreadOccupied = new CountDownLatch(1);
		final CountDownLatch releaseFxThread = new CountDownLatch(1);
		final AtomicBoolean taskRan = new AtomicBoolean();

		occupyFxThread(fxThreadOccupied, releaseFxThread);

		try {
			assertThat(fxThreadOccupied.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS))
					.describedAs("JavaFX thread became occupied")
					.isTrue();

			assertThatThrownBy(() ->
					PersistenceThreading.callOnFxThread(
							() -> {
								taskRan.set(true);
								return Boolean.TRUE;
							},
							SHORT_TIMEOUT_MILLIS
					)
			)
					.describedAs("task abandoned after the timeout elapsed")
					.isInstanceOf(BentoStateTimeoutException.class);
		} finally {
			releaseFxThread.countDown();
		}

		// Let the JavaFX thread work through anything still queued, including
		// the abandoned task, before checking that it did not execute.
		drainFxQueue();

		assertThat(taskRan)
				.describedAs("timed-out task ran after being abandoned")
				.isFalse();
	}

	/**
	 * The waiting thread's own interrupt, not the JavaFX thread's failure to
	 * run the task, is what {@link PersistenceThreading#callOnFxThread}'s
	 * {@code InterruptedException} branch exists for. Unlike the timeout
	 * tests above, this needs an ordinary worker thread to interrupt - the
	 * JavaFX application thread itself must never be interrupted, since
	 * JavaFX gives no guarantee about its behavior afterward and the rest of
	 * this suite shares that one thread.
	 */
	@Test
	void callOnFxThreadWrapsAnInterruptAsBentoStateExceptionAndRestoresTheFlag()
			throws InterruptedException {
		final CountDownLatch fxThreadOccupied = new CountDownLatch(1);
		final CountDownLatch releaseFxThread = new CountDownLatch(1);
		occupyFxThread(fxThreadOccupied, releaseFxThread);

		final AtomicReference<BentoStateException> caught = new AtomicReference<>();
		final AtomicBoolean interruptFlagAfterCatch = new AtomicBoolean();

		final Thread worker = new Thread(() -> {
			try {
				PersistenceThreading.callOnFxThread(
						() -> Boolean.TRUE,
						TimeUnit.SECONDS.toMillis(LATCH_TIMEOUT_SECONDS)
				);
			} catch (final BentoStateException e) {
				caught.set(e);
				interruptFlagAfterCatch.set(Thread.currentThread().isInterrupted());
			}
		}, "callOnFxThread-interrupt-test");
		worker.setDaemon(true);

		try {
			assertThat(fxThreadOccupied.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS))
					.describedAs("JavaFX thread became occupied")
					.isTrue();

			worker.start();
			awaitWaitingState(worker);
			worker.interrupt();
			worker.join(TimeUnit.SECONDS.toMillis(LATCH_TIMEOUT_SECONDS));
		} finally {
			releaseFxThread.countDown();
		}

		assertThat(worker.isAlive())
				.describedAs("worker thread after being interrupted")
				.isFalse();
		assertThat(caught.get())
				.describedAs("exception thrown by callOnFxThread when the waiting thread is interrupted")
				.isNotNull()
				.hasMessage("Interrupted while waiting for persistence task")
				.cause()
				.isInstanceOf(InterruptedException.class);
		assertThat(interruptFlagAfterCatch)
				.describedAs("interrupt flag restored after catching the interrupt")
				.isTrue();

		drainFxQueue();
	}

	/**
	 * Guards the other direction: the budget exists to detect a task that will
	 * never run, not one that is briefly late. A task whose turn comes up within
	 * the budget must still succeed.
	 */
	@Test
	void callOnFxThreadSucceedsWhenFxThreadIsBusyButFreesUpWithinBudget()
			throws InterruptedException, BentoStateException {
		final CountDownLatch fxThreadOccupied = new CountDownLatch(1);
		final CountDownLatch releaseFxThread = new CountDownLatch(1);

		// Hold the JavaFX thread only briefly, so it frees itself well inside the
		// budget the call below gives it.
		occupyFxThread(fxThreadOccupied, releaseFxThread, FX_BUSY_MILLIS);

		assertThat(
				fxThreadOccupied.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
		)
				.describedAs("JavaFX thread became occupied")
				.isTrue();

		try {
			final Boolean ranOnFxThread = PersistenceThreading.callOnFxThread(
					Platform::isFxApplicationThread,
					TimeUnit.SECONDS.toMillis(LATCH_TIMEOUT_SECONDS)
			);

			assertThat(ranOnFxThread)
					.describedAs(RAN_ON_FX_THREAD)
					.isTrue();
		} finally {
			releaseFxThread.countDown();
		}
	}

	/**
	 * A task's own unchecked failure must not be lost - it becomes the cause of
	 * the {@link BentoStateException} {@link PersistenceThreading#call} wraps it
	 * in.
	 */
	@Test
	void callOnFxThreadWrapsATaskFailureAsBentoStateExceptionWhenAlreadyOnFxThread(
			FxRobot robot
	) {
		final RuntimeException taskFailure = new RuntimeException("boom");
		final AtomicReference<BentoStateException> caught = new AtomicReference<>();

		robot.interact(() -> {
			try {
				PersistenceThreading.callOnFxThread(() -> {
					throw taskFailure;
				});
			} catch (final BentoStateException e) {
				caught.set(e);
			}
		});

		assertThat(caught.get())
				.describedAs("exception thrown by callOnFxThread when already on the FX thread and the task fails")
				.isInstanceOf(BentoStateException.class)
				.hasMessage("Persistence task failed")
				.hasCause(taskFailure);
	}

	/**
	 * A task that raises its own {@link BentoStateException} keeps that type and
	 * message rather than being wrapped a second time.
	 */
	@Test
	void callOnFxThreadRethrowsABentoStateExceptionTaskThrowsWithoutWrappingItAgainWhenAlreadyOnFxThread(
			FxRobot robot
	) {
		final BentoStateException taskFailure = new BentoStateException("task's own failure");
		final AtomicReference<BentoStateException> caught = new AtomicReference<>();

		robot.interact(() -> {
			try {
				PersistenceThreading.callOnFxThread(() -> {
					throw taskFailure;
				});
			} catch (final BentoStateException e) {
				caught.set(e);
			}
		});

		assertThat(caught.get())
				.describedAs("exception thrown by callOnFxThread when already on the FX thread and the task throws its own BentoStateException")
				.isSameAs(taskFailure);
	}

	/**
	 * The dispatched path - called off the FX thread, run via
	 * {@link Platform#runLater(Runnable)} - goes through {@code unwrap}'s
	 * {@link java.util.concurrent.ExecutionException} branch instead of {@code
	 * call}'s. Unlike the immediate path, an unchecked task failure here keeps
	 * the {@link java.util.concurrent.ExecutionException} {@link Future#get()}
	 * raised as the immediate cause, one level deeper than the task's own
	 * exception.
	 */
	@Test
	void callOnFxThreadWhenDispatchedWrapsATaskFailureAsBentoStateException() {
		final RuntimeException taskFailure = new RuntimeException("boom");

		final Throwable thrown = catchThrowable(() ->
				PersistenceThreading.callOnFxThread(() -> {
					throw taskFailure;
				})
		);

		assertThat(thrown)
				.describedAs("exception thrown by callOnFxThread dispatched to a failing task")
				.isInstanceOf(BentoStateException.class)
				.hasMessage("Persistence task failed");
		assertThat(thrown.getCause())
				.describedAs("cause of the exception thrown by callOnFxThread dispatched to a failing task")
				.isInstanceOf(ExecutionException.class)
				.hasCause(taskFailure);
	}

	@Test
	void callOnFxThreadWhenDispatchedRethrowsABentoStateExceptionTaskThrowsWithoutWrappingItAgain() {
		final BentoStateException taskFailure = new BentoStateException("task's own failure");

		assertThatThrownBy(() ->
				PersistenceThreading.callOnFxThread(() -> {
					throw taskFailure;
				})
		)
				.describedAs("exception thrown by callOnFxThread dispatched to a task that throws its own BentoStateException")
				.isSameAs(taskFailure);
	}

	/**
	 * The dispatched path - called on the FX thread, run on the shared
	 * off-thread executor - goes through {@code unwrap}'s {@link
	 * ExecutionException} branch instead of {@code call}'s. Unlike the
	 * immediate path, an unchecked task failure here keeps the {@link
	 * ExecutionException} {@link Future#get()} raised as the immediate cause,
	 * one level deeper than the task's own exception.
	 */
	@Test
	void callOffFxThreadWhenDispatchedWrapsATaskFailureAsBentoStateException(
			FxRobot robot
	) {
		final RuntimeException taskFailure = new RuntimeException("boom");
		final AtomicReference<BentoStateException> caught = new AtomicReference<>();

		robot.interact(() -> {
			try {
				PersistenceThreading.callOffFxThread(() -> {
					throw taskFailure;
				});
			} catch (final BentoStateException e) {
				caught.set(e);
			}
		});

		assertThat(caught.get())
				.describedAs("exception thrown by callOffFxThread dispatched to the shared executor when the task fails")
				.isInstanceOf(BentoStateException.class)
				.hasMessage("Persistence task failed");
		assertThat(caught.get().getCause())
				.describedAs("cause of the exception thrown by callOffFxThread dispatched to the shared executor when the task fails")
				.isInstanceOf(ExecutionException.class)
				.hasCause(taskFailure);
	}

	@Test
	void callOffFxThreadWhenDispatchedRethrowsABentoStateExceptionTaskThrowsWithoutWrappingItAgain(
			FxRobot robot
	) {
		final BentoStateException taskFailure = new BentoStateException("task's own failure");
		final AtomicReference<BentoStateException> caught = new AtomicReference<>();

		robot.interact(() -> {
			try {
				PersistenceThreading.callOffFxThread(() -> {
					throw taskFailure;
				});
			} catch (final BentoStateException e) {
				caught.set(e);
			}
		});

		assertThat(caught.get())
				.describedAs("exception thrown by callOffFxThread dispatched to the shared executor when the task throws its own BentoStateException")
				.isSameAs(taskFailure);
	}

	/**
	 * Occupies the JavaFX application thread until {@code releaseFxThread} is
	 * counted down, so that work submitted through
	 * {@link Platform#runLater(Runnable)} is accepted but cannot run.
	 *
	 * @param fxThreadOccupied counted down once the JavaFX thread is actually
	 * blocked, so callers do not race the submission.
	 * @param releaseFxThread awaited on the JavaFX thread; count it down to let
	 * the thread continue. Always count it down in a {@code finally} block:
	 * leaving the JavaFX thread blocked would break every later test.
	 */
	private static void occupyFxThread(
			final CountDownLatch fxThreadOccupied,
			final CountDownLatch releaseFxThread
	) {
		occupyFxThread(
				fxThreadOccupied,
				releaseFxThread,
				TimeUnit.SECONDS.toMillis(LATCH_TIMEOUT_SECONDS)
		);
	}

	/**
	 * Occupies the JavaFX application thread until {@code releaseFxThread} is
	 * counted down or {@code holdMillis} elapses, whichever comes first.
	 *
	 * @param fxThreadOccupied counted down once the JavaFX thread is actually
	 * blocked, so callers do not race the submission.
	 * @param releaseFxThread awaited on the JavaFX thread; count it down to let
	 * the thread continue. Always count it down in a {@code finally} block:
	 * leaving the JavaFX thread blocked would break every later test.
	 * @param holdMillis upper bound on the hold. For most callers this is the
	 * safety valve - a failing test must not leave the JavaFX thread blocked for
	 * the rest of the suite - and expiring means something went wrong. For a test
	 * that wants the thread to free itself, it is the release, and expiring is
	 * the expected outcome.
	 */
	private static void occupyFxThread(
			final CountDownLatch fxThreadOccupied,
			final CountDownLatch releaseFxThread,
			final long holdMillis
	) {
		Platform.runLater(() -> {
			fxThreadOccupied.countDown();
			try {
				// Bounded, and expiry is deliberately not treated as an error:
				// returning frees the thread exactly as counting the latch down
				// would. Interrupting it instead - the earlier safety valve - left
				// the interrupt flag set on the JavaFX thread for whatever ran
				// next.
				releaseFxThread.await(holdMillis, TimeUnit.MILLISECONDS);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
	}

	/**
	 * Polls until {@code thread} is blocked waiting on something, so a test can
	 * interrupt it deterministically instead of racing a fixed sleep against
	 * however long it takes the thread to reach its blocking call.
	 *
	 * <p>Parks between polls rather than sleeping, since {@link
	 * LockSupport#parkNanos} declares no {@code InterruptedException}.</p>
	 */
	private static void awaitWaitingState(final Thread thread) {
		final long deadline =
				System.nanoTime() + TimeUnit.SECONDS.toNanos(LATCH_TIMEOUT_SECONDS);

		while (System.nanoTime() < deadline) {
			final Thread.State state = thread.getState();
			if (state == Thread.State.TIMED_WAITING || state == Thread.State.WAITING) {
				return;
			}
			LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5L));
		}

		fail("worker thread never reached a waiting state");
	}

	/**
	 * Blocks until the JavaFX application thread has worked through everything
	 * currently queued. Because {@link Platform#runLater(Runnable)} preserves
	 * submission order, a fence submitted now completes only after the tasks
	 * submitted before it have been dealt with.
	 */
	private static void drainFxQueue() throws InterruptedException {
		final CountDownLatch drained = new CountDownLatch(1);
		Platform.runLater(drained::countDown);

		if (!drained.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
			fail("JavaFX application thread did not drain its queue");
		}
	}
}
