package software.coley.bentofx.impl.content;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public abstract class ImplContentBase extends StackPane {
	protected final BorderPane layout = new BorderPane();
	protected final Canvas canvas = new Canvas();

	protected ImplContentBase() {
		// Fit the canvas to the container size
		canvas.setManaged(false);
		canvas.setMouseTransparent(true);
		canvas.widthProperty().bind(widthProperty());
		canvas.heightProperty().bind(heightProperty());

		// Put canvas on top
		getChildren().addAll(layout, canvas);
	}
}
