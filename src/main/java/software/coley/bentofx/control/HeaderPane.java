package software.coley.bentofx.control;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import software.coley.bentofx.Bento;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.util.BentoUtils;
import software.coley.bentofx.util.DragDropTarget;
import software.coley.bentofx.util.DragUtils;

import java.util.Objects;

import static software.coley.bentofx.util.BentoStates.*;

/**
 * Basically just a re-implementation of a {@link TabPane} except for {@link Dockable}.
 *
 * @author Matt Coley
 */
public class HeaderPane extends BorderPane {
	private final DockContainerLeaf container;
	private final ContentWrapper contentWrapper = new ContentWrapper();
	private Headers headers;

	/**
	 * @param container
	 * 		Parent container.
	 */
	public HeaderPane(@Nonnull DockContainerLeaf container) {
		this.container = container;

		getStyleClass().add("header-pane");
		setAccessibleRole(AccessibleRole.TAB_PANE);

		// Track that this view has focus somewhere in the hierarchy.
		// This will allow us to style the active view's subclasses specially.
		container.focusWithinProperty().addListener((ob, old, cur) -> pseudoClassStateChanged(PSEUDO_ACTIVE, cur));

		// Setup layout + observers to handle layout updates
		recomputeLayout(container.getSide());
		container.sideProperty().addListener((ob, old, cur) -> recomputeLayout(cur));
		container.selectedDockableProperty().addListener((ob, old, cur) -> {
			Header oldSelectedHeader = getHeader(old);
			Header newSelectedHeader = getHeader(cur);

			if (oldSelectedHeader != null) oldSelectedHeader.setSelected(false);
			if (newSelectedHeader != null) newSelectedHeader.setSelected(true);

			if (cur != null) {
				// We need to ensure that the dockable's prior containing display unbinds it as a child.
				//   - https://bugs.openjdk.org/browse/JDK-8137251
				//   - This control will unbind its prior value when we tell it to bind the new value
				ObjectProperty<Node> dockableNodeProperty = cur.nodeProperty();
				if (dockableNodeProperty.get() != null && dockableNodeProperty.get().getParent() instanceof BorderPane oldContentWrapper)
					oldContentWrapper.centerProperty().unbind();

				// Rebind to display newly selected dockable's content.
				contentWrapper.centerProperty().unbind();
				contentWrapper.centerProperty().bind(dockableNodeProperty
						.map(display -> display != null ? display : getBento().placeholderBuilding().build(cur)));
			} else {
				// No current content, fill in with a placeholder (unless collapsed).
				contentWrapper.centerProperty().unbind();
				contentWrapper.setCenter(container.isCollapsed() ? null : getBento().placeholderBuilding().build(container));
			}
		});
		container.getDockables().addListener((ListChangeListener<Dockable>) c -> {
			ObservableList<Node> headerList = headers.getChildren();
			while (c.next()) {
				if (c.wasPermutated()) {
					headerList.subList(c.getFrom(), c.getTo()).clear();
					headerList.addAll(c.getFrom(), c.getList().subList(c.getFrom(), c.getTo()).stream()
							.map(d -> new Header(d, this).withEvents())
							.toList());
				} else if (c.wasRemoved()) {
					headerList.subList(c.getFrom(), c.getFrom() + c.getRemovedSize()).clear();
				} else if (c.wasAdded()) {
					headerList.addAll(c.getFrom(), c.getAddedSubList().stream()
							.map(d -> new Header(d, this).withEvents())
							.toList());
				}
			}
		});

		BooleanBinding notCollapsed = container.collapsedProperty().not();
		contentWrapper.visibleProperty().bind(notCollapsed);
		contentWrapper.managedProperty().bind(notCollapsed);
		setCenter(contentWrapper);
	}

