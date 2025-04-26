package software.coley.boxfx.demo;

import jakarta.annotation.Nonnull;
import javafx.application.Application;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Dockable;
import software.coley.bentofx.builder.ContentBuilder;
import software.coley.bentofx.builder.DockableBuilder;
import software.coley.bentofx.builder.TabbedContentArgs;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.RootContentLayout;

public class BoxApp extends Application {
	private static final int TOOLS = 1;

	@Override
	public void start(Stage stage) {
		stage.setWidth(1000);
		stage.setHeight(700);

		// TODO: Allow making dockables configure their containing DockableDestination/ContentLayout when splitting/new-windowing
		//  - For example, tool tabs in new windows making their TabbedContent#canSplitProperty = false

		Bento bento = Bento.newBento();
		ContentBuilder builder = bento.newContentBuilder();
		ContentLayout layout = builder.vsplit(
			builder.hsplit(
				builder.tabbed(
					new TabbedContentArgs()
						.setSide(Side.LEFT)
						.addDockables(
							buildDockable(builder, 1, "Workspace").withClosable(false).withDragGroup(TOOLS),
							buildDockable(builder, 2, "Bookmarks").withClosable(false).withDragGroup(TOOLS),
							buildDockable(builder, 3, "Modifications").withClosable(false).withDragGroup(TOOLS)
						)
						.setResizeWithParent(false)
						.setAutoPruneWhenEmpty(false)
						.setCanSplit(false)
				),
				builder.tabbed(
					Side.TOP,
					makeDockable(builder, 1, "Class1"),
					makeDockable(builder, 2, "Class2"),
					makeDockable(builder, 3, "Class3"),
					makeDockable(builder, 4, "Class4"),
					makeDockable(builder, 5, "Class5")
				)
			),
			builder.tabbed(
				new TabbedContentArgs()
					.setSide(Side.BOTTOM)
					.addDockables(
						buildDockable(builder, 1, "Logging").withClosable(false).withDragGroup(TOOLS),
						buildDockable(builder, 2, "Terminal").withClosable(false).withDragGroup(TOOLS),
						buildDockable(builder, 3, "Problems").withClosable(false).withDragGroup(TOOLS)
					)
					.setResizeWithParent(false)
					.setAutoPruneWhenEmpty(false)
					.setCanSplit(false)
			)
		);
		RootContentLayout root = builder.root(layout);
		stage.setScene(new Scene(root.getBackingRegion()));
		stage.setOnHidden(e -> System.exit(0));
		stage.show();
	}

	@Nonnull
	private Dockable makeDockable(@Nonnull ContentBuilder builder, int i, @Nonnull String title) {
		return buildDockable(builder, i, title).build();
	}

	@Nonnull
	private DockableBuilder buildDockable(@Nonnull ContentBuilder builder, int i, @Nonnull String title) {
		Label content = new Label("<" + title + ":" + i + ">");
		content.setFocusTraversable(true); // Set to facilitate detection of 'focusWithinProperty()' in TabbedContent implementations
		return builder.dockable()
			.withTitle(title)
			.withIconFactory(dockable -> makeIcon(i))
			.withContent(content)
			.withCachedContextMenu(true)
			.withContextMenuFactory(dockable -> new ContextMenu(
				new MenuItem("Menu for : " + dockable.titleProperty().get()),
				new SeparatorMenuItem(),
				new MenuItem("Stuff")));
	}

	@Nonnull
	private static Circle makeIcon(int i) {
		Circle icon = switch (i) {
			case 1 -> new Circle(6, Color.RED);
			case 2 -> new Circle(6, Color.ORANGE);
			case 3 -> new Circle(6, Color.LIME);
			case 4 -> new Circle(6, Color.CYAN);
			case 5 -> new Circle(6, Color.BLUE);
			case 6 -> new Circle(6, Color.PURPLE);
			default -> new Circle(6, Color.GREY);
		};
		icon.setEffect(new InnerShadow(BlurType.ONE_PASS_BOX, Color.BLACK, 2F, 10F, 0, 0));
		return icon;
	}
}
