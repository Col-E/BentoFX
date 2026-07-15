package software.coley.bentofx.control;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import org.jspecify.annotations.Nullable;
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
	private final ChangeListener<Node> focusOwnerListener;
	private @Nullable Node lastContentFocusOwner;
	private @Nullable Header lastFocusedHeader;
	private long focusGeneration;
	private @Nullable Headers headers;

	/**
	 * @param container
	 * 		Parent container.
	 */
	public HeaderPane(DockContainerLeaf container) {
		this.container = container;
		this.contentWrapper = container.getBento().controlsBuilding().newContentWrapper(container);
		this.focusOwnerListener = (ob, old, cur) -> {
			if (isContentNode(cur)) {
				lastContentFocusOwner = cur;
				lastFocusedHeader = null;
			} else if (!isHeaderNode(cur)) {
				lastFocusedHeader = null;
			}
		};

		getStyleClass().add("header-pane");
		setAccessibleRole(AccessibleRole.TAB_PANE);

		// Track that this view has focus somewhere in the hierarchy.
		// This will allow us to style the active view's subclasses specially.
		container.focusWithinProperty().addListener((ob, old, cur) -> pseudoClassStateChanged(PSEUDO_ACTIVE, cur));

		// Track the last focused node in the currently displayed dockable's content. JavaFX's default
		// focus traversal can move focus from a content control to an unrelated header, so this gives
		// us a focus target to restore when that happens.
		sceneProperty().addListener((ob, old, cur) -> {
			if (old != null)
				old.focusOwnerProperty().removeListener(focusOwnerListener);
			lastContentFocusOwner = null;
			lastFocusedHeader = null;
			focusGeneration++;
			if (cur != null)
				cur.focusOwnerProperty().addListener(focusOwnerListener);
		});

		// Setup layout + observers to handle layout updates
		recomputeLayout(container.getSide());
		container.sideProperty().addListener((ob, old, cur) -> recomputeLayout(cur));
		container.selectedDockableProperty().addListener((ob, old, cur) -> {
			lastContentFocusOwner = null;
			focusGeneration++;

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

	/**
	 * Restore focus to the last focused node in the current dockable's content.
	 *
	 * @return {@code true} when a valid focus target was found and focus was requested.
	 */
	public boolean restoreContentFocus() {
		Node focusTarget = findContentFocusTarget();
		if (focusTarget == null)
			return false;
		long restoreGeneration = focusGeneration;

		// Let JavaFX finish its current traversal operation before restoring focus. Otherwise
		// the traversal engine may re-apply the header focus after this listener returns.
		focusTarget.requestFocus();
		restoreContentFocus(focusTarget, restoreGeneration, 4);
		return true;
	}

	/**
	 * @return {@code true} when the current focus transition originated from another header.
	 */
	public boolean isHeaderFocusOrigin() {
		return lastFocusedHeader != null;
	}

	/**
	 * Mark that the given header is the last focused header.
	 *
	 * @param header
	 * 		The header that was last focused.
	 */
	public void markHeaderFocused(Header header) {
		lastFocusedHeader = header;
	}

	/**
	 * Restore focus to the given target node in the content area.
	 *
	 * @param focusTarget
	 * 		The node to restore focus to.
	 * @param restoreGeneration
	 * 		The generation of the focus restoration request.
	 * @param attemptsRemaining
	 * 		The number of attempts remaining to restore focus.
	 * 		If the focus restoration fails, this method will be called again with one less attempt.
	 */
	private void restoreContentFocus(Node focusTarget, long restoreGeneration, int attemptsRemaining) {
		Platform.runLater(() -> {
			if (focusGeneration != restoreGeneration || !isFocusableContentNode(focusTarget))
				return;
			focusTarget.requestFocus();

			// We may have to try multiple times to restore focus.
			// If the focus restoration fails, we will try again with one less attempt.
			if (!focusTarget.isFocused() && attemptsRemaining > 0)
				restoreContentFocus(focusTarget, restoreGeneration, attemptsRemaining - 1);
		});
	}

	/**
	 * @return The last focused node in the content area,
	 * or a focusable descendant of the current content if the last focused node is no longer valid.
	 * If neither can be found, then {@code null}.
	 */
	@Nullable
	private Node findContentFocusTarget() {
		// First check if the last focused node is still valid and focusable.
		if (isFocusableContentNode(lastContentFocusOwner))
			return lastContentFocusOwner;

		// If not, then find a focusable descendant of the current content.
		return findFocusableDescendant(contentWrapper.getCenter());
	}

	@Nullable
	private Node findFocusableDescendant(@Nullable Node node) {
		if (node == null)
			return null;

		// Check if the node itself is focusable and valid.
		if (isFocusableContentNode(node))
			return node;

		// Check children for focusable descendants.
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				Node focusTarget = findFocusableDescendant(child);
				if (focusTarget != null)
					return focusTarget;
			}
		}
		return null;
	}

	private boolean isFocusableContentNode(@Nullable Node node) {
		// Sanity checks:
		//  - Must be a content node (not a header)
		//  - Must be in the same scene as this pane
		//  - Must be focus traversable, visible, and not disabled
		if (!isContentNode(node)
				|| node.getScene() != getScene()
				|| !node.isFocusTraversable()
				|| !node.isVisible()
				|| node.isDisabled())
			return false;

		// Must be able to walk up from the node to the content wrapper without hitting a disabled or invisible node.
		for (Node current = node; current != null; current = current.getParent()) {
			if (!current.isVisible() || current.isDisabled())
				return false;
			if (current == contentWrapper)
				return true;
		}

		return false;
	}

	/**
	 * @param node
	 * 		Node to check.
	 *
	 * @return {@code true} when the given node is a child of the {@link #contentWrapper}.
	 */
	private boolean isContentNode(@Nullable Node node) {
		if (node == null)
			return false;
		for (Node current = node; current != null; current = current.getParent())
			if (current == contentWrapper)
				return true;
		return false;
	}

	/**
	 * @param node
	 * 		Node to check.
	 *
	 * @return {@code true} when the given node is a child of the {@link #headers}.
	 */
	private boolean isHeaderNode(@Nullable Node node) {
		return node != null && headers != null && headers.getChildren().contains(node);
	}

	/**
	 * Recompute the layout of this pane based on the given side.
	 *
	 * @param side
	 * 		The side to place the headers on, or {@code null} to remove headers entirely.
	 */
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
	protected Node[] createButtonArray() {
		Button dockableListButton = createDockableListButton();
		Button containerConfigButton = createContainerConfigButton();
		return new Node[]{dockableListButton, containerConfigButton};
	}

	/**
	 * @return New button that displays all dockables in this space.
	 */
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
	protected Button createContainerConfigButton() {
		Button button = new Button("≡");
		button.setEllipsisString("≡");
		button.getStyleClass().addAll("corner-button", "context-button");
		button.setOnMousePressed(e -> button.setContextMenu(container.buildContextMenu()));
		button.setOnMouseClicked(e -> {
			ContextMenu menu = button.getContextMenu();
			if (menu != null)
				menu.show(button, e.getScreenX(), e.getScreenY());
		});
		button.visibleProperty().bind(container.menuFactoryProperty().isNotNull());
		button.managedProperty().bind(button.visibleProperty());
		return button;
	}

	private Header createHeader(Dockable dockable) {
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
	public DockContainerLeaf getContainer() {
		return container;
	}

	/**
	 * @return The border-pane that holds the currently selected {@link Dockable#getNode()}.
	 */
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
	private Bento getBento() {
		return container.getBento();
	}
}
