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
import software.coley.bentofx.builder.DockableBuilder;
import software.coley.bentofx.builder.LayoutBuilder;
import software.coley.bentofx.builder.SplitLayoutArgs;
import software.coley.bentofx.builder.TabbedSpaceArgs;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.LeafDockLayout;
import software.coley.bentofx.layout.RootDockLayout;
import software.coley.bentofx.layout.SplitDockLayout;
import software.coley.bentofx.space.TabbedDockSpace;

public class BoxApp extends Application {
	private static final int TOOLS = 1;

	@Override
	public void start(Stage stage) {
		stage.setWidth(1000);
		stage.setHeight(700);

		// TODO: Allow making dockables configure their containing DockableDestination/DockLayout when splitting/new-windowing
		//  - For example, tool tabs in new windows making their TabbedDockSpace#canSplitProperty = false
		//  - Can kinda do it with dockable#spaceProperty() listener

		Bento bento = Bento.newBento();
		bento.setEmptyDisplayFactory(parentLayout -> new Label("You closed all the classes"));
		bento.addDockableOpenListener((path, dockable) -> System.out.println("Opened: " + dockable.getTitle()));
		bento.addDockableMoveListener((oldPath, newPath, dockable) -> System.out.println("Moved: " + dockable.getTitle()));
		bento.addDockableCloseListener((path, dockable) -> System.out.println("Closed: " + dockable.getTitle()));
		bento.addDockableSelectListener((path, dockable) -> System.out.println("Select: " + dockable.getTitle()));

		LayoutBuilder builder = bento.newLayoutBuilder();

		TabbedDockSpace toolTabs = builder.tabbed(new TabbedSpaceArgs()
				.setIdentifier("left-tool-tabs")
				.setSide(Side.LEFT)
				.addDockables(
						buildDockable(builder, 1, "Workspace").withClosable(false).withDragGroup(TOOLS),
						buildDockable(builder, 2, "Bookmarks").withClosable(false).withDragGroup(TOOLS),
						buildDockable(builder, 3, "Modifications").withClosable(false).withDragGroup(TOOLS)
				)
				.setAutoPruneWhenEmpty(false)
				.setCanSplit(false));
		TabbedDockSpace workTabs = builder.tabbed(
				new TabbedSpaceArgs().setSide(Side.TOP)
						.setIdentifier("class-tabs")
						.setAutoPruneWhenEmpty(false)
						.addDockables(
								makeDockable(builder, 1, "Class1"),
								makeDockable(builder, 2, "Class2"),
								makeDockable(builder, 3, "Class3"),
								makeDockable(builder, 4, "Class4"),
								makeDockable(builder, 5, "Class5")
						)
		);
		SplitDockLayout topSplit = builder.split(new SplitLayoutArgs()
				.setOrientation(Orientation.HORIZONTAL)
				.addChildren(
						builder.fitLeaf(toolTabs),
						builder.leaf(workTabs)
				)
				.setChildrenSizes(200)
		);
		LeafDockLayout bottomLeaf = builder.fitLeaf(builder.tabbed(new TabbedSpaceArgs()
				.setIdentifier("bottom-tool-tabs")
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
		SplitDockLayout layout = builder.split(new SplitLayoutArgs()
				.setOrientation(Orientation.VERTICAL)
				.addChildren(topSplit, bottomLeaf)
				.setChildrenPercentages(-1, 0.2) // The negative first value lets us specifically only configure the 2nd child
		);
		RootDockLayout root = builder.root(layout);
		Scene scene = new Scene(root.getBackingRegion());
		scene.getStylesheets().add(Bento.getCssPath());
		stage.setScene(scene);
		stage.setOnHidden(e -> System.exit(0));
		stage.show();
	}

	@Nonnull
	private Dockable makeDockable(@Nonnull LayoutBuilder builder, int i, @Nonnull String title) {
		return buildDockable(builder, i, title).build();
	}

	@Nonnull
	private DockableBuilder buildDockable(@Nonnull LayoutBuilder builder, int i, @Nonnull String title) {
		Label label = new Label("<" + title + ":" + i + ">");
		label.setFocusTraversable(true); // Set to facilitate detection of 'focusWithinProperty()' in TabbedDockSpace implementations
		return builder.dockable()
				.withTitle(title)
				.withIconFactory(dockable -> makeIcon(i))
				.withNode(label)
				.withCachedContextMenu(true)
				.withContextMenuFactory(dockable -> new ContextMenu(
						new MenuItem("Menu for : " + dockable.getTitle()),
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

	private static void addSideItems(@Nonnull ContextMenu menu, @Nonnull TabbedDockSpace tabbed) {
		for (Side side : Side.values()) {
			Label graphic = new Label(side == tabbed.sideProperty().get() ? "✓" : " ");
			MenuItem item = new MenuItem(side.name(), graphic);
			item.setOnAction(ignored -> tabbed.sideProperty().set(side));
			menu.getItems().add(item);
		}
	}
}
