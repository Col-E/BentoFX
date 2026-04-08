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
import javafx.scene.layout.BorderPane;
import software.coley.bentofx.Bento;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerLeafMenuFactory;
import software.coley.bentofx.util.BentoUtils;

import static software.coley.bentofx.util.BentoStates.*;

/**
 * Basically just a re-implementation of a {@link TabPane} except for {@link Dockable}.
 *
 * @author Matt Coley
 */
public class HeaderPane extends BorderPane {
	private final DockContainerLeaf container;
	private final ContentWrapper contentWrapper;
	private Headers headers;

	/**
	 * @param container
	 * 		Parent container.
	 */
	public HeaderPane(@Nonnull DockContainerLeaf container) {
		this.container = container;
		this.contentWrapper = container.getBento().controlsBuilding().newContentWrapper(container);

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
							.map(this::createHeader)
							.toList());
				} else if (c.wasRemoved()) {
					headerList.subList(c.getFrom(), c.getFrom() + c.getRemovedSize()).clear();
				} else if (c.wasAdded()) {
					headerList.addAll(c.getFrom(), c.getAddedSubList().stream()
							.map(this::createHeader)
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
		headers = getBento().controlsBuilding().newHeaders(container, BentoUtils.sideToOrientation(side), side);
		BorderPane headersWrapper = new BorderPane(headers);
		headersWrapper.getStyleClass().add("header-region-wrapper");
		if (BentoUtils.sideToOrientation(side) == Orientation.HORIZONTAL) {
			headersWrapper.setRight(new ButtonHBar(headers, createButtonArray()));
		} else {
			headersWrapper.setBottom(new ButtonVBar(headers, createButtonArray()));
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
					Header header = createHeader(d);
					if (container.getSelectedDockable() == d)
						header.setSelected(true);
					return header;
				})
				.forEach(headers::add);
	}

	/**
	 * @return Array of buttons to show in the corner of the headers region.
	 *
	 * @see #createDockableListButton()
	 * @see #createContainerConfigButton()
	 */
	@Nonnull
	protected Node[] createButtonArray() {
		Button dockableListButton = createDockableListButton();
		Button containerConfigButton = createContainerConfigButton();
		return new Node[]{dockableListButton, containerConfigButton};
	}

	/**
	 * @return New button that displays all dockables in this space.
	 */
	@Nonnull
	protected Button createDockableListButton() {
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

	/**
	 * @return New button that displays a user-defined menu.
	 *
	 * @see DockContainerLeaf#setMenuFactory(DockContainerLeafMenuFactory)
	 */
	@Nonnull
	protected Button createContainerConfigButton() {
		Button button = new Button("≡");
		button.setEllipsisString("≡");
		button.getStyleClass().addAll("corner-button", "context-button");
		button.setOnMousePressed(e -> button.setContextMenu(container.buildContextMenu()));
		button.setOnMouseClicked(e -> button.getContextMenu().show(button, e.getScreenX(), e.getScreenY()));
		button.visibleProperty().bind(container.menuFactoryProperty().isNotNull());
		button.managedProperty().bind(button.visibleProperty());
		return button;
	}

	@Nonnull
	private Header createHeader(@Nonnull Dockable dockable) {
		return getBento().controlsBuilding().newHeader(dockable, this);
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
}
