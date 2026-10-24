package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;
import software.coley.bentofx.Bento;
import software.coley.bentofx.building.DockBuilding;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.DockingLayout;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.LayoutRestorer;
import software.coley.bentofx.persistence.core.api.LayoutSaver;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.provider.DockContainerLeafMenuFactoryProvider;
import software.coley.bentofx.persistence.core.api.provider.DockableStateProvider;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutRestorable;
import software.coley.bentofx.persistence.core.api.provider.LayoutCodecProvider;
import software.coley.bentofx.persistence.core.api.provider.StageIconImageProvider;
import software.coley.bentofx.persistence.core.api.storage.LayoutIdentifierProblem;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;
import software.coley.bentofx.persistence.core.impl.provider.DefaultDockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.testfixtures.codec.InMemoryLayoutCodec;
import software.coley.bentofx.persistence.testfixtures.provider.InMemoryLayoutStorageProvider;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static javafx.scene.control.ButtonType.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers.SESSION_LAYOUT_IDENTIFIER;

/**
 * Coverage for {@link LayoutOperations}: the layout CRUD operations.
 *
 * <p>Runs as an integration-test graphical because the constructor builds
 * {@link DefaultArrangementState}, which schedules a recording through
 * {@link javafx.application.Platform#runLater} when no session layout is
 * stored, so the FX toolkit must be running.</p>
 *
 * @author Phil Bryant
 */
@ExtendWith(ApplicationExtension.class)
class LayoutOperationsITG {

	private static final String BUNDLE_BASE_NAME =
			"software.coley.bentofx.persistence.core.ui.layoutsmenu.LayoutsMenu";

	/** Captured in {@link #start} so helpers can pass it as the dialog owner. */
	private @Nullable Stage stage;

	/**
	 * Captured in {@link #start} so {@link #createRestorable} can hand it to
	 * saveLayout. The bento holds a root branch attached to the stage's scene,
	 * so capture finds something to persist rather than silently skipping with
	 * a nothing-to-save guard.
	 */
	private @Nullable BentoProvider bentoProvider;

	@Start
	@SuppressWarnings("unused") // Called by ApplicationExtension
	private void start(final Stage s) {
		stage = s;

		final Bento bento = new Bento();
		final DockBuilding dockBuilding = bento.dockBuilding();
		final DockContainerRootBranch root = dockBuilding.root("itg-root");
		final DockContainerLeaf leaf = dockBuilding.leaf("itg-leaf");
		final Dockable dockable = dockBuilding.dockable("itg-dockable");
		leaf.addDockable(dockable);
		root.addContainer(leaf);

		s.setScene(new Scene(root));
		s.show();

		final DefaultBentoProvider provider = new DefaultBentoProvider();
		provider.addBento(bento);
		bentoProvider = provider;
	}

	/**
	 * Happy path: {@link LayoutOperations#writeLayout} saves the layout to
	 * storage and marks it as active through {@link ActiveLayoutState}.
	 */
	@Test
	void writeLayoutPersistsAndActivatesProfile() throws BentoStateException {
		final DockingLayoutPersistenceProvider provider = createPersistenceProvider();
		final DockingLayoutRestorable restorable = createRestorable(provider);
		final ActiveLayoutState activeLayoutState = new ActiveLayoutState(restorable);
		final DefaultArrangementState defaultArrangementState =
				createDefaultArrangementState(restorable);
		final FakeContext context = new FakeContext();
		final LayoutOperations ops =
				createLayoutOperations(restorable, activeLayoutState, defaultArrangementState, context);
		final LayoutPersistenceProfile profile =
				LayoutPersistenceProfile.of("test-layout").withNaming("Test Layout", null);

		ops.writeLayout(profile);

		assertThat(provider.isLayoutStored(profile))
				.describedAs("layout should be stored after writeLayout")
				.isTrue();
		assertThat(activeLayoutState.getActiveLayoutProfile())
				.describedAs("active profile after writeLayout")
				.isEqualTo(profile);
		assertThat(context.showLayoutErrorCalled)
				.describedAs("showLayoutError should not have been called")
				.isFalse();
	}

