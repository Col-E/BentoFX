package software.coley.bentofx.demo.persistence;

import javafx.application.Application;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.bentofx.Bento;
import software.coley.bentofx.building.DockBuilding;
import software.coley.bentofx.control.DragDropStage;
import software.coley.bentofx.demo.persistence.provider.BoxAppDockContainerLeafMenuFactoryProvider;
import software.coley.bentofx.demo.persistence.provider.BoxAppDockableMenuFactoryProvider;
import software.coley.bentofx.demo.persistence.provider.BoxAppDockableStateProvider;
import software.coley.bentofx.demo.persistence.provider.BoxAppStageIconImageProvider;
import software.coley.bentofx.demo.persistence.provider.DockableProperties;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.event.DockEvent;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.persistence.core.api.BentoLayout;
import software.coley.bentofx.persistence.core.api.BentoLayout.BentoLayoutBuilder;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.DockingLayout;
import software.coley.bentofx.persistence.core.api.DockingLayout.DockingLayoutBuilder;
import software.coley.bentofx.persistence.core.api.DockingLayoutPersistence;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.LayoutRestorer;
import software.coley.bentofx.persistence.core.api.LayoutSaver;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.provider.DockContainerLeafMenuFactoryProvider;
import software.coley.bentofx.persistence.core.api.provider.DockableStateProvider;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutRestorable;
import software.coley.bentofx.persistence.core.api.provider.StageIconImageProvider;
import software.coley.bentofx.persistence.core.api.state.DockableState;

import java.util.List;
import java.util.function.Supplier;

import static software.coley.bentofx.demo.persistence.provider.DockableProperties.*;
import static software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers.SESSION_LAYOUT_IDENTIFIER;

/**
 * JavaFX application that demonstrates using the BentoFX docking and docking
 * persistence frameworks. Derived from the {@code BoxApp} class in the basic
 * demo.
 *
 * @author Matt Coley
 * @author Phil Bryant
 */
public class BoxApp extends Application implements DockingLayoutRestorable {

	private static final Logger logger =
			LoggerFactory.getLogger(BoxApp.class);

	private final Bento bento = new Bento("box-app-bento");

	private final DockingLayoutPersistenceProvider persistenceProvider =
			DockingLayoutPersistence.provider();

	private final DockableStateProvider dockableStateProvider =
			new BoxAppDockableStateProvider(
					new BoxAppDockableMenuFactoryProvider()
			);

	private final StageIconImageProvider stageIconImageProvider =
			new BoxAppStageIconImageProvider();

	private final DockContainerLeafMenuFactoryProvider dockContainerLeafMenuFactoryProvider =
			new BoxAppDockContainerLeafMenuFactoryProvider();

	private final BentoProvider bentoProvider = BentoProvider.of(bento);

	private @Nullable Stage stage;

	private @Nullable LayoutSaver layoutSaver;

	/**
	 * The scene root, holding the menu bar above the docking tree.
	 *
	 * <p>Built by the first {@link #applyBentoLayout} and kept, so that a
	 * layout change replaces the docking tree below the menu bar rather than
	 * the whole scene, so the menu that started a change survives it.</p>
	 */
	private @Nullable VBox sceneRoot;

