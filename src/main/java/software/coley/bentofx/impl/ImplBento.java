package software.coley.bentofx.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;
import software.coley.bentofx.Bento;
import software.coley.bentofx.builder.DockableBuilder;
import software.coley.bentofx.builder.LayoutBuilder;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableCloseListener;
import software.coley.bentofx.dockable.DockableDestination;
import software.coley.bentofx.dockable.DockableMoveListener;
import software.coley.bentofx.dockable.DockableOpenListener;
import software.coley.bentofx.dockable.DockableSelectListener;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.impl.space.ImplEmptyDockSpace;
import software.coley.bentofx.layout.DockLayout;
import software.coley.bentofx.layout.LeafDockLayout;
import software.coley.bentofx.layout.RootDockLayout;
import software.coley.bentofx.space.DockSpace;
import software.coley.bentofx.space.EmptyDisplayFactory;
import software.coley.bentofx.util.BentoUtils;
import software.coley.bentofx.util.DragDropStage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ImplBento implements Bento {
	private final List<DockableOpenListener> openListeners = new CopyOnWriteArrayList<>();
	private final List<DockableMoveListener> moveListeners = new CopyOnWriteArrayList<>();
	private final List<DockableCloseListener> closeListeners = new CopyOnWriteArrayList<>();
	private final List<DockableSelectListener> selectListeners = new CopyOnWriteArrayList<>();
	private final ObservableList<RootDockLayout> rootLayouts = FXCollections.observableArrayList();
	private EmptyDisplayFactory emptyDisplayFactory = EmptyDisplayFactory.BLANK;

	@Nonnull
	@Override
	public ObservableList<RootDockLayout> getRootLayouts() {
		return FXCollections.unmodifiableObservableList(rootLayouts);
	}

	@Nonnull
	@Override
	public LayoutBuilder newLayoutBuilder() {
		return new LayoutBuilder(this);
	}

	@Nonnull
	@Override
	public DockableBuilder newDockableBuilder() {
		return new DockableBuilder(this);
	}

	@Nonnull
	@Override
	public DockSpace newEmptySpace(@Nonnull DockLayout parentLayout) {
		Node display = emptyDisplayFactory.build(parentLayout);
		return new ImplEmptyDockSpace(BentoUtils.newIdentifier(), display);
	}

	@Override
	public void setEmptyDisplayFactory(@Nullable EmptyDisplayFactory factory) {
		if (factory == null)
			factory = EmptyDisplayFactory.BLANK;
		emptyDisplayFactory = factory;
	}

	@Nonnull
	@Override
	public Stage newStageForDroppedHeader(@Nonnull DockableDestination source, @Nonnull Header header) {
		Scene sourceScene = source.getBackingRegion().getScene();
		double width = source.getBackingRegion().getWidth();
		double height = source.getBackingRegion().getHeight();
		Dockable dockable = header.getDockable();
		return newStageForDockable(sourceScene, dockable, width, height);
	}

	@Nonnull
	@Override
	public Stage newStageForDockable(@Nullable Scene sourceScene, @Nonnull Dockable dockable, double width, double height) {
		LayoutBuilder builder = newLayoutBuilder();
		LeafDockLayout layout = builder.leaf(builder.tabbed(Side.TOP, dockable));
		Region region = builder.root(layout).getBackingRegion();

		// TODO: Need to allow users to control the creation of stages/scenes
		Stage stage = new DragDropStage(true);
		Scene scene = new Scene(region, width, height);
		stage.setScene(scene);

		if (sourceScene != null) {
			scene.setUserAgentStylesheet(sourceScene.getUserAgentStylesheet());
			scene.getStylesheets().addAll(sourceScene.getStylesheets());

			Window sourceWindow = sourceScene.getWindow();
			stage.initOwner(sourceWindow);
			stage.setMinWidth(150);
			stage.setMinHeight(100);
			if (sourceWindow instanceof Stage sourceStage) {
				stage.getIcons().addAll(sourceStage.getIcons());
			}
		}

		return stage;
	}

	@Override
	public void addDockableOpenListener(@Nonnull DockableOpenListener listener) {
		openListeners.add(listener);
	}

	@Override
	public boolean removeDockableOpenListener(@Nonnull DockableOpenListener listener) {
		return openListeners.remove(listener);
	}

	@Override
	public void addDockableMoveListener(@Nonnull DockableMoveListener listener) {
		moveListeners.add(listener);
	}

	@Override
	public boolean removeDockableMoveListener(@Nonnull DockableMoveListener listener) {
		return moveListeners.remove(listener);
	}

	@Override
	public void addDockableCloseListener(@Nonnull DockableCloseListener listener) {
		closeListeners.add(listener);
	}

	@Override
	public boolean removeDockableCloseListener(@Nonnull DockableCloseListener listener) {
		return closeListeners.remove(listener);
	}

	@Override
	public void addDockableSelectListener(@Nonnull DockableSelectListener listener) {
		selectListeners.add(listener);
	}

	@Override
	public boolean removeDockableSelectListener(@Nonnull DockableSelectListener listener) {
		return selectListeners.remove(listener);
	}

	@Nonnull
	public List<DockableOpenListener> getOpenListeners() {
		return openListeners;
	}

	@Nonnull
	public List<DockableMoveListener> getMoveListeners() {
		return moveListeners;
	}

	@Nonnull
	public List<DockableCloseListener> getCloseListeners() {
		return closeListeners;
	}

	@Nonnull
	public List<DockableSelectListener> getSelectListeners() {
		return selectListeners;
	}

	public void registerRoot(@Nonnull RootDockLayout layout) {
		if (!rootLayouts.contains(layout))
			rootLayouts.add(layout);
	}

	public void unregisterRoot(@Nonnull RootDockLayout layout) {
		rootLayouts.remove(layout);
	}
}