	private void recomputeLayout(@Nullable Side side) {
		// Clear CSS state
		pseudoClassStateChanged(PSEUDO_SIDE_TOP, false);
		pseudoClassStateChanged(PSEUDO_SIDE_BOTTOM, false);
		pseudoClassStateChanged(PSEUDO_SIDE_LEFT, false);
		pseudoClassStateChanged(PSEUDO_SIDE_RIGHT, false);

		// Clear edge nodes
		setTop(null);
		setBottom(null);
		setLeft(null);
		setRight(null);

		// Skip populating headers if there is no side specified.
		//  - Yes, this also means no container-config button
		if (side == null)
			return;

		// Update CSS state and edge node to display our headers + controls aligned to the given side.
		headers = new Headers(BentoUtils.sideToOrientation(side), side);
		Button dockableListButton = createDockableListButton();
		Button containerConfigButton = createContainerConfigButton();
		BorderPane headersWrapper = new BorderPane(headers);
		headersWrapper.getStyleClass().add("header-region-wrapper");
		if (BentoUtils.sideToOrientation(side) == Orientation.HORIZONTAL) {
			headersWrapper.setRight(new ButtonHBar(headers, dockableListButton, containerConfigButton));
		} else {
			headersWrapper.setBottom(new ButtonVBar(headers, dockableListButton, containerConfigButton));
		}
		switch (side) {
			case TOP -> {
				setTop(headersWrapper);
				pseudoClassStateChanged(PSEUDO_SIDE_TOP, true);
			}
			case BOTTOM -> {
				setBottom(headersWrapper);
				pseudoClassStateChanged(PSEUDO_SIDE_BOTTOM, true);
			}
			case LEFT -> {
				setLeft(headersWrapper);
				pseudoClassStateChanged(PSEUDO_SIDE_LEFT, true);
			}
			case RIGHT -> {
				setRight(headersWrapper);
				pseudoClassStateChanged(PSEUDO_SIDE_RIGHT, true);
			}
		}

		// Add all dockables to the headers display
		container.getDockables().stream()
				.map(d -> {
					Header header = new Header(d, this).withEvents();
					if (container.getSelectedDockable() == d)
						header.setSelected(true);
					return header;
				})
				.forEach(headers::add);
	}

	@Nonnull
	private Button createDockableListButton() {
		Button button = new Button("▼");
		button.setEllipsisString("▼");
		button.getStyleClass().addAll("corner-button", "list-button");
		button.setOnMousePressed(e -> {
			// TODO: A name filter that appears when you begin to type would be nice
			ContextMenu menu = new ContextMenu();
			menu.getItems().addAll(container.getDockables().stream().map(d -> {
				MenuItem item = new MenuItem();
				item.textProperty().bind(d.titleProperty());
				item.graphicProperty().bind(d.iconFactoryProperty().map(ic -> ic.build(d)));
				item.setOnAction(ignored -> container.selectDockable(d));
				return item;
			}).toList());
			button.setContextMenu(menu);
		});
		button.setOnMouseClicked(e -> button.getContextMenu().show(button, e.getScreenX(), e.getScreenY()));
		button.visibleProperty().bind(headers.overflowingProperty());
		button.managedProperty().bind(button.visibleProperty());
		return button;
	}

	@Nonnull
	private Button createContainerConfigButton() {
		Button button = new Button("≡");
		button.setEllipsisString("≡");
		button.getStyleClass().addAll("corner-button", "context-button");
		button.setOnMousePressed(e -> button.setContextMenu(container.buildContextMenu()));
		button.setOnMouseClicked(e -> button.getContextMenu().show(button, e.getScreenX(), e.getScreenY()));
		button.visibleProperty().bind(container.menuFactoryProperty().isNotNull());
		button.managedProperty().bind(button.visibleProperty());
		return button;
	}

	/**
	 * @param dockable
	 * 		Some dockable.
	 *
	 * @return Associated header within this pane that represents the given dockable.
	 */
	@Nullable
	public Header getHeader(@Nullable Dockable dockable) {
		if (dockable == null)
			return null;
		for (Node child : headers.getChildren())
			if (child instanceof Header header && header.getDockable() == dockable)
				return header;
		return null;
	}

	/**
	 * @return Parent container.
	 */
	@Nonnull
	public DockContainerLeaf getContainer() {
		return container;
	}

	/**
	 * @return The border-pane that holds the currently selected {@link Dockable#getNode()}.
	 */
	@Nonnull
	public ContentWrapper getContentWrapper() {
		return contentWrapper;
	}

	/**
	 * @return The linear-item-pane holding {@link Header} children.
	 */
	@Nullable
	public Headers getHeaders() {
		return headers;
	}

	/**
	 * @return Convenience call.
	 */
	@Nonnull
	private Bento getBento() {
		return container.getBento();
	}

