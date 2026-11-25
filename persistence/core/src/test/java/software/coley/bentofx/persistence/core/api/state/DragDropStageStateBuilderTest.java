package software.coley.bentofx.persistence.core.api.state;

import javafx.geometry.Orientation;
import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState.DockContainerRootBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DragDropStageState.DragDropStageStateBuilder;

import static javafx.stage.Modality.NONE;
import static org.assertj.core.api.Assertions.assertThat;

class DragDropStageStateBuilderTest {

	private static final String STAGE_TITLE = "Detached";

	@Test
	void dragDropStageBuilderCapturesOptionalStageMetadata() {
		DockContainerRootBranchState rootState =
				new DockContainerRootBranchStateBuilder("root-1")
						.setOrientation(Orientation.HORIZONTAL)
						.build();

		DragDropStageState stageState =
				new DragDropStageStateBuilder(true)
						.setDockContainerRootBranchState(rootState)
						.setTitle(STAGE_TITLE)
						.setX(10.0)
						.setY(20.0)
						.setWidth(800.0)
						.setHeight(600.0)
						.setModality(NONE)
						.setOpacity(0.85)
						.setIconified(false)
						.setFullScreen(false)
						.setMaximized(true)
						.setAlwaysOnTop(true)
						.setResizable(true)
						.setShowing(true)
						.setFocused(false)
						.build();

		assertThat(stageState.isAutoClosedWhenEmpty())
				.describedAs("stageState.isAutoClosedWhenEmpty()")
				.isTrue();

		assertThat(stageState.getDockContainerRootBranchState())
				.describedAs("stageState.getDockContainerRootBranchState()")
				.contains(rootState);

		assertThat(stageState.getTitle())
				.describedAs("stageState.getTitle()")
				.contains(STAGE_TITLE);

		assertThat(stageState.getX())
				.describedAs("stageState.getX()")
				.contains(10.0);

		assertThat(stageState.getY())
				.describedAs("stageState.getY()")
				.contains(20.0);

		assertThat(stageState.getWidth())
				.describedAs("stageState.getWidth()")
				.contains(800.0);

		assertThat(stageState.getHeight())
				.describedAs("stageState.getHeight()")
				.contains(600.0);

		assertThat(stageState.getModality())
				.describedAs("stageState.getModality()")
				.contains(NONE);

		assertThat(stageState.getOpacity())
				.describedAs("stageState.getOpacity()")
				.contains(0.85);

		assertThat(stageState.isIconified())
				.describedAs("stageState.isIconified()")
				.contains(false);

		assertThat(stageState.isFullScreen())
				.describedAs("stageState.isFullScreen()")
				.contains(false);

		assertThat(stageState.isMaximized())
				.describedAs("stageState.isMaximized()")
				.contains(true);

		assertThat(stageState.isAlwaysOnTop())
				.describedAs("stageState.isAlwaysOnTop()")
				.contains(true);

		assertThat(stageState.isResizable())
				.describedAs("stageState.isResizable()")
				.contains(true);

		assertThat(stageState.isShowing())
				.describedAs("stageState.isShowing()")
				.contains(true);

		assertThat(stageState.isFocused())
				.describedAs("stageState.isFocused()")
				.contains(false);
	}
}
