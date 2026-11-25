package software.coley.bentofx.persistence.core.impl.state;

import javafx.scene.Node;
import javafx.scene.control.Label;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import software.coley.bentofx.Bento;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.persistence.core.api.state.DockableState;
import software.coley.bentofx.persistence.core.api.state.DockableState.DockableStateBuilder;

import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ApplicationExtension.class)
class DockableStateBuilderITG {

	private static final String DOCKABLECONSUMER_CONSUMEDDOCKABLE = "DockableConsumer.consumedDockable";

	@Test
	void testDockableBuilder() {

		final String expectedDockableId = "dockable";
		final Node expectedDockableNode = new Label("Dockable node");
		final String expectedTitle = "Selected";
		final String expectedTooltip = "Tooltip text for Selected.";
		final int expectedDragGroupMask = 7;
		final boolean expectedClosable = true;
		final Bento expectedBento = new Bento("bento");
		final Dockable expectedDockable =
				new Dockable(expectedBento, expectedDockableId);

		DockableState dockable =
				new DockableStateBuilder(expectedDockableId)
						.setDockableNode(expectedDockableNode)
						.setTitle(expectedTitle)
						.setTooltipText(expectedTooltip)
						.setDockableIconFactory(null)
						.setDockableMenuFactory(null)
						.setDragGroupMask(expectedDragGroupMask)
						.setClosable(expectedClosable)
						.setDockableConsumer(DockableConsumer::consumeDockable)
						.build();

		assertThat(dockable.getIdentifier())
				.describedAs("dockable.getIdentifier()")
				.isEqualTo(expectedDockableId);

		assertThat(dockable.getDockableNode())
				.describedAs("dockable.getDockableNode()")
				.contains(expectedDockableNode);

		assertThat(dockable.getTitle())
				.describedAs("dockable.getTitle()")
				.contains(expectedTitle);

		assertThat(dockable.getTooltipText())
				.describedAs("dockable.getTooltipText()")
				.contains(expectedTooltip);

		assertThat(dockable.getDockableIconFactory())
				.describedAs("dockable.getDockableIconFactory()")
				.isEmpty();

		assertThat(dockable.getDockableMenuFactory())
				.describedAs("dockable.getDockableMenuFactory()")
				.isEmpty();

		assertThat(dockable.getDragGroupMask())
				.describedAs("dockable.getDragGroupMask()")
				.contains(expectedDragGroupMask);

		assertThat(dockable.isClosable())
				.describedAs("dockable.isClosable()")
				.contains(expectedClosable);

		Optional<Consumer<Dockable>> optionalDockableConsumer =
				dockable.getDockableConsumer();
		assertThat(optionalDockableConsumer)
				.describedAs("optionalDockableConsumer")
				.isPresent();

		assertThat(DockableConsumer.consumedDockable)
				.describedAs(DOCKABLECONSUMER_CONSUMEDDOCKABLE)
				.isNull();

		optionalDockableConsumer.get().accept(expectedDockable);
		assertThat(DockableConsumer.consumedDockable)
				.describedAs(DOCKABLECONSUMER_CONSUMEDDOCKABLE)
				.isEqualTo(expectedDockable);
	}

	private static class DockableConsumer {

		private static @Nullable Dockable consumedDockable = null;
		private static void consumeDockable(Dockable dockable) {
			consumedDockable = dockable;
		}
	}
}