	/**
	 * Error path: when {@link DockingLayoutPersistenceProvider#saveLayout} throws
	 * {@link BentoStateException}, {@link LayoutOperations#writeLayout} reports
	 * the failure through the context and leaves the active profile unchanged.
	 */
	@Test
	void writeLayoutCallsShowLayoutErrorWhenSaveFails() {
		final DockingLayoutRestorable restorable =
				createRestorable(throwingOnSaveProvider());
		final ActiveLayoutState activeLayoutState = new ActiveLayoutState(restorable);
		final DefaultArrangementState defaultArrangementState =
				createDefaultArrangementState(restorable);
		final FakeContext context = new FakeContext();
		final LayoutOperations ops =
				createLayoutOperations(restorable, activeLayoutState, defaultArrangementState, context);
		final LayoutPersistenceProfile profile =
				LayoutPersistenceProfile.of("failing-layout");

		ops.writeLayout(profile);

		assertThat(context.showLayoutErrorCalled)
				.describedAs("showLayoutError should have been called on save failure")
				.isTrue();
		assertThat(activeLayoutState.getActiveLayoutProfile())
				.describedAs("active profile should stay null after failed save")
				.isNull();
	}

	/**
	 * Happy path: {@link LayoutOperations#deleteStoredLayout} removes the layout
	 * from storage when the user confirms.
	 */
	@Test
	void deleteStoredLayoutRemovesLayoutWhenConfirmed() throws BentoStateException {
		final DockingLayoutPersistenceProvider provider = createPersistenceProvider();
		final DockingLayoutRestorable restorable = createRestorable(provider);
		final ActiveLayoutState activeLayoutState = new ActiveLayoutState(restorable);
		final DefaultArrangementState defaultArrangementState =
				createDefaultArrangementState(restorable);
		final FakeContext context = new FakeContext(); // confirmResult defaults to true
		final LayoutOperations ops =
				createLayoutOperations(restorable, activeLayoutState, defaultArrangementState, context);
		final LayoutPersistenceProfile profile =
				LayoutPersistenceProfile.of("to-delete").withNaming("To Delete", null);
		provider.saveLayout(profile, restorable.getBentoProvider());

		ops.deleteStoredLayout(profile);

		assertThat(provider.isLayoutStored(profile))
				.describedAs("layout should be gone after deleteStoredLayout")
				.isFalse();
	}

	/**
	 * Edge case: {@link LayoutOperations#deleteStoredLayout} does nothing when
	 * the user denies the confirmation.
	 */
	@Test
	void deleteStoredLayoutDoesNothingWhenDenied() throws BentoStateException {
		final DockingLayoutPersistenceProvider provider = createPersistenceProvider();
		final DockingLayoutRestorable restorable = createRestorable(provider);
		final ActiveLayoutState activeLayoutState = new ActiveLayoutState(restorable);
		final DefaultArrangementState defaultArrangementState =
				createDefaultArrangementState(restorable);
		final FakeContext context = new FakeContext();
		context.confirmResult = false;
		final LayoutOperations ops =
				createLayoutOperations(restorable, activeLayoutState, defaultArrangementState, context);
		final LayoutPersistenceProfile profile =
				LayoutPersistenceProfile.of("kept").withNaming("Kept", null);
		provider.saveLayout(profile, restorable.getBentoProvider());

		ops.deleteStoredLayout(profile);

		assertThat(provider.isLayoutStored(profile))
				.describedAs("layout should still be stored when delete was denied")
				.isTrue();
	}

	/**
	 * Edge case: when the deleted layout was active,
	 * {@link LayoutOperations#deleteStoredLayout} clears the active profile.
	 */
	@Test
	void deleteStoredLayoutClearsActiveProfileWhenDeletingActiveLayout()
			throws BentoStateException {
		final DockingLayoutPersistenceProvider provider = createPersistenceProvider();
		final DockingLayoutRestorable restorable = createRestorable(provider);
		final ActiveLayoutState activeLayoutState = new ActiveLayoutState(restorable);
		final DefaultArrangementState defaultArrangementState =
				createDefaultArrangementState(restorable);
		final FakeContext context = new FakeContext();
		final LayoutOperations ops =
				createLayoutOperations(restorable, activeLayoutState, defaultArrangementState, context);
		final LayoutPersistenceProfile profile =
				LayoutPersistenceProfile.of("active-one").withNaming("Active One", null);
		provider.saveLayout(profile, restorable.getBentoProvider());
		activeLayoutState.setActiveCustomLayoutProfile(profile);

		ops.deleteStoredLayout(profile);

		assertThat(activeLayoutState.getActiveLayoutProfile())
				.describedAs("active profile should be cleared after deleting the active layout")
				.isNull();
	}

