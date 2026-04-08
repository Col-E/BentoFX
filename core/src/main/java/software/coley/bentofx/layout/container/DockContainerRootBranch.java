package software.coley.bentofx.layout.container;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import software.coley.bentofx.Bento;
import software.coley.bentofx.control.DragDropStage;
import software.coley.bentofx.path.DockContainerPath;

import java.util.Collections;

import static software.coley.bentofx.util.BentoStates.PSEUDO_ROOT;

/**
 * Root branch container.
 *
 * @author Matt Coley
 */
public class DockContainerRootBranch extends DockContainerBranch {
	private final DockContainerPath path = new DockContainerPath(Collections.singletonList(this));

	/**
	 * @param bento
	 * 		Parent bento instance.
	 * @param identifier
	 * 		This container's identifier.
	 */
	public DockContainerRootBranch(@Nonnull Bento bento, @Nonnull String identifier) {
		super(bento, identifier);

		pseudoClassStateChanged(PSEUDO_ROOT, true);

		sceneProperty().addListener((on, old, cur) -> {
			if (cur != null) {
				bento.registerRoot(this);
			} else {
				bento.unregisterRoot(this);
			}
		});
	}

	@Override
	public boolean removeFromParent() {
		Region thisAsRegion = asRegion();
		Scene scene = thisAsRegion.getScene();
		if (scene != null
				&& scene.getRoot() == thisAsRegion
				&& scene.getWindow() instanceof DragDropStage ddStage
				&& ddStage.isAutoCloseWhenEmpty()) {
			ddStage.close();
			return true;
		}
		return false;
	}

	@Nullable
	@Override
	public DockContainerBranch getParentContainer() {
		return null;
	}

	@Override
	public void setParentContainer(@Nonnull DockContainerBranch parent) {
		throw new IllegalStateException("Root should not have a parent container assigned");
	}


	@Nonnull
	@Override
	public DockContainerPath getPath() {
		return path;
	}
}
