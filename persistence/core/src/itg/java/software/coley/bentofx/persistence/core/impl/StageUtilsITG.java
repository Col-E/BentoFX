package software.coley.bentofx.persistence.core.impl;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ApplicationExtension.class)
class StageUtilsITG {

	private static final String FIRST_STAGE_TITLE = "first";
	private static final String SECOND_STAGE_TITLE = "second";

	private @Nullable Stage first;
	private @Nullable Stage second;
	private @Nullable Popup popup;

	@Start
	@SuppressWarnings("unused") // Called by ApplicationExtension
	private void start(Stage ignored) {
		first = new Stage();
		first.setTitle(FIRST_STAGE_TITLE);
		first.setScene(new Scene(new Label("one"), 100, 50));
		first.show();

		second = new Stage();
		second.setTitle(SECOND_STAGE_TITLE);
		second.setScene(new Scene(new Label("two"), 100, 50));
		second.show();

		popup = new Popup();
		popup.getContent().add(new Label("popup"));
		popup.show(first);
	}


	@Test
	void getAllStagesReturnsOnlyJavaFxStages(final FxRobot robot) {
		final Stage firstStage = getFirstStage();
		final Stage secondStage = getSecondStage();
		final Popup activePopup = getPopup();
		final List<Stage> stages = StageUtils.getAllStages();

		assertThat(stages)
				.describedAs("stages")
				.contains(firstStage, secondStage);
		assertThat(stages.stream().map(Stage::getTitle))
				.describedAs("stages.stream().map(Stage::getTitle)")
				.containsOnly(FIRST_STAGE_TITLE, SECOND_STAGE_TITLE);
		assertThat(Window.getWindows())
				.describedAs("Window.getWindows()")
				.contains(activePopup);

		robot.interact(() -> {
			activePopup.hide();
			secondStage.hide();
			firstStage.hide();
		});
	}

	/**
	 * Tests a position already on screen comes back untouched, and one off either
	 * edge is pulled back to the boundary.
	 *
	 * <p>Expectations are derived from {@link Screen} rather than hard-coded, so
	 * this holds on any monitor arrangement. </p>
	 */
	@Test
	void positionHelpersClampToTheBoundaryEnclosingEveryScreen(
			final FxRobot robot
	) {
		final Stage stage = getFirstStage();
		final Rectangle2D screens = boundaryEnclosingEveryScreen();
		final double farOutside = 10_000;

		final double onScreenX = screens.getMinX() + 10;
		assertThat(StageUtils.getXInScreenBounds(stage, onScreenX))
				.describedAs("getXInScreenBounds(stage, a position already on screen)")
				.isEqualTo(onScreenX);
		assertThat(StageUtils.getXInScreenBounds(stage, screens.getMinX() - farOutside))
				.describedAs("getXInScreenBounds(stage, a position left of every screen)")
				.isEqualTo(screens.getMinX());
		assertThat(StageUtils.getXInScreenBounds(stage, screens.getMaxX() + farOutside))
				.describedAs("getXInScreenBounds(stage, a position right of every screen)")
				.isEqualTo(screens.getMaxX() - stage.getWidth());

		final double onScreenY = screens.getMinY() + 10;
		assertThat(StageUtils.getYInScreenBounds(stage, onScreenY))
				.describedAs("getYInScreenBounds(stage, a position already on screen)")
				.isEqualTo(onScreenY);
		assertThat(StageUtils.getYInScreenBounds(stage, screens.getMinY() - farOutside))
				.describedAs("getYInScreenBounds(stage, a position above every screen)")
				.isEqualTo(screens.getMinY());
		assertThat(StageUtils.getYInScreenBounds(stage, screens.getMaxY() + farOutside))
				.describedAs("getYInScreenBounds(stage, a position below every screen)")
				.isEqualTo(screens.getMaxY() - stage.getHeight());

		robot.interact(() -> {
			getPopup().hide();
			getSecondStage().hide();
			stage.hide();
		});
	}

	private static Rectangle2D boundaryEnclosingEveryScreen() {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (final Screen screen : Screen.getScreens()) {
			final Rectangle2D bounds = screen.getVisualBounds();
			minX = Math.min(bounds.getMinX(), minX);
			minY = Math.min(bounds.getMinY(), minY);
			maxX = Math.max(bounds.getMaxX(), maxX);
			maxY = Math.max(bounds.getMaxY(), maxY);
		}

		return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
	}

	private Stage getFirstStage() {
		return requireNonNull(first, "first");
	}

	private Stage getSecondStage() {
		return requireNonNull(second, "second");
	}

	private Popup getPopup() {
		return requireNonNull(popup, "popup");
	}
}