	/**
	 * Happy path: {@link LayoutOperations#saveCurrentLayoutAsNew} opens a
	 * {@link javafx.scene.control.TextInputDialog}, saves the layout under the
	 * typed name, and marks it active through {@link ActiveLayoutState}.
	 *
	 * <p>No groups are pre-loaded, so no
	 * {@link javafx.scene.control.ChoiceDialog} appears.</p>
	 */
	@Test
	void saveCurrentLayoutAsNewPersistsAndActivatesProfile() {
		final DockingLayoutPersistenceProvider provider = createPersistenceProvider();
		final DockingLayoutRestorable restorable = createRestorable(provider);
		final ActiveLayoutState activeLayoutState = new ActiveLayoutState(restorable);
		final DefaultArrangementState defaultArrangementState =
				createDefaultArrangementState(restorable);
		final FakeContext context = new FakeContext();
		final LayoutOperations ops =
				createLayoutOperations(restorable, activeLayoutState, defaultArrangementState, context);

		fireDialogOperation(
				ops::saveCurrentLayoutAsNew,
				typeAndDismiss("My New Layout")
		);

		assertThat(activeLayoutState.getActiveLayoutProfile())
				.describedAs("active profile after saveCurrentLayoutAsNew")
				.isNotNull()
				.extracting(LayoutPersistenceProfile::displayName)
				.isEqualTo("My New Layout");
		assertThat(context.showLayoutErrorCalled)
				.describedAs("showLayoutError should not have been called")
				.isFalse();
	}

	/**
	 * Happy path: {@link LayoutOperations#renameStoredLayout} opens a
	 * {@link javafx.scene.control.TextInputDialog} pre-filled with the current
	 * name, stores the new name, and keeps {@link ActiveLayoutState} in step.
	 */
	@Test
	void renameStoredLayoutUpdatesDisplayNameAndKeepsActiveLayoutInStep()
			throws BentoStateException {
		final DockingLayoutPersistenceProvider provider = createPersistenceProvider();
		final DockingLayoutRestorable restorable = createRestorable(provider);
		final ActiveLayoutState activeLayoutState = new ActiveLayoutState(restorable);
		final DefaultArrangementState defaultArrangementState =
				createDefaultArrangementState(restorable);
		final FakeContext context = new FakeContext();
		final LayoutOperations ops =
				createLayoutOperations(restorable, activeLayoutState, defaultArrangementState, context);
		final LayoutPersistenceProfile profile =
				LayoutPersistenceProfile.of("my-layout").withNaming("Old Name", null);
		provider.saveLayout(profile, restorable.getBentoProvider());
		activeLayoutState.setActiveCustomLayoutProfile(profile);

		fireDialogOperation(
				() -> ops.renameStoredLayout(profile),
				typeAndDismiss("New Name")
		);

		assertThat(activeLayoutState.getActiveLayoutProfile())
				.describedAs("active profile after renameStoredLayout")
				.isNotNull()
				.extracting(LayoutPersistenceProfile::displayName)
				.isEqualTo("New Name");
		assertThat(provider.getStoredLayouts(LayoutPersistenceProfile.of(SESSION_LAYOUT_IDENTIFIER)))
				.describedAs("stored layouts after rename")
				.extracting(LayoutPersistenceProfile::displayName)
				.contains("New Name");
		assertThat(context.showLayoutErrorCalled)
				.describedAs("showLayoutError should not have been called")
				.isFalse();
	}

