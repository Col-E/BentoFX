package software.coley.bentofx.persistence.core.impl;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import software.coley.bentofx.Bento;
import software.coley.bentofx.event.DockEvent;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the scheduled auto-save task itself and {@code close()}'s delegation
 * to it. Requires the JavaFX toolkit only to build a real {@link DockContainerLeaf}
 * for a {@link DockEvent} - {@link AbstractAutoCloseableLayoutSaver} does not
 * touch JavaFX itself, but every {@link DockEvent} variant carries a real
 * {@link software.coley.bentofx.layout.DockContainer} or
 * {@link software.coley.bentofx.dockable.Dockable}, and those are JavaFX nodes
 * that require the toolkit to construct.
 */
@ExtendWith(ApplicationExtension.class)
class AbstractAutoCloseableLayoutSaverITG {

	private static final String LEAF_IDENTIFIER = "auto-save-test-leaf";
	private static final long LATCH_TIMEOUT_SECONDS = 10L;

	@Test
	void autoSaveCallsSaveLayoutOnceTheScheduledIntervalElapsesAfterADockEventWasReceived(
			final FxRobot robot
	) throws InterruptedException {
		final Bento bento = new Bento();
		final BentoProvider bentoProvider = new DefaultBentoProvider(bento);
		final DockContainerLeaf leaf = buildLeaf(robot, bento);

		try (RecordingSaver saver = new RecordingSaver(bentoProvider)) {
			robot.interact(() ->
					saver.markLayoutDirty(new DockEvent.RootContainerAdded(leaf))
			);
			saver.enableAutoSave(1L, TimeUnit.MILLISECONDS);

			assertThat(saver.saveLatch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS))
					.describedAs("saveLayout() invoked by the scheduled auto-save task")
					.isTrue();
		}
	}

	@Test
	void autoSaveSwallowsASaveLayoutFailureInsteadOfPropagatingIt(
			final FxRobot robot
	) throws InterruptedException {
		final Bento bento = new Bento();
		final BentoProvider bentoProvider = new DefaultBentoProvider(bento);
		final DockContainerLeaf leaf = buildLeaf(robot, bento);

		try (RecordingSaver saver =
					 new RecordingSaver(bentoProvider, new BentoStateException("boom"))) {
			robot.interact(() ->
					saver.markLayoutDirty(new DockEvent.RootContainerAdded(leaf))
			);
			saver.enableAutoSave(1L, TimeUnit.MILLISECONDS);

			// The failure is caught and logged rather than propagated or left to
			// kill the scheduler thread - reaching the latch at all is the proof.
			assertThat(saver.saveLatch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS))
					.describedAs("saveLayout() invoked by the scheduled auto-save task despite failing")
					.isTrue();
		}
	}

	@Test
	void closeDelegatesToSaveLayoutThroughTheDefaultSaveLayoutForShutdown(
			final FxRobot robot
	) {
		final Bento bento = new Bento();
		final DockContainerLeaf leaf = buildLeaf(robot, bento);
		final RecordingSaver saver =
				new RecordingSaver(new DefaultBentoProvider(bento));

		robot.interact(() ->
				saver.markLayoutDirty(new DockEvent.RootContainerAdded(leaf))
		);
		saver.close();

		assertThat(saver.saveCount.get())
				.describedAs("saveCount after close() with a pending dock event")
				.isEqualTo(1);
	}

	private static DockContainerLeaf buildLeaf(final FxRobot robot, final Bento bento) {
		final AtomicReference<DockContainerLeaf> leafReference =
				new AtomicReference<>();
		robot.interact(() ->
				leafReference.set(bento.dockBuilding().leaf(LEAF_IDENTIFIER))
		);
		return leafReference.get();
	}

	private static final class RecordingSaver extends AbstractAutoCloseableLayoutSaver {

		private final CountDownLatch saveLatch = new CountDownLatch(1);
		private final AtomicInteger saveCount = new AtomicInteger();
		private final @Nullable BentoStateException failure;

		RecordingSaver(final BentoProvider bentoProvider) {
			this(bentoProvider, null);
		}

		RecordingSaver(
				final BentoProvider bentoProvider,
				final @Nullable BentoStateException failure
		) {
			super(bentoProvider);
			this.failure = failure;
		}

		@Override
		protected void saveLayoutInternal() throws BentoStateException {
			saveCount.incrementAndGet();
			saveLatch.countDown();
			if (failure != null) {
				throw failure;
			}
		}
	}
}