	@Override
	public void start(Stage stage) {
		this.stage = stage;
		stage.setWidth(1000);
		stage.setHeight(700);

		// Initialize the Bento
		bento.placeholderBuilding().setDockablePlaceholderFactory(dockable -> new Label("Empty Dockable"));
		bento.placeholderBuilding().setContainerPlaceholderFactory(container -> new Label("Empty Container"));
		bento.events().addEventListener((DockEvent event) -> {
			if (event instanceof DockEvent.DockableClosing closingEvent)
				handleDockableClosing(closingEvent);
		});
		bento.stageBuilding().setApplyMousePosition(true);
		bento.stageBuilding().setApplySourceAsOwner(false);

		stage.setTitle("BentoFX Persistence Demo");
		stage.getIcons().addAll(
				stageIconImageProvider.getStageIcons()
		);
		stage.centerOnScreen();

		// We need to save the docking layout on close request because the stage
		// is (and all other windows are) no longer available after they are
		// closed and will not be discoverable when saving the docking layout.
		stage.setOnCloseRequest(this::saveDockingLayout);
		stage.setOnHidden(e -> System.exit(0));

		// A Scene is created and additional Stage properties are set when
		// applying the docking layout.
		DockingLayout dockingLayout = getDockingLayout(
				LayoutPersistenceProfile.of(SESSION_LAYOUT_IDENTIFIER),
				this::getDefaultDockingLayout
		);

		if (!applyDockingLayout(dockingLayout)) {
			// Nothing was applied, so the stage has no Scene and was never
			// shown. Fall back to the default layout to keep the application
			// usable; continuing without a saved layout would leave the
			// application process running with no window.
			discardDockingLayout(dockingLayout);

			logger.warn(
					"Could not apply the restored docking layout; " +
							"applying the default docking layout instead."
			);

			if (!applyDockingLayout(getDefaultDockingLayout())) {
				logger.error("Could not apply the default docking layout.");
			}
		}

		// Built last: a capture only sees root branches that have a Scene, and
		// auto-save starts as soon as the saver exists.
		layoutSaver = createLayoutSaver();
	}

	/**
	 * {@return a new root branch holding the layout this demo builds for
	 * itself.}
	 *
	 * <p>Built afresh on each call, so {@link #getDefaultDockingLayout()} never
	 * hands back a branch that has been shown and rearranged since.</p>
	 */
	private DockContainerRootBranch buildDefaultRootBranch() {
		final DockBuilding builder = bento.dockBuilding();
		final DockContainerRootBranch branchRoot = builder.root("root");
		final DockContainerBranch branchWorkspace = builder.branch("workspace");
		final DockContainerLeaf leafWorkspaceTools = builder.leaf("workspace-tools");
		final DockContainerLeaf leafWorkspaceHeaders = builder.leaf("workspace-headers");
		final DockContainerLeaf leafTools = builder.leaf("misc-tools");

		branchWorkspace.setPruneWhenEmpty(false);
		leafWorkspaceTools.setPruneWhenEmpty(false);
		leafTools.setPruneWhenEmpty(false);

		// Add dummy menus to each.
		dockContainerLeafMenuFactoryProvider.getDockContainerLeafMenuFactory(
				leafTools.getIdentifier()
		).ifPresent(leafTools::setMenuFactory);
		dockContainerLeafMenuFactoryProvider.getDockContainerLeafMenuFactory(
				leafWorkspaceHeaders.getIdentifier()
		).ifPresent(leafWorkspaceHeaders::setMenuFactory);
		dockContainerLeafMenuFactoryProvider.getDockContainerLeafMenuFactory(
				leafWorkspaceTools.getIdentifier()
		).ifPresent(leafWorkspaceTools::setMenuFactory);

		// These leaves shouldn't auto-expand. They are intended to be a set size.
		SplitPane.setResizableWithParent(leafTools, false);
		SplitPane.setResizableWithParent(leafWorkspaceTools, false);

		// Root: Workspace on top, tools on bottom
		// Workspace: Explorer on left, primary editor tabs on right
		branchRoot.setOrientation(Orientation.VERTICAL);
		branchWorkspace.setOrientation(Orientation.HORIZONTAL);
		branchRoot.addContainers(branchWorkspace, leafTools);
		branchWorkspace.addContainers(leafWorkspaceTools, leafWorkspaceHeaders);

		// Changing tool header sides to be aligned with application's far edges (to facilitate better collapsing UX)
		leafWorkspaceTools.setSide(Side.LEFT);
		leafTools.setSide(Side.BOTTOM);

		// Tools shouldn't allow splitting (mirroring IntelliJ behavior)
		leafWorkspaceTools.setCanSplit(false);
		leafTools.setCanSplit(false);

		// Primary editor space should not prune when empty
		leafWorkspaceHeaders.setPruneWhenEmpty(false);

		// Set intended sizes for tools (leaf does not need to be a direct child, just some level down in the chain)
		branchRoot.setContainerSizePx(leafTools, 200);
		branchRoot.setContainerSizePx(leafWorkspaceTools, 300);

		// Make the bottom collapsed by default
		branchRoot.setContainerCollapsed(leafTools, true);

		// Adding dockables to the WorkspaceTools leaf
		addDockable(WORKSPACE, dockableStateProvider, leafWorkspaceTools);
		addDockable(BOOKMARKS, dockableStateProvider, leafWorkspaceTools);
		addDockable(MODIFICATIONS, dockableStateProvider, leafWorkspaceTools);

		// Adding dockables to Tools leaf
		addDockable(LOGGING, dockableStateProvider, leafTools);
		addDockable(TERMINAL, dockableStateProvider, leafTools);
		addDockable(PROBLEMS, dockableStateProvider, leafTools);

		// Adding dockables to WorkspaceHeaders leaf
		addDockable(CLASS_1, dockableStateProvider, leafWorkspaceHeaders);
		addDockable(CLASS_2, dockableStateProvider, leafWorkspaceHeaders);
		addDockable(CLASS_3, dockableStateProvider, leafWorkspaceHeaders);
		addDockable(CLASS_4, dockableStateProvider, leafWorkspaceHeaders);
		addDockable(CLASS_5, dockableStateProvider, leafWorkspaceHeaders);

		return branchRoot;
	}