	/**
	 * Happy path: {@link LayoutOperations#moveStoredLayoutToGroup} opens a
	 * {@link javafx.scene.control.ChoiceDialog} with the available groups, and
	 * stores the chosen group on the profile.
	 */
	@Test
	void moveStoredLayoutToGroupUpdatesGroupOnStoredProfile()
			throws BentoStateException {
		final DefaultDockingLayoutPersistenceProvider provider = createExtendedPersistenceProvider();
		provider.setStoredGroups(
				LayoutPersistenceProfile.of(SESSION_LAYOUT_IDENTIFIER),
				List.of("Workspace")
		);
		final DockingLayoutRestorable restorable = createRestorable(provider);
		final ActiveLayoutState activeLayoutState = new ActiveLayoutState(restorable);
		final DefaultArrangementState defaultArrangementState =
				createDefaultArrangementState(restorable);
		final FakeContext context = new FakeContext();
		final LayoutOperations ops =
				createLayoutOperations(restorable, activeLayoutState, defaultArrangementState, context);
		final LayoutPersistenceProfile profile =
				LayoutPersistenceProfile.of("my-layout").withNaming("My Layout", null);
		provider.saveLayout(profile, restorable.getBentoProvider());

		fireDialogOperation(
				() -> ops.moveStoredLayoutToGroup(profile),
				LayoutOperationsITG::selectChoiceAndDismiss
		);

		assertThat(provider.getStoredLayouts(LayoutPersistenceProfile.of(SESSION_LAYOUT_IDENTIFIER)))
				.describedAs("stored layouts after moveStoredLayoutToGroup")
				.filteredOn(p -> "my-layout".equals(p.layoutIdentifier()))
				.extracting(LayoutPersistenceProfile::group)
				.containsExactly("Workspace");
		assertThat(context.showLayoutErrorCalled)
				.describedAs("showLayoutError should not have been called")
				.isFalse();
	}

