package software.coley.boxfx.demo;

import jakarta.annotation.Nonnull;
import javafx.application.Application;
import javafx.geometry.Orientation;
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
import software.coley.bentofx.builder.ContentBuilder;
import software.coley.bentofx.builder.DockableBuilder;
import software.coley.bentofx.builder.SplitContentLayoutArgs;
import software.coley.bentofx.builder.TabbedContentArgs;
import software.coley.bentofx.content.TabbedContent;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.LeafContentLayout;
import software.coley.bentofx.layout.RootContentLayout;
import software.coley.bentofx.layout.SplitContentLayout;

public class BoxApp extends Application {
	private static final int TOOLS = 1;

	@Override
	public void start(Stage stage) {
		stage.setWidth(1000);
		stage.setHeight(700);

		// TODO: Allow making dockables configure their containing DockableDestination/ContentLayout when splitting/new-windowing
		//  - For example, tool tabs in new windows making their TabbedContent#canSplitProperty = false

		Bento bento = Bento.newBento();
		bento.addDockableOpenListener((path, dockable) -> System.out.println("Opened: " + dockable.titleProperty().get()));
		bento.addDockableMoveListener((oldPath, newPath, dockable) -> System.out.println("Moved: " + dockable.titleProperty().get()));
		bento.addDockableCloseListener((path, dockable) -> System.out.println("Closed: " + dockable.titleProperty().get()));
		bento.addDockableSelectListener((path, dockable) -> System.out.println("Select: " + dockable.titleProperty().get()));

		ContentBuilder builder = bento.newContentBuilder();

		TabbedContent toolTabs = builder.tabbed(new TabbedContentArgs()
				.setSide(Side.LEFT)
				.addDockables(
						buildDockable(builder, 1, "Workspace").withClosable(false).withDragGroup(TOOLS),
						buildDockable(builder, 2, "Bookmarks").withClosable(false).withDragGroup(TOOLS),
						buildDockable(builder, 3, "Modifications").withClosable(false).withDragGroup(TOOLS)
				)
				.setAutoPruneWhenEmpty(false)
				.setCanSplit(false));
		TabbedContent workTabs = builder.tabbed(
				Side.TOP,
				makeDockable(builder, 1, "Class1"),
				makeDockable(builder, 2, "Class2"),
				makeDockable(builder, 3, "Class3").withClosable(false),
				makeDockable(builder, 4, "Class4"),
				makeDockable(builder, 5, "Class5")
		);
		SplitContentLayout topSplit = builder.split(new SplitContentLayoutArgs()
				.setOrientation(Orientation.HORIZONTAL)
				.addChildren(
						builder.fitLeaf(toolTabs),
						builder.leaf(workTabs)
				)
				.setChildrenSizes(200)
		);
		LeafContentLayout bottomLeaf = builder.fitLeaf(builder.tabbed(new TabbedContentArgs()
				.setSide(Side.BOTTOM)
				.addDockables(
						buildDockable(builder, 1, "Logging").withClosable(false).withDragGroup(TOOLS),
						buildDockable(builder, 2, "Terminal").withClosable(false).withDragGroup(TOOLS),
						buildDockable(builder, 3, "Problems").withClosable(false).withDragGroup(TOOLS)
				)
				.setMenuFactory(c -> {
					ContextMenu menu = new ContextMenu();
					addSideItems(menu, c);
					return menu;
				})
				.setAutoPruneWhenEmpty(false)
				.setCanSplit(false))
		);
		SplitContentLayout layout = builder.split(new SplitContentLayoutArgs()
				.setOrientation(Orientation.VERTICAL)
				.addChildren(topSplit, bottomLeaf)
				.setChildrenPercentages(-1, 0.2) // The negative first value lets us specifically only configure the 2nd child
		);
		RootContentLayout root = builder.root(layout);
		Scene scene = new Scene(root.getBackingRegion());
		scene.getStylesheets().add(Bento.getCssPath());
		stage.setScene(scene);
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
				.withNode(content)
				.withCachedContextMenu(true)
				.withContextMenuFactory(dockable -> new ContextMenu(
						new MenuItem("Menu for : " + dockable.titleProperty().get()),
						new SeparatorMenuItem(),
						new MenuItem("Stuff")));
	}

	@Nonnull
	private static Circle makeIcon(int i) {
		final int radius = 6;
		Circle icon = switch (i) {
			case 1 -> new Circle(radius, Color.RED);
			case 2 -> new Circle(radius, Color.ORANGE);
			case 3 -> new Circle(radius, Color.LIME);
			case 4 -> new Circle(radius, Color.CYAN);
			case 5 -> new Circle(radius, Color.BLUE);
			case 6 -> new Circle(radius, Color.PURPLE);
			default -> new Circle(radius, Color.GREY);
		};
		icon.setEffect(new InnerShadow(BlurType.ONE_PASS_BOX, Color.BLACK, 2F, 10F, 0, 0));
		return icon;
	}

	private static void addSideItems(@Nonnull ContextMenu menu, @Nonnull TabbedContent tabbedContent) {
		for (Side side : Side.values()) {
			Label graphic = new Label(side == tabbedContent.sideProperty().get() ? "✓" : " ");
			MenuItem item = new MenuItem(side.name(), graphic);
			item.setOnAction(ignored -> tabbedContent.sideProperty().set(side));
			menu.getItems().add(item);
		}
	}
}