	/**
	 * Builds and returns the {@link Dockable} for the specified
	 * {@link DockableState}.
	 *
	 * @param dockableState the {@link DockableState} specifying the state of
	 * the {@link Dockable} to be built.
	 *
	 * @return the {@link Dockable} for the specified {@link DockableState}.
	 */
	private Dockable buildDockable(
			final DockableState dockableState
	) {
		final DockBuilding dockBuilding = bento.dockBuilding();

		final Dockable dockable = dockBuilding.dockable(dockableState.getIdentifier());
		dockableState.getDockableNode().ifPresent(dockable::setNode);
		dockableState.getTitle().ifPresent(dockable::setTitle);
		dockableState.getTooltipText().ifPresent(tooltipText -> dockable.setTooltip(new Tooltip(tooltipText)));
		dockableState.getDockableIconFactory().ifPresent(dockable::setIconFactory);
		dockableState.getDockableMenuFactory().ifPresent(dockable::setContextMenuFactory);
		return dockable;
	}

	/**
	 * Called when a {@link DockEvent.DockableClosing} occurs.
	 *
	 * @param closingEvent the {@link Event} that occurred when the
	 * {@link DockContainerLeaf} closed a {@link Dockable} item.
	 */
	private void handleDockableClosing(DockEvent.DockableClosing closingEvent) {
		final Dockable dockable = closingEvent.dockable();
		if (!dockable.getTitle().startsWith("Class "))
			return;

		final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Confirmation");
		alert.setHeaderText(null);
		alert.setContentText("Save changes to [" + dockable.getTitle() + "] before closing?");
		alert.getButtonTypes().setAll(
				ButtonType.YES,
				ButtonType.NO,
				ButtonType.CANCEL
		);

		final ButtonType result = alert.showAndWait()
				.orElse(ButtonType.CANCEL);

		if (result.equals(ButtonType.YES)) {
			// simulate saving application (not docking layout) state
			logger.debug("Saving {}...", dockable.getTitle());

		} else if (result.equals(ButtonType.NO)) {
			// nothing to do - just close
			logger.debug("Closing {} without saving...", dockable.getTitle());
		} else if (result.equals(ButtonType.CANCEL)) {
			// prevent closing
			closingEvent.cancel();
		}
	}

	/**
	 * Optionally adds the {@code Dockable} with the provided {@code dockableId}
	 * to the {@code DockContainer}. Logs a warning message when the
	 * {@code Dockable} cannot be resolved using the {@code dockableId}.
	 *
	 * @param dockableProperties the identifier for the {@code Dockable} to add.
	 * @param container the {@code DockContainer} to which the {@code Dockable}
	 * should be added.
	 */
	private void addDockable(
			final DockableProperties dockableProperties,
			final DockableStateProvider dockableStateProvider,
			final DockContainer container
	) {
		dockableStateProvider.resolveDockableState(dockableProperties.getIdentifier())
				.ifPresentOrElse(
						dockableState ->
								// Our application isn't doing anything with the
								// reconstructed Dockable. Just add it to the
								// container.
								container.addDockable(buildDockable(dockableState)),
						() -> logger.warn("Could not add dockable {}.", dockableProperties)
				);
	}

