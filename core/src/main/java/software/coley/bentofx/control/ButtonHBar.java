package software.coley.bentofx.control;

import jakarta.annotation.Nonnull;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import software.coley.bentofx.layout.container.DockContainerLeaf;

import static software.coley.bentofx.util.BentoStates.PSEUDO_ORIENTATION_H;

/**
 * {@link HBox} for {@link DockContainerLeaf} level controls in a {@link HeaderPane}.
 *
 * @author Matt Coley
 */
public class ButtonHBar extends HBox {
	/**
	 * @param parent
	 * 		Parent region to bind child height to.
	 * @param children
	 * 		Children to add to this box.
	 */
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
