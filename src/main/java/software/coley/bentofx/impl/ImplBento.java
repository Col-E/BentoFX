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
import software.coley.bentofx.EmptyDisplayFactory;
import software.coley.bentofx.builder.ContentBuilder;
import software.coley.bentofx.builder.DockableBuilder;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.impl.content.ImplEmptyContent;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.LeafContentLayout;
import software.coley.bentofx.layout.RootContentLayout;
import software.coley.bentofx.util.BentoUtils;
import software.coley.bentofx.util.DragDropStage;

public class ImplBento implements Bento {
	private final ObservableList<RootContentLayout> rootLayouts = FXCollections.observableArrayList();
	private EmptyDisplayFactory emptyDisplayFactory = EmptyDisplayFactory.BLANK;

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
	public void setEmptyDisplayFactory(@Nullable EmptyDisplayFactory factory) {
		if (factory == null)
			factory = EmptyDisplayFactory.BLANK;
		emptyDisplayFactory = factory;
	}

	@Nonnull
	@Override
	public Stage newStageForDroppedHeader(@Nonnull Header header) {
		Scene sourceScene = header.getScene();
		ContentBuilder builder = newContentBuilder();
		LeafContentLayout layout = builder.tabbed(Side.TOP, header.getDockable());
		Region region = builder.root(layout).getBackingRegion();
		Stage stage = new DragDropStage(true);
		Scene scene = new Scene(region);
		stage.setScene(scene);
		if (sourceScene != null) {
			scene.setUserAgentStylesheet(sourceScene.getUserAgentStylesheet());
			scene.getStylesheets().addAll(sourceScene.getStylesheets());

			Window sourceWindow = scene.getWindow();
			stage.initOwner(sourceWindow);
			stage.setMinWidth(150);
			stage.setMinHeight(100);
			if (sourceWindow instanceof Stage sourceStage) {
				stage.getIcons().addAll(sourceStage.getIcons());
			}
		}
		return stage;
	}

	public void registerRoot(@Nonnull RootContentLayout layout) {
		if (!rootLayouts.contains(layout))
			rootLayouts.add(layout);
	}

	public void unregisterRoot(@Nonnull RootContentLayout layout) {
		rootLayouts.remove(layout);
	}
}
