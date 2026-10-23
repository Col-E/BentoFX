package software.coley.bentofx.persistence.core.impl;

import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.BentoStateTimeoutException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Coverage for the paths through {@code PersistenceThreading} that neither
 * dispatch via {@link Platform#runLater(Runnable)} nor need an {@code
 * FxRobot}: the JavaFX toolkit never has to start for these, so they belong
 * here rather than in {@code PersistenceThreadingITG}.
 */
class PersistenceThreadingTest {

	@Test
	void callOffFxThreadRunsImmediatelyWhenAlreadyOffFxThread()
			throws BentoStateException {
		final AtomicBoolean called = new AtomicBoolean();

		final Boolean ranOnFxThread = PersistenceThreading.callOffFxThread(() -> {
			called.set(true);
			return Platform.isFxApplicationThread();
		});

		assertThat(called)
				.describedAs("called")
				.isTrue();
		assertThat(ranOnFxThread)
				.describedAs("ranOnFxThread")
				.isFalse();
	}

	/**
	 * The timeout type must stay assignable to {@link BentoStateException} so
	 * existing callers that catch the general type keep compiling and working,
	 * while callers that need to tell a timeout apart - notably
	 * {@code DockingLayoutRestorer}, which must not substitute a default layout
	 * on timeout - can still catch it specifically.
	 */
	@Test
	void timeoutExceptionRemainsCatchableAsBentoStateException() {
		final BentoStateTimeoutException timeoutException =
				new BentoStateTimeoutException("timed out");

		assertThat(timeoutException)
				.describedAs("BentoStateTimeoutException")
				.isInstanceOf(BentoStateException.class);
	}

	/**
	 * {@link PersistenceThreading#callOffFxThread} run immediately - the caller
	 * is already off the FX thread - wraps a task's own unchecked failure the
	 * same way the on-thread call does.
	 */
	@Test
	void callOffFxThreadWrapsATaskFailureAsBentoStateExceptionWhenAlreadyOffFxThread() {
		final RuntimeException taskFailure = new RuntimeException("boom");

		assertThatThrownBy(() ->
				PersistenceThreading.callOffFxThread(() -> {
					throw taskFailure;
				})
		)
				.describedAs("exception thrown by callOffFxThread when already off the FX thread and the task fails")
				.isInstanceOf(BentoStateException.class)
				.hasMessage("Persistence task failed")
				.hasCause(taskFailure);
	}

	@Test
	void callOffFxThreadRethrowsABentoStateExceptionTaskThrowsWithoutWrappingItAgainWhenAlreadyOffFxThread() {
		final BentoStateException taskFailure = new BentoStateException("task's own failure");

		assertThatThrownBy(() ->
				PersistenceThreading.callOffFxThread(() -> {
					throw taskFailure;
				})
		)
				.describedAs("exception thrown by callOffFxThread when already off the FX thread and the task throws its own BentoStateException")
				.isSameAs(taskFailure);
	}

	/**
	 * A utility class with only static members has no reason to be
	 * instantiated; the private constructor exists to say so rather than to
	 * silently allow it.
	 */
	@Test
	void utilityClassConstructorThrowsIllegalStateException() throws Exception {
		final Constructor<PersistenceThreading> constructor =
				PersistenceThreading.class.getDeclaredConstructor();
		constructor.setAccessible(true);

		assertThatThrownBy(constructor::newInstance)
				.describedAs("reflective instantiation of the utility class")
				.isInstanceOf(InvocationTargetException.class)
				.cause()
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Utility class");
	}
}
