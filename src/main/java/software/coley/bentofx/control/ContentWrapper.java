package software.coley.bentofx.control;

import jakarta.annotation.Nonnull;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import software.coley.bentofx.Bento;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.util.BentoUtils;
import software.coley.bentofx.util.DragDropTarget;
import software.coley.bentofx.util.DragUtils;

import java.util.Objects;

/**
 * Border pane with handling for drag-drop in the context of a {@link HeaderPane}'s parent {@link DockContainerLeaf}.
 *
 * @author Matt Coley
 */
public class ContentWrapper extends BorderPane {
	/**
	 * @param container
	 * 		Parent container.
	 */
	public ContentWrapper(@Nonnull DockContainerLeaf container) {
		getStyleClass().add("node-wrapper");

		// Handle drag-drop
		setupDragDrop(container);
	}

	protected void setupDragDrop(@Nonnull DockContainerLeaf container) {
		Bento bento = container.getBento();
		setOnDragOver(e -> {
			Dragboard dragboard = e.getDragboard();
			String dockableIdentifier = DragUtils.extractIdentifier(dragboard);
			if (dockableIdentifier != null) {
				DockablePath dragSourcePath = bento.search().dockable(dockableIdentifier);
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
			DockablePath dragSourcePath = bento.search().dockable(dockableIdentifier);
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
					DockContainerLeaf containerForDropped = bento.dockBuilding().leaf();
					containerForDropped.setSide(container.getSide()); // Copy our container's side-ness.
					containerForDropped.addDockable(sourceDockable);

					// Create container to hold both our own container and the dropped header.
					// This will combine them in a split view according to the side the user dropped
					// the incoming dockable on.
					DockContainerBranch splitContainer = bento.dockBuilding().branch();
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