	/**
	 * Linear item pane to hold {@link Header} displays of {@link DockContainerLeaf#getDockables()}.
	 */
	public class Headers extends LinearItemPane {
		/**
		 * @param orientation
		 * 		Which axis to layout children on.
		 * @param side
		 * 		Side in the parent container where tabs are displayed.
		 */
		private Headers(@Nonnull Orientation orientation, @Nonnull Side side) {
			super(orientation);

			// Create side-specific header region class.
			getStyleClass().add("header-region");
			switch (side) {
				case TOP -> pseudoClassStateChanged(PSEUDO_SIDE_TOP, true);
				case BOTTOM -> pseudoClassStateChanged(PSEUDO_SIDE_BOTTOM, true);
				case LEFT -> pseudoClassStateChanged(PSEUDO_SIDE_LEFT, true);
				case RIGHT -> pseudoClassStateChanged(PSEUDO_SIDE_RIGHT, true);
			}

			// Use a clip to prevent headers from rendering beyond expected bounds.
			// Currently, with the CSS in use this is not needed but in some cases it is.
			//    Rectangle clip = new Rectangle();
			//    clip.widthProperty().bind(widthProperty());
			//    clip.heightProperty().bind(heightProperty());
			//    setClip(clip);

			// Make this pane fill the full width/height (matching orientation) of the parent container.
			if (orientation == Orientation.HORIZONTAL) {
				prefWidthProperty().bind(container.widthProperty());
			} else {
				prefHeightProperty().bind(container.heightProperty());
			}

			// Make children fill the full width/height of this pane on the perpendicular (to orientation) axis.
			fitChildrenToPerpendicularProperty().set(true);

			// Keep the selected dockable in view.
			keepInViewProperty().bind(container.selectedDockableProperty().map(container::getHeader));

			// Support drag-drop.
			setOnDragOver(e -> {
				Dragboard dragboard = e.getDragboard();
				String dockableIdentifier = DragUtils.extractIdentifier(dragboard);
				if (dockableIdentifier != null) {
					DockablePath dragSourcePath = getBento().search().dockable(dockableIdentifier);
					if (dragSourcePath != null) {
						Dockable dragSourceDockable = dragSourcePath.dockable();
						if (container.canReceiveDockable(dragSourceDockable, null)) {
							container.drawCanvasHint(this);
						} else {
							container.clearCanvas();
						}
					}

					// We always need to accept content if there is a dockable identifier.
					// In the case where it is not actually receivable, we'll handle that in the completion logic.
					e.acceptTransferModes(TransferMode.MOVE);
				}

				// Do not propagate upwards.
				e.consume();
			});
			setOnDragDropped(e -> {
				// Skip if dragboard doesn't contain a dockable identifier.
				Dragboard dragboard = e.getDragboard();
				String dockableIdentifier = DragUtils.extractIdentifier(dragboard);
				if (dockableIdentifier == null)
					return;

				// Skip if the dockable cannot be found in our bento instance.
				DockablePath dragSourcePath = getBento().search().dockable(dockableIdentifier);
				if (dragSourcePath == null)
					return;

				// If our container can receive the header, move it over.
				DockContainerLeaf sourceContainer = dragSourcePath.leafContainer();
				Dockable sourceDockable = dragSourcePath.dockable();
				if (container.canReceiveDockable(sourceDockable, null)) {
					sourceContainer.removeDockable(sourceDockable);
					container.addDockable(sourceDockable);
					container.selectDockable(sourceDockable);
					DragUtils.completeDnd(e, sourceDockable, DragDropTarget.REGION);
				}
			});
			setOnDragExited(e -> container.clearCanvas());
		}
	}

