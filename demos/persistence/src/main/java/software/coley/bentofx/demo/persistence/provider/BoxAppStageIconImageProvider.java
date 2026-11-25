package software.coley.bentofx.demo.persistence.provider;

import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.bentofx.persistence.core.api.provider.StageIconImageProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This demo's {@link StageIconImageProvider}, loading the application icons from
 * resources.
 *
 * <p>Constructed by the application and passed to {@code getLayoutRestorer}.</p>
 *
 * @author Phil Bryant
 */
public class BoxAppStageIconImageProvider implements StageIconImageProvider {

	private static final Logger logger =
			LoggerFactory.getLogger(BoxAppStageIconImageProvider.class);

	private static final List<String> ICON_RESOURCES = List.of(
			"/images/logo-16.png",
			"/images/logo-32.png",
			"/images/logo-48.png",
			"/images/logo-256.png"
	);

	@Override
	public Collection<Image> getStageIcons() {

		final List<Image> images = new ArrayList<>();

		for (final String iconResource : ICON_RESOURCES) {
			try (
					final InputStream inputStream =
							getClass().getResourceAsStream(iconResource)
			) {
				if (inputStream == null) {

					logger.warn(
							"Could not find the resource {}.", iconResource
					);
				} else {

					images.add(new Image(inputStream));
				}
			} catch (IOException e) {

				logger.warn(
						"Could not read the resource {}.", iconResource,
						e
				);
			}
		}

		return images;
	}
}