	/**
	 * {@return the {@link LayoutSaver} for this application's layout, or
	 * {@code null} when one cannot be created.}
	 *
	 * <p>The saver returned from a {@link DockingLayoutPersistenceProvider}
	 * already has auto-save running, so this is called where auto-save should
	 * start rather than where the layout is saved: once while the application
	 * is starting, and again after a switch has replaced the tree it was
	 * watching.</p>
	 */
	private @Nullable LayoutSaver createLayoutSaver() {
		try {
			return persistenceProvider.getLayoutSaver(
					SESSION_LAYOUT_IDENTIFIER,
					bentoProvider
			);
		} catch (final BentoStateException e) {
			logger.warn("Could not create the docking layout saver.", e);
			return null;
		}
	}

	/**
	 * {code EventHandler<WindowEvent>} implementation that saves the docking
	 * layout and then releases the saver; it does <b><i><u>not</u></i></b> save
	 * the layout of the main Stage, non-docking components, or other application
	 * state.
	 *
	 * <p>The saver is closed here rather than from {@code stop()} because this
	 * runs while the windows still exist, and closing is what removes the saver's
	 * listener from each {@code Bento} and stops its auto-save. Saving explicitly
	 * first is deliberate: closing saves only when a dock event has been received
	 * since the last save. Both are done by
	 * {@link #saveAndReleaseLayoutSaver()}, which this application's own exit
	 * and a layout switch need as well.</p>
	 *
	 * @param windowEvent unused.
	 */
	private void saveDockingLayout(final WindowEvent windowEvent) {
		saveAndReleaseLayoutSaver();
	}

	/**
	 * Saves the docking layout and then releases the saver, leaving auto-save
	 * stopped.
	 *
	 * <p>Called when the window is closing, when {@code File | Exit} is chosen,
	 * and before a layout switch. A switch needs both halves of this for its
	 * own reasons: the outgoing layout has to be written before it is taken
	 * apart, and auto-save has to be down while it is, or a capture landing
	 * mid-switch writes a layout that is neither one.</p>
	 *
	 * <p>The field is cleared first, so a second call cannot close the same
	 * saver twice - the window's close request and this application's own exit
	 * both reach here.</p>
	 */
	private void saveAndReleaseLayoutSaver() {
		final LayoutSaver saver = layoutSaver;
		layoutSaver = null;

		if (saver == null) {
			return;
		}

		try (saver) {
			saver.saveLayout();
		} catch (final BentoStateException e) {
			logger.warn("Could not save the docking layout.", e);
		}
	}

	@Override
	public DockingLayout getDockingLayout(
			final LayoutPersistenceProfile layoutPersistenceProfile,
			final Supplier<DockingLayout> fallbackLayoutSupplier
	) {

		// The restorer owns the LayoutStorage it was given and closes it, so it is
		// closed here rather than abandoned. The layout it returns is already built,
		// so closing the storage afterward costs nothing.
		try (final LayoutRestorer layoutRestorer =
					 persistenceProvider.getLayoutRestorer(
							 layoutPersistenceProfile,
							 bentoProvider,
							 dockableStateProvider,
							 stageIconImageProvider,
							 dockContainerLeafMenuFactoryProvider
					 )) {

			return layoutRestorer.restoreLayout(fallbackLayoutSupplier);
		} catch (BentoStateException e) {
			logger.warn("Could not create the docking layout restorer.", e);
			return fallbackLayoutSupplier.get();
		}
	}

	/**
	 * Applies all {@link BentoLayout} found in the {@link DockingLayout}.
	 *
	 * @param dockingLayout the {@link DockingLayout} to be applied.
	 *
	 * @return {@code true} when at least one {@link BentoLayout} was applied;
	 * otherwise, {@code false}, which means no {@link Scene} was set and the
	 * caller has to apply a layout that can be.
	 */
	private boolean applyDockingLayout(
			final DockingLayout dockingLayout
	) {
		boolean isApplied = false;

		for (final BentoLayout bentoLayout :
				dockingLayout.getBentoLayouts()) {
			if (bentoLayout.matchesIdentity(bento)) {
				isApplied |= applyBentoLayout(bentoLayout);
			} else {
				logger.warn(
						"Unknown BentoLayout identifier: {}",
						bentoLayout.getIdentifier()
				);
			}
		}

		return isApplied;
	}

