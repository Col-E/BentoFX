package software.coley.bentofx.demo.persistence.provider;

import javafx.scene.control.Label;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableMenuFactory;
import software.coley.bentofx.persistence.core.api.provider.DockableMenuFactoryProvider;
import software.coley.bentofx.persistence.core.api.provider.DockableStateProvider;
import software.coley.bentofx.persistence.core.api.state.DockableState;
import software.coley.bentofx.persistence.core.api.state.DockableState.DockableStateBuilder;

import java.util.Optional;

import static javafx.scene.effect.BlurType.ONE_PASS_BOX;
import static javafx.scene.paint.Color.BLACK;

/**
 * This demo's {@link DockableStateProvider}, rebuilding the content of each
 * {@link Dockable} in a restored layout.
 *
 * <p>Constructed by the application and passed to {@code getLayoutRestorer}.</p>
 *
 * @author Phil Bryant
 */
public class BoxAppDockableStateProvider implements DockableStateProvider {

	private static final Logger logger =
			LoggerFactory.getLogger(BoxAppDockableStateProvider.class);

	private final @Nullable DockableMenuFactoryProvider dockableMenuFactoryProvider;

	/**
	 * Creates a {@code BoxAppDockableStateProvider}.
	 *
	 * @param dockableMenuFactoryProvider {@link DockableMenuFactory} to use to create
	 * {@code ContextMenu} instances.
	 */
	public BoxAppDockableStateProvider(
			final @Nullable DockableMenuFactoryProvider dockableMenuFactoryProvider
	) {
		this.dockableMenuFactoryProvider = dockableMenuFactoryProvider;
	}

	@Override
	public Optional<DockableState> resolveDockableState(
			String id
	) {
		// Built here rather than in the constructor because a state holds
		// JavaFX components, the constructor runs on the JavaFX-Launcher thread
		// where those cannot be built.
		//
		// Built fresh on every call rather than cached, because a state holds
		// one node instance and a node has one parent. Handing the same state
		// to two restores would have the second layout take the first layout's
		// content out of it, leaving a blank panel behind.
		return DockableProperties.findByIdentifier(id)
				.map(this::buildDockableState);
	}

	/**
	 * {@return the {@link DockableState} for the supplied
	 * {@link DockableProperties}.}
	 *
	 * @param dockableProperties the properties of the dockable to build.
	 */
	private DockableState buildDockableState(
			final DockableProperties dockableProperties
	) {
		final String dockableIdentifier = dockableProperties.getIdentifier();
		final boolean isDecorated = dockableProperties.isDecorated();
		final int shapeMode = dockableProperties.getShapeMode();
		final int colorIndex = dockableProperties.getColorIndex();

		final String nodeText = isDecorated ?
				"<" + dockableIdentifier + ":" + colorIndex + ">" :
				"<" + dockableIdentifier + ">";

		final DockableStateBuilder builder =
				new DockableStateBuilder(dockableIdentifier)
						.setTitle(dockableIdentifier)
						.setTooltipText(dockableProperties.getTooltipText())
						.setDockableNode(new Label(nodeText))
						.setDockableConsumer(
								BoxAppDockableStateProvider::consumeDockable
						);

		if (!isDecorated) {
			return builder.build();
		}

		builder.setDockableIconFactory(dockable -> makeIcon(shapeMode, colorIndex))
				.setDockableMenuFactory(resolveMenuFactory(dockableIdentifier));

		if (shapeMode > 0) {
			builder.setDragGroupMask(1);
			builder.setClosable(false);
		}

		return builder.build();
	}

	/**
	 * {@return the {@link DockableMenuFactory} for the identified dockable, or
	 * {@code null} when this demo was built without a menu factory provider or that
	 * provider has none for it.}
	 *
	 * @param dockableIdentifier identifies the dockable whose menu is wanted.
	 */
	private @Nullable DockableMenuFactory resolveMenuFactory(
			final String dockableIdentifier
	) {
		if (dockableMenuFactoryProvider == null) {
			return null;
		}

		return dockableMenuFactoryProvider
				.getDockableMenuFactory(dockableIdentifier)
				.orElse(null);
	}

	private static Shape makeIcon(int shapeMode, int i) {
		final int radius = 6;
		Shape icon = switch (shapeMode) {
			case 1 -> new Polygon(
					radius,
					0,
					0,
					radius * 2,
					radius * 2,
					radius * 2
			);
			case 2 -> new Rectangle(radius * 2d, radius * 2d);
			default -> new Circle(radius);
		};
		switch (i) {
			case 0 -> icon.setFill(Color.RED);
			case 1 -> icon.setFill(Color.ORANGE);
			case 2 -> icon.setFill(Color.LIME);
			case 3 -> icon.setFill(Color.CYAN);
			case 4 -> icon.setFill(Color.BLUE);
			case 5 -> icon.setFill(Color.PURPLE);
			default -> icon.setFill(Color.GREY);
		}
		icon.setEffect(
				new InnerShadow(
						ONE_PASS_BOX,
						BLACK,
						2F,
						10F,
						0,
						0
				)
		);
		return icon;
	}

	/**
	 * Callback function executed when the {@link Dockable} is created from the
	 * {@link DockableState} returned by {@link #buildDockableState}.
	 *
	 * @param dockable the {@link Dockable} created from the
	 * {@link DockableState} returned by {@link #buildDockableState}.
	 */
	private static void consumeDockable(Dockable dockable) {
		logger.debug("Consuming dockable {}", dockable);
	}
}