	/**
	 * Border pane with handling for drag-drop in the context of this header pane's parent container.
	 */
	public class ContentWrapper extends BorderPane {
		public ContentWrapper() {
			getStyleClass().add("node-wrapper");

			// Handle drag-drop
			setOnDragOver(e -> {
				Dragboard dragboard = e.getDragboard();
				String dockableIdentifier = DragUtils.extractIdentifier(dragboard);
				if (dockableIdentifier != null) {
					DockablePath dragSourcePath = getBento().search().dockable(dockableIdentifier);
					if (dragSourcePath != null) {
						Dockable dragSourceDockable = dragSourcePath.dockable();
						Side side = container.isCanSplit() ? BentoUtils.computeClosestSide(this, e.getX(), e.getY()) : null;
						if (container.canReceiveDockable(dragSourceDockable, side)) {
							container.drawCanvasHint(this, side);
						} else {
							container.clearCanvas();
						}
					}

					// We always need to accept content if there is a dockable identifier.
					// In the case where it is not actually receivable, we'll handle that in the completion logic.
					e.acceptTransferModes(TransferMode.MOVE);
				}

				// Do not propagate upwards.
				e.consume();
			});
			setOnDragExited(e -> container.clearCanvas());
			setOnDragDropped(e -> {
				// Skip if dragboard doesn't contain a dockable identifier.
				Dragboard dragboard = e.getDragboard();
				String dockableIdentifier = DragUtils.extractIdentifier(dragboard);
				if (dockableIdentifier == null)
					return;

				// Skip if the dockable cannot be found in our bento instance.
				DockablePath dragSourcePath = getBento().search().dockable(dockableIdentifier);
				if (dragSourcePath == null)
					return;

				// Skip if this source/target containers are the same, and there is only one dockable.
				// This means there would be no change after the "move" and thus its wasted effort to do anything.
				DockContainerLeaf sourceContainer = dragSourcePath.leafContainer();
				Dockable sourceDockable = dragSourcePath.dockable();
				if (container == sourceContainer && container.getDockables().size() == 1)
					return;

				// If our container can receive the header, move it over.
				Side side = container.isCanSplit() ? BentoUtils.computeClosestSide(this, e.getX(), e.getY()) : null;
				if (container.canReceiveDockable(sourceDockable, side)) {
					// Disable empty pruning while we handle splitting.
					boolean pruneState = sourceContainer.doPruneWhenEmpty();
					sourceContainer.setPruneWhenEmpty(false);

					// Remove the dockable from its current parent.
					sourceContainer.removeDockable(sourceDockable);

					// Handle splitting by side if provided.
					if (side != null) {
						// Keep track of the current container's parent for later.
						DockContainerBranch ourParent = Objects.requireNonNull(container.getParentContainer());

						// Create container for dropped header.
						DockContainerLeaf containerForDropped = getBento().dockBuilding().leaf();
						containerForDropped.setSide(container.getSide()); // Copy our container's side-ness.
						containerForDropped.addDockable(sourceDockable);

						// Create container to hold both our own container and the dropped header.
						// This will combine them in a split view according to the side the user dropped
						// the incoming dockable on.
						DockContainerBranch splitContainer = getBento().dockBuilding().branch();
						if (side == Side.TOP || side == Side.BOTTOM)
							splitContainer.setOrientation(Orientation.VERTICAL);
						if (side == Side.TOP || side == Side.LEFT) {
							// User dropped on top/left, so the dropped item is first in the split.
							splitContainer.addContainer(containerForDropped);
							splitContainer.addContainer(container);
						} else {
							// User dropped on bottom/right, so the dropped item is last in the split.
							splitContainer.addContainer(container);
							splitContainer.addContainer(containerForDropped);
						}

						// Now we get the parent container (a branch) that holds our container (a leaf) and have it replace
						// the leaf it currently has (our current container) with the new branch container we just made.
						ourParent.replaceContainer(container, splitContainer);
					} else {
						// Just move the dockable from its prior container to our container.
						container.addDockable(sourceDockable);
						container.selectDockable(sourceDockable);
					}

					// Restore original prune state.
					sourceContainer.setPruneWhenEmpty(pruneState);
					if (sourceContainer.doPruneWhenEmpty() && sourceContainer.getDockables().isEmpty())
						sourceContainer.removeFromParent();

					DragUtils.completeDnd(e, sourceDockable, DragDropTarget.REGION);
				}
			});
		}
	}

	/**
	 * {@link HBox} for {@link DockContainerLeaf} level controls.
	 */
	private static class ButtonHBar extends HBox {
		public ButtonHBar(@Nonnull Region parent, Node... children) {
			getStyleClass().add("button-bar");
			pseudoClassStateChanged(PSEUDO_ORIENTATION_H, true);

			for (Node child : children) {
				if (child instanceof Region childRegion)
					childRegion.prefHeightProperty().bind(parent.heightProperty());
				getChildren().add(child);
			}
		}
	}

	/**
	 * {@link VBox} for {@link DockContainerLeaf} level controls.
	 */
	private static class ButtonVBar extends VBox {
		public ButtonVBar(@Nonnull Region parent, Node... children) {
			getStyleClass().add("button-bar");
			pseudoClassStateChanged(PSEUDO_ORIENTATION_V, true);

			for (Node child : children) {
				if (child instanceof Region childRegion)
					childRegion.prefWidthProperty().bind(parent.widthProperty());
				getChildren().add(child);
			}
		}
	}
}