	/**
	 * Lets go of a restored {@link DockingLayout} that is not going to be
	 * applied.
	 *
	 * <p>A restorer gives every drag/drop stage it rebuilds a {@code Scene}
	 * straight away, and a root branch registers itself with its {@link Bento}
	 * as soon as it has one. So a layout that is restored and then abandoned
	 * leaves root branches registered that nothing is showing, and the next
	 * save captures them: the layout written then holds more than one root
	 * branch, {@link #applyBentoLayout} refuses such a layout, and the next
	 * launch quietly comes up with the default layout instead.</p>
	 *
	 * <p>Unregistering by hand rather than by hiding the stages: a drag/drop
	 * stage does clear its scene root when hidden, and that is what unregisters
	 * a floating layout the user was looking at, but it happens from a
	 * window-hidden event filter. These stages were never shown, so hiding them
	 * raises no event and would leave every branch registered.</p>
	 *
	 * @param dockingLayout the layout being abandoned.
	 */
	private void discardDockingLayout(final DockingLayout dockingLayout) {
		for (final BentoLayout bentoLayout : dockingLayout.getBentoLayouts()) {
			for (final DragDropStage dragDropStage :
					bentoLayout.getDragDropStages()) {

				final Scene dragDropStageScene = dragDropStage.getScene();

				if (dragDropStageScene != null
						&& dragDropStageScene.getRoot()
						instanceof DockContainerRootBranch rootBranch) {
					bento.unregisterRoot(rootBranch);
				}
			}
		}
	}

	@Override
	public DockingLayoutPersistenceProvider getPersistenceProvider() {
		return persistenceProvider;
	}

