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
import software.coley.bentofx.builder.ContentBuilder;
import software.coley.bentofx.builder.DockableBuilder;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.content.EmptyContentDisplayFactory;
import software.coley.bentofx.dockable.DockableCloseListener;
import software.coley.bentofx.dockable.DockableDestination;
import software.coley.bentofx.dockable.DockableMoveListener;
import software.coley.bentofx.dockable.DockableOpenListener;
import software.coley.bentofx.dockable.DockableSelectListener;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.impl.content.ImplEmptyContent;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.LeafContentLayout;
import software.coley.bentofx.layout.RootContentLayout;
import software.coley.bentofx.util.BentoUtils;
import software.coley.bentofx.util.DragDropStage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ImplBento implements Bento {
	private final List<DockableOpenListener> openListeners = new CopyOnWriteArrayList<>();
	private final List<DockableMoveListener> moveListeners = new CopyOnWriteArrayList<>();
	private final List<DockableCloseListener> closeListeners = new CopyOnWriteArrayList<>();
	private final List<DockableSelectListener> selectListeners = new CopyOnWriteArrayList<>();
	private final ObservableList<RootContentLayout> rootLayouts = FXCollections.observableArrayList();
	private EmptyContentDisplayFactory emptyDisplayFactory = EmptyContentDisplayFactory.BLANK;

	@Nonnull
	@Override
	public ObservableList<RootContentLayout> getRootLayouts() {
		return FXCollections.unmodifiableObservableList(rootLayouts);
	}

	@Nonnull
	@Override
	public ContentBuilder newContentBuilder() {
		return new ContentBuilder(this);
	}

	@Nonnull
	@Override
	public DockableBuilder newDockableBuilder() {
		return new DockableBuilder(this);
	}

	@Nonnull
	@Override
	public Content newEmptyContent(@Nonnull ContentLayout parentLayout) {
		Node display = emptyDisplayFactory.build(parentLayout);
		return new ImplEmptyContent(BentoUtils.newIdentifier(), display);
	}

	@Override
	public void setEmptyDisplayFactory(@Nullable EmptyContentDisplayFactory factory) {
		if (factory == null)
			factory = EmptyContentDisplayFactory.BLANK;
		emptyDisplayFactory = factory;
	}

	@Nonnull
	@Override
	public Stage newStageForDroppedHeader(@Nonnull DockableDestination source, @Nonnull Header header) {
		ContentBuilder builder = newContentBuilder();
		LeafContentLayout layout = builder.leaf(builder.tabbed(Side.TOP, header.getDockable()));
		Region region = builder.root(layout).getBackingRegion();

		// TODO: Need to allow users to control the creation of stages/scenes
		Stage stage = new DragDropStage(true);
		Scene scene = new Scene(region, source.getBackingRegion().getWidth(), source.getBackingRegion().getHeight());
		stage.setScene(scene);

		Scene sourceScene = source.getBackingRegion().getScene();
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

	public void registerRoot(@Nonnull RootContentLayout layout) {
		if (!rootLayouts.contains(layout))
			rootLayouts.add(layout);
	}

	public void unregisterRoot(@Nonnull RootContentLayout layout) {
		rootLayouts.remove(layout);
	}
}
