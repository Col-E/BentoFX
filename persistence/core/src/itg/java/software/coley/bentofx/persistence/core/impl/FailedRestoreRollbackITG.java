package software.coley.bentofx.persistence.core.impl;

import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import software.coley.bentofx.Bento;
import software.coley.bentofx.persistence.core.api.DockingLayout.DockingLayoutBuilder;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.BentoState.BentoStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState.DockContainerLeafStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState.DockContainerRootBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockableState;
import software.coley.bentofx.persistence.core.api.state.DockableState.DockableStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DragDropStageState.DragDropStageStateBuilder;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;
import software.coley.bentofx.persistence.testfixtures.codec.InMemoryLayoutCodec;
import software.coley.bentofx.persistence.testfixtures.storage.InMemoryLayoutStorage;

import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Coverage for what a restore that fails part way through leaves behind.
 *
 * <p>A drag/drop stage's root branch registers with its {@link Bento} as soon
 * as the stage has a scene, which happens before the rest of the layout is
 * built. A restore that then fails used to leave those roots registered but
 * never shown, and the next save captured them as extra root branches.</p>
 *
 * @author Phil Bryant
 */
@ExtendWith(ApplicationExtension.class)
class FailedRestoreRollbackITG {

	private static final String BENTO_ID = "bento-failed-restore";
	private static final String GOOD_LEAF_ID = "leaf-restores";
	private static final String BAD_LEAF_ID = "leaf-callback-throws";

	@Test
	void aRestoreThatFailsLeavesNoRootsRegistered(final FxRobot robot) {
		final Bento bento = new Bento(BENTO_ID);
		final AtomicReference<RuntimeException> failure = new AtomicReference<>();
		final AtomicInteger rootsAfterFailure = new AtomicInteger(-1);

		robot.interact(() -> {
			final DefaultDockingLayoutRebuilder restorer = new DefaultDockingLayoutRebuilder(
					new DefaultBentoProvider(bento),
					id -> Optional.of(createDockableState(id)),
					null,
					leafIdentifier -> {
						if (BAD_LEAF_ID.equals(leafIdentifier)) {
							throw new IllegalStateException("application callback failed");
						}
						return Optional.empty();
					}
			);

			try {
				restorer.rebuild(List.of(createBentoState()));
			} catch (final RuntimeException e) {
				failure.set(e);
			}

			rootsAfterFailure.set(bento.getRootContainers().size());
		});

		assertThat(failure.get())
				.describedAs("failure from the application callback")
				.isInstanceOf(IllegalStateException.class);
		assertThat(rootsAfterFailure.get())
				.describedAs("root branches still registered after the failed restore")
				.isZero();
	}

	/**
	 * A restore already running on the JavaFX thread when its caller times out
	 * cannot be stopped. When it finishes, the layout it built is discarded, so
	 * the roots it registered do not outlive it.
	 */
	@Test
	void aRestoreThatFinishesAfterItsCallerTimedOutLeavesNoRootsRegistered(
			final FxRobot robot
	) throws Exception {
		final Bento bento = new Bento(BENTO_ID);
		final CountDownLatch callbackEntered = new CountDownLatch(1);
		final CountDownLatch releaseCallback = new CountDownLatch(1);
		final AtomicInteger rootsWhileStalled = new AtomicInteger(-1);
		final AtomicInteger rootsAfterLateRestore = new AtomicInteger(-1);

		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final InMemoryLayoutStorage storage = new InMemoryLayoutStorage();
		try (OutputStream out = storage.openOutputStream()) {
			codec.writeEncoded(List.of(createBentoState()), out);
		}

		final DockingLayoutRestorer restorer = new DockingLayoutRestorer(
				codec,
				storage,
				new DefaultBentoProvider(bento),
				id -> Optional.of(createDockableState(id)),
				null,
				leafIdentifier -> {
					// Stalls the restore part way through, after the first
					// stage's root has registered, until its caller gives up.
					if (BAD_LEAF_ID.equals(leafIdentifier)) {
						rootsWhileStalled.set(bento.getRootContainers().size());
						callbackEntered.countDown();
						awaitRelease(releaseCallback);
					}
					return Optional.empty();
				}
		);

		try {
			assertThatThrownBy(() ->
					restorer.restoreLayout(() -> new DockingLayoutBuilder().build())
			)
					.describedAs("restoreLayout whose restore outlives the wait for it")
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("Timed out restoring the docking layout");
		} finally {
			releaseCallback.countDown();
		}

		// Queued behind the late restore and its cleanup, so runs after both.
		robot.interact(() -> rootsAfterLateRestore.set(bento.getRootContainers().size()));

		assertThat(callbackEntered.getCount())
				.describedAs("restore reached the stalling callback")
				.isZero();
		assertThat(rootsWhileStalled.get())
				.describedAs("root branches registered while the restore was stalled")
				.isPositive();
		assertThat(rootsAfterLateRestore.get())
				.describedAs("root branches still registered after the late restore finished")
				.isZero();
	}

	/**
	 * Blocks the JavaFX thread until released, bounded so a broken test fails
	 * rather than hangs.
	 */
	private static void awaitRelease(final CountDownLatch release) {
		try {
			if (!release.await(1, TimeUnit.MINUTES)) {
				throw new IllegalStateException("stalled callback was never released");
			}
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("interrupted while stalled", e);
		}
	}

	/**
	 * {@return a Bento with two drag/drop stages, the second holding the leaf
	 * whose menu factory callback throws or stalls.}
	 */
	private static BentoState createBentoState() {
		return new BentoStateBuilder(BENTO_ID)
				.addDragDropStageState(new DragDropStageStateBuilder(false)
						.setDockContainerRootBranchState(createRoot("root-first", GOOD_LEAF_ID))
						.build())
				.addDragDropStageState(new DragDropStageStateBuilder(false)
						.setDockContainerRootBranchState(createRoot("root-second", BAD_LEAF_ID))
						.build())
				.build();
	}

	private static DockContainerRootBranchState createRoot(
			final String rootIdentifier,
			final String leafIdentifier
	) {
		final DockContainerLeafStateBuilder leaf =
				new DockContainerLeafStateBuilder(leafIdentifier);
		leaf.addChildDockableState(createDockableState(leafIdentifier + "-dockable"));

		final DockContainerRootBranchStateBuilder root =
				new DockContainerRootBranchStateBuilder(rootIdentifier);
		root.addDockContainerState(leaf.build());
		return root.build();
	}

	private static DockableState createDockableState(final String identifier) {
		return new DockableStateBuilder(identifier)
				.setTitle(identifier)
				.setDockableNode(new Label(identifier))
				.build();
	}
}
