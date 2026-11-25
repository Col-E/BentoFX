package software.coley.bentofx.persistence.core.impl;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;
import software.coley.bentofx.persistence.testfixtures.codec.InMemoryLayoutCodec;
import software.coley.bentofx.persistence.testfixtures.storage.InMemoryLayoutStorage;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractAutoCloseableLayoutSaverTest {

	@Test
	void enableAutoSaveRejectsANonPositiveInterval() {
		try (DockingLayoutSaver saver = new DockingLayoutSaver(
				new InMemoryLayoutCodec(),
				new InMemoryLayoutStorage(),
				new DefaultBentoProvider()
		)) {
			assertThatThrownBy(() -> saver.enableAutoSave(0, TimeUnit.SECONDS))
					.describedAs("enableAutoSave(0, ...)")
					.isInstanceOf(IllegalArgumentException.class);

			assertThatThrownBy(() -> saver.enableAutoSave(-1, TimeUnit.SECONDS))
					.describedAs("enableAutoSave(-1, ...)")
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	/**
	 * A save that fails leaves the layout changed, so the next scheduled save
	 * tries again instead of treating the change as saved.
	 */
	@Test
	void aFailedScheduledSaveIsRetried() throws InterruptedException {
		final CountDownLatch secondAttempt = new CountDownLatch(2);
		final AtomicInteger attempts = new AtomicInteger();

		try (AbstractAutoCloseableLayoutSaver saver =
					 new AbstractAutoCloseableLayoutSaver(new DefaultBentoProvider()) {
						 @Override
						 protected void saveLayoutInternal() throws BentoStateException {
							 secondAttempt.countDown();
							 if (attempts.incrementAndGet() == 1) {
								 throw new BentoStateException("first save fails");
							 }
						 }
					 }) {
			markDirty(saver);
			saver.enableAutoSave(10, TimeUnit.MILLISECONDS);

			assertThat(secondAttempt.await(5, TimeUnit.SECONDS))
					.describedAs("a second save attempt after the first failed")
					.isTrue();
		}
	}

	/**
	 * Closing while a scheduled save is still waiting stops that save, and the
	 * change it was carrying is saved on exit rather than lost.
	 */
	@Test
	void closingDuringAScheduledSaveStillSavesOnExit() throws InterruptedException {
		final CountDownLatch scheduledSaveStarted = new CountDownLatch(1);
		final CountDownLatch neverReleased = new CountDownLatch(1);
		final AtomicBoolean savedOnExit = new AtomicBoolean();

		final AbstractAutoCloseableLayoutSaver saver =
				new AbstractAutoCloseableLayoutSaver(new DefaultBentoProvider()) {
					@Override
					protected void saveLayoutInternal() throws BentoStateException {
						scheduledSaveStarted.countDown();
						try {
							// Stands in for waiting on a busy JavaFX thread.
							neverReleased.await(1, TimeUnit.MINUTES);
						} catch (final InterruptedException e) {
							Thread.currentThread().interrupt();
							throw new BentoStateException("interrupted", e);
						}
					}

					@Override
					protected void saveLayoutForShutdown() {
						savedOnExit.set(true);
					}
				};

		markDirty(saver);
		saver.enableAutoSave(10, TimeUnit.MILLISECONDS);
		assertThat(scheduledSaveStarted.await(5, TimeUnit.SECONDS))
				.describedAs("scheduled save started")
				.isTrue();

		saver.close();

		assertThat(savedOnExit)
				.describedAs("saved on exit after interrupting the scheduled save")
				.isTrue();
	}

	/**
	 * Pins the published default interval. A change here is observable to
	 * subclasses that read {@link AbstractAutoCloseableLayoutSaver#DEFAULT_AUTO_SAVE_INTERVAL}
	 * to size their own choice against the framework default, so changing it
	 * silently would surprise them.
	 */
	@Test
	void defaultAutoSaveIntervalIsFiveMinutes() {
		assertThat(AbstractAutoCloseableLayoutSaver.DEFAULT_AUTO_SAVE_INTERVAL)
				.describedAs("published default auto-save interval")
				.isEqualTo(Duration.ofMinutes(5));
	}

	/**
	 * Marks the layout changed. {@code markLayoutDirty} only logs the event it is
	 * given, and these tests have no dock event to give it.
	 */
	@SuppressWarnings("NullAway")
	private static void markDirty(final AbstractAutoCloseableLayoutSaver saver) {
		saver.markLayoutDirty(null);
	}
}
