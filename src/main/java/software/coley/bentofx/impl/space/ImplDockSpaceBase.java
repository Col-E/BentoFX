package software.coley.bentofx.impl.space;

import jakarta.annotation.Nonnull;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import software.coley.bentofx.impl.ImplBento;

public abstract class ImplDockSpaceBase extends StackPane {
	protected final ImplBento bento;
	protected final BorderPane layout = new BorderPane();
	protected final Canvas canvas = new Canvas();

	protected ImplDockSpaceBase(@Nonnull ImplBento bento) {
		this.bento = bento;

		// Fit the canvas to the container size
		canvas.setManaged(false);
		canvas.setMouseTransparent(true);
		canvas.widthProperty().bind(widthProperty());
		canvas.heightProperty().bind(heightProperty());

		// Put canvas on top
		getChildren().addAll(layout, canvas);
	}
}
