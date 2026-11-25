package software.coley.bentofx.persistence.core.impl;

import javafx.application.Platform;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.BentoStateTimeoutException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * Threading utility methods used by persistence implementations to keep
 * JavaFX object access on the JavaFX application thread while allowing codec
 * and storage work to run off that thread.
 *
 * <p>Off that thread is not the same as asynchronous. Both entry points here are
 * synchronous: they hand work to another thread and wait for it. In particular
 * {@link #callOffFxThread} blocks the JavaFX application thread when that is the
 * caller - see its documentation.</p>
 *
 * <p>Handing work to the JavaFX application thread is not guaranteed to
 * complete. Once the toolkit has shut down, {@link Platform#runLater} accepts a
 * task and silently never runs it, and JavaFX offers no public way to ask
 * whether the toolkit is still alive. Waiting without a bound therefore hangs
 * the calling thread for good. Every wait here is bounded, and a wait that
 * expires cancels the task it was waiting on so a late run cannot apply changes
 * the caller has already given up on.</p>
 *
 * <p>The bounds are deliberately generous. They exist to detect a task that will
 * <em>never</em> run, not to detect one that is merely slow, so they sit orders
 * of magnitude above the real cost of the work.</p>
 *
 * @author Phil Bryant
 */
final class PersistenceThreading {

	/**
	 * Budget for capturing layout state on the JavaFX application thread.
	 *
	 * <p>Capture is a bounded walk of the container tree with no I/O and no
	 * application callbacks - single-digit milliseconds in practice. Ten seconds
	 * absorbs a busy JavaFX thread, a long layout pass and a garbage collection
	 * pause while still bounding a hang.</p>
	 */
	static final long FX_CAPTURE_TIMEOUT_MILLIS = 10_000L;

	/**
	 * Budget for the final save attempted from {@code close()}.
	 *
	 * <p>Shorter than {@link #FX_CAPTURE_TIMEOUT_MILLIS} on purpose. Closing
	 * happens while the application is trying to exit, and the thread running
	 * {@code close()} may not be a daemon, so a long wait here delays or blocks
	 * shutdown. Losing the final save is the better trade.</p>
	 */
	static final long FX_CLOSE_TIMEOUT_MILLIS = 3_000L;

	/**
	 * Name given to the {@link #OFF_FX_EXECUTOR} thread.
	 *
	 * <p>The whole point of naming the thread is, when a save or restore
	 * freezes the UI, a thread dump has to show which thread the JavaFX thread
	 * is waiting on, and a default {@code pool-N-thread-1} says nothing about
	 * whose I/O it is.</p>
	 *
	 * <p>Package-private rather than private so tests can reuse it.</p>
	 */
	static final String OFF_FX_EXECUTOR_THREAD_NAME = "bentofx-persistence-io-thread";

	/**
	 * Runs codec and storage work handed off by the JavaFX application thread.
	 *
	 * <p>One shared executor rather than one per call. Every caller blocks on
	 * its own result, so the work was already serialized and a thread per call
	 * bought nothing but the cost of creating it - plus, on Java 19+, an
	 * {@code ExecutorService.close()} that blocks until the thread terminates.
	 * The thread is a daemon so a storage write that never returns cannot hold
	 * up JVM exit, and it is named so it is identifiable in a thread dump.</p>
	 *
	 * <p>Deliberately never shut down: one idle daemon thread for the lifetime
	 * of the JVM is cheaper than giving this utility a lifecycle that every
	 * saver and restorer sharing it would have to agree on.</p>
	 */
	private static final ExecutorService OFF_FX_EXECUTOR =
			newSingleThreadExecutor(runnable -> {
				final Thread thread =
						new Thread(runnable, OFF_FX_EXECUTOR_THREAD_NAME);
				thread.setDaemon(true);
				return thread;
			});

	private PersistenceThreading() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Runs the supplied task on the JavaFX application thread and waits up to
	 * {@link #FX_CAPTURE_TIMEOUT_MILLIS} for the result. If the current thread
	 * is already the JavaFX application thread, the task is executed
	 * immediately.
	 *
	 * @param task task to execute.
	 * @param <T> task result type.
	 * @return task result.
	 * @throws BentoStateTimeoutException when the task does not run within its
	 * budget.
	 * @throws BentoStateException when the task fails or the waiting thread is
	 * interrupted.
	 */
	static <T> T callOnFxThread(final Callable<T> task)
			throws BentoStateException {
		return callOnFxThread(task, FX_CAPTURE_TIMEOUT_MILLIS);
	}

	/**
	 * Runs the supplied task on the JavaFX application thread and waits up to
	 * the supplied budget for the result. If the current thread is already the
	 * JavaFX application thread, the task is executed immediately and the
	 * budget does not apply - there is no other thread to wait for, and
	 * offloading to create something to time out against would deadlock.
	 *
	 * @param task task to execute.
	 * @param timeoutMillis how long to wait for the JavaFX application thread to
	 * run the task.
	 * @param <T> task result type.
	 * @return task result.
	 * @throws BentoStateTimeoutException when the task does not run within
	 * {@code timeoutMillis}.
	 * @throws BentoStateException when the task fails or the waiting thread is
	 * interrupted.
	 */
	static <T> T callOnFxThread(
			final Callable<T> task,
			final long timeoutMillis
	) throws BentoStateException {
		if (Platform.isFxApplicationThread()) {
			return call(task);
		}

		final FutureTask<T> futureTask = new FutureTask<>(task);
		Platform.runLater(futureTask);

		try {
			return futureTask.get(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (final TimeoutException e) {
			// The task is still queued and would otherwise run later, mutating
			// JavaFX state for a caller that has already stopped waiting for it.
			// FutureTask.run() re-checks state before invoking the callable, so
			// cancelling turns any queued run into a no-op. Interrupting is
			// pointless here: the task either has not started or is running on
			// the JavaFX thread, which must not be interrupted.
			futureTask.cancel(false);
			throw new BentoStateTimeoutException(
					"Timed out after " + timeoutMillis +
							"ms waiting for the JavaFX application thread to " +
							"run a persistence task. The JavaFX toolkit may " +
							"have shut down.",
					e
			);
		} catch (final InterruptedException e) {
			futureTask.cancel(false);
			Thread.currentThread().interrupt();
			throw new BentoStateException(
					"Interrupted while waiting for persistence task",
					e
			);
		} catch (final ExecutionException e) {
			throw unwrap(e);
		}
	}

	/**
	 * Runs the supplied task away from the JavaFX application thread and waits
	 * for the result. If the current thread is already not the JavaFX
	 * application thread, the task is executed immediately.
	 *
	 * <p><b>The calling thread blocks until the task completes, including when
	 * that caller is the JavaFX application thread.</b> Moving the work to
	 * another thread keeps JavaFX state out of reach of the codec and storage
	 * implementations, but it does not free the JavaFX thread: a save or restore
	 * invoked from a window close handler or an exit-time {@code close()}
	 * freezes the UI for as long as the I/O takes. Callers on the JavaFX thread
	 * that cannot afford that need to arrange their own asynchrony - this method
	 * has a synchronous contract.</p>
	 *
	 * <p>No timeout applies here. The task runs on an executor this class owns,
	 * so unlike {@link #callOnFxThread} there is no possibility of the task
	 * never being picked up; the work is codec and storage I/O whose duration
	 * belongs to the storage implementation.</p>
	 *
	 * @param task task to execute.
	 * @param <T> task result type.
	 * @return task result.
	 * @throws BentoStateException when the task fails or the waiting thread is
	 * interrupted.
	 */
	static <T> T callOffFxThread(final Callable<T> task)
			throws BentoStateException {
		if (!Platform.isFxApplicationThread()) {
			return call(task);
		}

		return get(OFF_FX_EXECUTOR.submit(task));
	}

	private static <T> T call(final Callable<T> task)
			throws BentoStateException {
		try {
			return task.call();
		} catch (final BentoStateException e) {
			throw e;
		} catch (final Exception e) {
			throw new BentoStateException(
					"Persistence task failed",
					e
			);
		}
	}

	private static <T> T get(final Future<T> future)
			throws BentoStateException {
		try {
			return future.get();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BentoStateException(
					"Interrupted while waiting for persistence task",
					e
			);
		} catch (final ExecutionException e) {
			throw unwrap(e);
		}
	}

	/**
	 * Unwraps the cause of an {@link ExecutionException} so a
	 * {@link BentoStateException} thrown by the task keeps its own type and
	 * message rather than being buried a cause deeper.
	 *
	 * @param e the {@link ExecutionException} to unwrap.
	 * @return the exception to throw.
	 */
	private static BentoStateException unwrap(final ExecutionException e) {
		final Throwable cause = e.getCause();
		if (cause instanceof final BentoStateException bentoStateException) {
			return bentoStateException;
		}
		return new BentoStateException("Persistence task failed", e);
	}
}
