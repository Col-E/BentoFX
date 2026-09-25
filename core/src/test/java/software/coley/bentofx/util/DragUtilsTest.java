package software.coley.bentofx.util;

import javafx.scene.input.DataFormat;
import org.junit.jupiter.api.Test;
import software.coley.bentofx.Bento;
import software.coley.bentofx.dockable.Dockable;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage for the drag content {@link DragUtils} puts on a {@code Dragboard}.
 *
 * @author Phil Bryant
 */
class DragUtilsTest {

	private static final String IDENTIFIER = "dockable-1";
	private static final int DRAG_GROUP = 3;

	/**
	 * Regression test for plain text - dropping a dragged tab onto a text
	 * editor pasted the drag content into it.
	 */
	@Test
	void contentIsNotOfferedAsPlainText() {
		Map<DataFormat, Object> content =
				DragUtils.content(createDockable(), DragDropTarget.HEADER);

		assertThat(content)
				.describedAs("drag content offered to other applications")
				.doesNotContainKey(DataFormat.PLAIN_TEXT);
	}

	@Test
	void contentRoundTrips() {
		Map<DataFormat, Object> content =
				DragUtils.content(createDockable(), DragDropTarget.HEADER);

		assertThat(DragUtils.splitContent(content::get))
				.describedAs("content read back from what DragUtils wrote")
				.containsExactly(
						DragUtils.PREFIX.replace(";", ""),
						String.valueOf(DRAG_GROUP),
						IDENTIFIER,
						"HEADER"
				);
	}

	@Test
	void isDockableDragIsTrueForContentDragUtilsWrote() {
		Map<DataFormat, Object> content = DragUtils.content(createDockable());

		assertThat(DragUtils.isDockableDrag(content::get))
				.describedAs("isDockableDrag() for a dragged dockable")
				.isTrue();
	}

	@Test
	void isDockableDragFalseForPlainTextThatLooksLikeDockableContent() {
		Map<DataFormat, Object> content = Map.of(
				DataFormat.PLAIN_TEXT,
				DragUtils.PREFIX + DRAG_GROUP + ";" + IDENTIFIER
		);

		assertThat(DragUtils.isDockableDrag(content::get))
				.describedAs("isDockableDrag() for plain text")
				.isFalse();
	}

	@Test
	void isDockableDragFalseForEmptyContent() {
		Map<DataFormat, Object> content = Map.of();

		assertThat(DragUtils.isDockableDrag(content::get))
				.describedAs("isDockableDrag() for empty content")
				.isFalse();
	}

	private static Dockable createDockable() {
		Dockable dockable = new Dockable(new Bento(), IDENTIFIER);
		dockable.dragGroupMaskProperty().set(DRAG_GROUP);
		return dockable;
	}
}