	/**
	 * {@return a {@link LayoutOperations} instance wired to the supplied
	 * components, using the default resource bundle and the stage from
	 * {@link #start} as the dialog owner.}
	 */
	private LayoutOperations createLayoutOperations(
			final DockingLayoutRestorable restorable,
			final ActiveLayoutState activeLayoutState,
			final DefaultArrangementState defaultArrangementState,
			final LayoutOperationsContext context
	) {
		Objects.requireNonNull(stage);
		final ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME);
		final LayoutGroupOperations groupOperations = new LayoutGroupOperations(
				restorable,
				stage,
				bundle,
				stubGroupContext()
		);
		return new LayoutOperations(
				restorable,
				stage,
				bundle,
				groupOperations,
				activeLayoutState,
				defaultArrangementState,
				context
		);
	}

	/**
	 * {@return a {@link DefaultArrangementState} whose bootstrap task has
	 * already run.}
	 *
	 * <p>{@link DefaultArrangementState}'s constructor schedules
	 * {@link javafx.application.Platform#runLater} to record the arrangement
	 * showing now as the default layout's. That queued task calls
	 * {@code provider.saveLayout} on the FX thread, which iterates the
	 * {@code BentoProvider}'s state. A test that constructs
	 * {@code DefaultArrangementState} and immediately calls
	 * {@code provider.saveLayout} on the test thread races the bootstrap: both
	 * sides iterate the same state concurrently and the second one throws
	 * {@link java.util.ConcurrentModificationException}. Draining the FX event
	 * queue here lets the bootstrap complete before the caller continues.</p>
	 *
	 * @param restorable the application whose persistence providers the
	 * default arrangement is read and written through.
	 */
	private static DefaultArrangementState createDefaultArrangementState(
			final DockingLayoutRestorable restorable
	) {
		final DefaultArrangementState state = new DefaultArrangementState(restorable);
		WaitForAsyncUtils.waitForFxEvents();
		return state;
	}

	/**
	 * {@return a provider backed by in-memory storage and a single in-memory
	 * codec.}
	 */
	private static DockingLayoutPersistenceProvider createPersistenceProvider() {
		return createExtendedPersistenceProvider();
	}

	/**
	 * {@return a provider backed by in-memory storage and a single in-memory
	 * codec, narrowed to the extended interface for tests that need to seed
	 * the group catalog or read it back directly.}
	 */
	private static DefaultDockingLayoutPersistenceProvider createExtendedPersistenceProvider() {
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		return new DefaultDockingLayoutPersistenceProvider(
				List.of(new SingleCodecProvider(codec)),
				List.of(new InMemoryLayoutStorageProvider())
		);
	}

	/**
	 * {@return a {@link DockingLayoutRestorable} backed by the supplied
	 * provider, throwing {@link UnsupportedOperationException} for every method
	 * {@link LayoutOperations} does not use through this fixture.}
	 *
	 * @param provider what {@link DockingLayoutRestorable#getPersistenceProvider()}
	 * hands back.
	 */
	private DockingLayoutRestorable createRestorable(
			final DockingLayoutPersistenceProvider provider
	) {
		final BentoProvider bentos = Objects.requireNonNull(
				bentoProvider, "bentoProvider (did @Start run?)"
		);
		return new DockingLayoutRestorable() {
			@Override
			public DockingLayout getDefaultDockingLayout() {
				throw new UnsupportedOperationException();
			}

			@Override
			public DockingLayout getDockingLayout(
					final LayoutPersistenceProfile layoutPersistenceProfile,
					final Supplier<DockingLayout> fallbackLayoutSupplier
			) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean switchToLayout(
					final Supplier<DockingLayout> dockingLayoutSupplier
			) {
				throw new UnsupportedOperationException();
			}

			@Override
			public DockingLayoutPersistenceProvider getPersistenceProvider() {
				return provider;
			}

			@Override
			public BentoProvider getBentoProvider() {
				return bentos;
			}
		};
	}

	/**
	 * {@return a {@link DockingLayoutPersistenceProvider} whose
	 * {@link DockingLayoutPersistenceProvider#saveLayout} always throws
	 * {@link BentoStateException}, to exercise the error path in
	 * {@link LayoutOperations#writeLayout}.}
	 */
	private static DockingLayoutPersistenceProvider throwingOnSaveProvider() {
		return new DockingLayoutPersistenceProvider() {
			@Override
			public LayoutSaver getLayoutSaver(
					final LayoutPersistenceProfile layoutPersistenceProfile,
					final BentoProvider bentoProvider
			) {
				throw new UnsupportedOperationException();
			}

			@Override
			public LayoutRestorer getLayoutRestorer(
					final LayoutPersistenceProfile layoutPersistenceProfile,
					final BentoProvider bentoProvider,
					final DockableStateProvider dockableStateProvider,
					final @Nullable StageIconImageProvider stageIconImageProvider,
					final @Nullable DockContainerLeafMenuFactoryProvider dockContainerLeafMenuFactoryProvider
			) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void saveLayout(
					final LayoutPersistenceProfile layoutPersistenceProfile,
					final BentoProvider bentoProvider
			) throws BentoStateException {
				throw new BentoStateException("simulated write failure");
			}

			@Override
			public List<String> getStoredLayoutIdentifiers(
					final LayoutPersistenceProfile layoutPersistenceProfile
			) {
				return List.of();
			}

			@Override
			public List<LayoutPersistenceProfile> getStoredLayouts(
					final LayoutPersistenceProfile layoutPersistenceProfile
			) {
				return List.of();
			}

			@Override
			public boolean isLayoutStored(
					final LayoutPersistenceProfile layoutPersistenceProfile
			) {
				return false;
			}

			@Override
			public boolean deleteLayout(
					final LayoutPersistenceProfile layoutPersistenceProfile
			) {
				return false;
			}

			@Override
			public boolean updateStoredLayoutNaming(
					final LayoutPersistenceProfile layoutPersistenceProfile
			) {
				return false;
			}
		};
	}

	/**
	 * {@return a {@link LayoutGroupOperationsContext} that throws
	 * {@link UnsupportedOperationException} on every method, for test setups
	 * that do not exercise any group operation.}
	 */
	private static LayoutGroupOperationsContext stubGroupContext() {
		return new LayoutGroupOperationsContext() {
			@Override
			public boolean confirmLayoutAction(
					final String header,
					final String content
			) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void showLayoutError(
					final String header,
					final @Nullable String content
			) {
				throw new UnsupportedOperationException();
			}

			@Override
			public String problemText(final LayoutIdentifierProblem problem) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void populate() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void followNamingChange(
					final LayoutPersistenceProfile renamedLayout
			) {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * A {@link LayoutCodecProvider} that hands out one codec instance, so that
	 * what one call encoded a later call can decode.
	 */
	private record SingleCodecProvider(InMemoryLayoutCodec codec)
			implements LayoutCodecProvider {

		@Override
		public String getIdentifier() {
			return codec.getIdentifier();
		}

		@Override
		public boolean isDefault() {
			return true;
		}

		@Override
		public LayoutCodec getLayoutCodec() {
			return codec;
		}
	}

	/** How long to wait for a dialog to appear or close before failing. */
	private static final long DIALOG_TIMEOUT_SECONDS = 5;

	/**
	 * Fires an operation from the FX thread, dismissing any dialogs it raises
	 * with the supplied steps, one dialog at a time and in order. Blocks the
	 * test thread until the operation returns or
	 * {@value #DIALOG_TIMEOUT_SECONDS} seconds elapse.
	 *
	 * <p>Each step is queued via {@link Platform#runLater} before the
	 * operation runs. The dialog's nested event loop pumps the task queue, so
	 * each step fires in order and closes its dialog before
	 * {@code showAndWait} returns.</p>
	 *
	 * <p>The test thread waits on a {@link CountDownLatch} that the FX thread
	 * counts down when the operation returns, rather than polling
	 * {@link #findShowingDialogPane()}. A dialog that opens and closes within
	 * a single FX pulse — which happens whenever the dismiss step is already
	 * queued when the nested event loop starts pumping — would never be
	 * observed in the "showing" state by a polling waiter, and the test would
	 * time out looking for a dialog that already closed. Waiting on the
	 * operation itself has no such window.</p>
	 *
	 * <p>If the operation does not complete in time, any still-showing dialog
	 * is force-cancelled so {@code showAndWait} can return and surface the real
	 * failure rather than hanging the test runner.</p>
	 *
	 * @param operation the operation to fire.
	 * @param dialogSteps steps that dismiss dialogs, in the order they open.
	 */
	private static void fireDialogOperation(
			final Runnable operation,
			final Runnable... dialogSteps
	) {
		final AtomicReference<@Nullable Throwable> operationFailure = new AtomicReference<>();
		final CountDownLatch operationDone = new CountDownLatch(1);
		Platform.runLater(() -> {
			for (final Runnable step : dialogSteps) {
				Platform.runLater(step);
			}
			try {
				operation.run();
			} catch (final RuntimeException | Error e) {
				operationFailure.set(e);
			} finally {
				operationDone.countDown();
			}
		});
		final boolean completed = awaitUninterruptibly(
				operationDone, DIALOG_TIMEOUT_SECONDS
		);
		if (!completed) {
			Platform.runLater(() -> findShowingDialogPane().ifPresent(pane -> {
				final Button cancel = cancelButton(pane);
				if (cancel != null) {
					cancel.fire();
				}
			}));
			if (!awaitUninterruptibly(operationDone, 1)) {
				throw new AssertionError(
						"Timed out waiting for the operation to complete"
								+ " (dialog still open after force-cancel)"
				);
			}
			throw new AssertionError(
					"Dialog step did not dismiss the dialog within "
							+ DIALOG_TIMEOUT_SECONDS + "s"
			);
		}
		if (operationFailure.get() != null) {
			throw new AssertionError(
					"Operation threw",
					operationFailure.get()
			);
		}
		// Let the FX thread finish the post-dialog portion of the operation
		// (setActive, followNamingChange, updateStoredLayoutNaming) before
		// the test asserts on state.
		WaitForAsyncUtils.waitForFxEvents();
	}

	/**
	 * {@return whether {@code latch} counted down to zero within the timeout},
	 * ignoring interrupts and restoring the interrupt flag on return.
	 *
	 * @param latch
	 * 		the latch to wait on.
	 * @param timeout
	 * 		how long to wait.
	 */
	private static boolean awaitUninterruptibly(
			final CountDownLatch latch,
			final long timeout
	) {
		final long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeout);
		boolean interrupted = false;
		try {
			while (true) {
				final long remaining = deadlineNanos - System.nanoTime();
				if (remaining <= 0) {
					return false;
				}
				try {
					return latch.await(remaining, TimeUnit.NANOSECONDS);
				} catch (final InterruptedException e) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * {@return a step that types into the currently-showing text-input dialog,
	 * then dismisses it with OK.}
	 *
	 * @param text what to put in the text field.
	 */
	private static Runnable typeAndDismiss(final String text) {
		return () -> runDialogStep(pane -> {
			((TextField) Objects.requireNonNull(
					pane.lookup(".text-field"), ".text-field"
			)).setText(text);
			((Button) pane.lookupButton(OK)).fire();
		});
	}

	/**
	 * Picks the Workspace value in the currently-showing choice dialog, then
	 * dismisses it with OK.
	 *
	 * <p>The selection goes through the {@link ComboBox} the dialog builds,
	 * which is what the dialog reads its result from. Written as a plain
	 * method rather than a factory returning a {@link Runnable}, so a call
	 * site can pass it as {@code LayoutOperationsITG::selectChoiceAndDismiss}
	 * without the outer "return a lambda" wrapper ErrorProne flags.</p>
	 */
	private static void selectChoiceAndDismiss() {
		runDialogStep(pane -> {
			@SuppressWarnings("unchecked")
			final ComboBox<String> choices = (ComboBox<String>) Objects.requireNonNull(
					pane.lookup(".combo-box"), ".combo-box"
			);

			assertThat(choices.getItems())
					.describedAs("choices offered by the dialog")
					.contains("Workspace");

			choices.getSelectionModel().select("Workspace");
			((Button) pane.lookupButton(OK)).fire();
		});
	}

	/**
	 * Runs a dialog-interaction step on the dialog currently showing. On a
	 * throw from the step, force-dismisses the dialog with CANCEL (or CLOSE)
	 * so {@code showAndWait} returns and the test fails with the real cause
	 * rather than hanging on an open dialog.
	 *
	 * <p>Runs on the FX Application Thread (from the nested event loop a
	 * {@code showAndWait} dialog pumps). Before the step reads the scene
	 * graph, {@link DialogPane#applyCss} and {@link DialogPane#layout} are
	 * called to force style and layout passes synchronously, so a lookup for
	 * a styled control resolves rather than racing the next pulse.</p>
	 *
	 * @param step what to do with the dialog pane.
	 */
	private static void runDialogStep(final Consumer<DialogPane> step) {
		final DialogPane pane = findShowingDialogPane().orElseThrow(
				() -> new IllegalStateException(
						"No dialog is currently showing"
				)
		);
		pane.applyCss();
		pane.layout();
		try {
			step.accept(pane);
		} catch (final RuntimeException | Error e) {
			final Button cancel = cancelButton(pane);
			if (cancel != null) {
				cancel.fire();
			}
			throw e;
		}
	}

	/**
	 * {@return the dialog's cancel-equivalent button, or {@code null} when
	 * the dialog has no CANCEL or CLOSE button.}
	 *
	 * @param pane the dialog to inspect.
	 */
	private static @Nullable Button cancelButton(final DialogPane pane) {
		for (final ButtonType buttonType :
				List.of(ButtonType.CANCEL, ButtonType.CLOSE)) {
			final Node button = pane.lookupButton(buttonType);
			if (button instanceof Button b) {
				return b;
			}
		}
		return null;
	}

/**
	 * {@return the {@link DialogPane} of the dialog currently showing, or an
	 * empty {@link Optional} when none is showing.}
	 */
	private static Optional<DialogPane> findShowingDialogPane() {
		return Window.getWindows().stream()
				.filter(Window::isShowing)
				.map(Window::getScene)
				.filter(scene -> scene != null && scene.getRoot() instanceof DialogPane)
				.map(scene -> (DialogPane) scene.getRoot())
				.findFirst();
	}

	/**
	 * A {@link LayoutOperationsContext} that records whether
	 * {@link #showLayoutError} was called and returns a configurable answer
	 * from {@link #confirmLayoutAction}, without opening any dialog.
	 */
	private static final class FakeContext implements LayoutOperationsContext {

		boolean confirmResult = true;
		boolean showLayoutErrorCalled = false;

		@Override
		public boolean confirmLayoutAction(
				final String header,
				final String content
		) {
			return confirmResult;
		}

		@Override
		public void showLayoutError(
				final String header,
				final @Nullable String content
		) {
			showLayoutErrorCalled = true;
		}
	}
}