	@Override
	public BentoProvider getBentoProvider() {
		return bentoProvider;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Built for {@link #bento} from {@link #buildDefaultRootBranch()}, afresh
	 * on each call.</p>
	 */
	@Override
	public DockingLayout getDefaultDockingLayout() {

		DockingLayoutBuilder dockingLayoutBuilder =
				new DockingLayoutBuilder();

		BentoLayoutBuilder bentoLayoutBuilder = new BentoLayoutBuilder(
				bento.getIdentifier()
		);
		bentoLayoutBuilder.addRootBranch(buildDefaultRootBranch());
		dockingLayoutBuilder.addBentoLayout(bentoLayoutBuilder.build());

		return dockingLayoutBuilder.build();
	}

	/**
	 * Applies the {@link BentoLayout} to docking components.
	 *
	 * @param bentoLayout the layout to be applied.
	 *
	 * @return {@code true} when the layout was applied to the {@link Stage};
	 * otherwise, {@code false}.
	 */
	private boolean applyBentoLayout(final BentoLayout bentoLayout) {
		final List<DockContainerRootBranch> bentoRootBranches =
				bentoLayout.getRootBranches();

		if (bentoRootBranches.size() != 1) {
			// This stage only has one root branch
			logger.error(
					"The stage should have one root branch but {} " +
							"were found.",
					bentoRootBranches.size()
			);
			return false;
		}

		if (stage == null) {
			// The primary stage should have been set when the application was started
			logger.error("The stage cannot be null.");
			return false;
		}

		if (!bentoLayout.matchesIdentity(bento)) {
			// A DockingLayout can have multiple BentoLayout; make sure we're
			// applying the right one
			logger.warn(
					"Cannot apply BentoLayout {} to {}.",
					bentoLayout.getIdentifier(),
					bento.getIdentifier()
			);
			return false;
		}

		// Apply the root branch of the BentoLayout
		final DockContainerRootBranch bentoRootBranch =
				bentoRootBranches.getFirst();

		// The docking tree takes every pixel the menu bar leaves.
		VBox.setVgrow(bentoRootBranch, Priority.ALWAYS);

		VBox currentSceneRoot = sceneRoot;

		if (currentSceneRoot == null) {

			// One Scene and one MenuBar for as long as the application runs.
			// Switching layouts replaces the docking tree below the menu bar,
			// so the menu used to start the switch is still there afterward.

			// The MenuBar is built here rather than in a field initializer
			// because those run in the constructor, on the JavaFX-Launcher
			// thread, where JavaFX components cannot be built.
			currentSceneRoot = new VBox(
					new BoxAppMenuBar(
							stage,
							this,
							this::exitApplication
					),
					bentoRootBranch
			);
			sceneRoot = currentSceneRoot;

			final Scene scene = new Scene(currentSceneRoot);
			scene.getStylesheets().add("/bento.css");
			stage.setScene(scene);
		} else {
			// Child 0 is the menu bar and child 1 the docking tree. Replacing
			// child 1 is also what clears the outgoing branch's Scene, and that
			// is what unregisters it from the Bento so the next save does not
			// capture both trees.
			currentSceneRoot.getChildren().set(1, bentoRootBranch);
		}

		stage.show();

		// Show the DragDropStages that were showing when the layout was saved.
		// Only reached after the main layout is up; otherwise, the only thing
		// on screen would be floating windows from a layout whose root branch
		// could not be applied.
		for (final DragDropStage dragDropStage :
				bentoLayout.getDragDropStages()) {
			if (bentoLayout.wasShowing(dragDropStage)) {
				dragDropStage.show();
			}
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The arrangement being left is written to the <em>session</em> layout,
	 * not to the named layout the user was working in. The saver this releases
	 * was built for the session layout, and that is the layout meant to hold
	 * whatever was last on screen so a restart returns to it. The consequence
	 * worth knowing is that rearranging a named layout and then switching away
	 * does not put those changes with the name: {@code Save Changes} is what
	 * does that.</p>
	 */
	@Override
	public boolean switchToLayout(
			final Supplier<DockingLayout> dockingLayoutSupplier
	) {
		// Auto-save comes down for the whole switch, and releasing the saver
		// writes the outgoing layout on the way. Between taking one tree out
		// and putting the next one in, this Bento knows about both trees or
		// neither, and a capture landing there would write a layout that is
		// neither the one being left nor the one being restored.
		saveAndReleaseLayoutSaver();

		try {
			final DockingLayout dockingLayout = dockingLayoutSupplier.get();

			// Taken before anything is applied. The window list is live, and
			// applying the incoming layout shows floating windows of its own,
			// which must not be hidden along with the outgoing ones.
			final List<DragDropStage> outgoingDragDropStages =
					getShowingDragDropStages();

			if (!applyDockingLayout(dockingLayout)) {
				// Nothing was applied. Everything applyBentoLayout checks, it
				// checks before it changes anything, so the layout on screen is
				// still whole.
				discardDockingLayout(dockingLayout);
				return false;
			}

			// Hiding is what takes the outgoing floating layouts apart: a
			// drag/drop stage clears its scene root when hidden, and that is
			// what unregisters the branch so the next save does not capture it.
			// Hiding raises no close request, so this does not run the
			// close-every-dockable path and does not ask about unsaved
			// dockables.
			for (final DragDropStage dragDropStage : outgoingDragDropStages) {
				dragDropStage.hide();
			}

			return true;
		} finally {
			// However the switch went, auto-save has to come back. Leaving it
			// down would leave the application running with nothing saving and
			// nothing on screen to say so.
			layoutSaver = createLayoutSaver();
		}
	}

	/**
	 * {@return the drag/drop stages on screen now.}
	 *
	 * <p>{@code Window.getWindows()} lists only windows that are showing, and
	 * it is a live list, so this copies it.</p>
	 */
	private static List<DragDropStage> getShowingDragDropStages() {
		return Window.getWindows().stream()
				.filter(DragDropStage.class::isInstance)
				.map(DragDropStage.class::cast)
				.toList();
	}

	/**
	 * Saves the docking layout and closes the application.
	 *
	 * <p>Handed to {@link BoxAppMenuBar} as the {@code File | Exit} action.</p>
	 *
	 * <p>Saving here rather than leaving it to the close-request handler:
	 * {@code Stage.close()} raises no close request, so choosing
	 * {@code File | Exit} would otherwise close the window without writing the
	 * layout. Closing the stage hides it, which is what ends the process.</p>
	 */
	private void exitApplication() {
		saveAndReleaseLayoutSaver();

		if (stage != null) {
			stage.close();
		}
	}
}
