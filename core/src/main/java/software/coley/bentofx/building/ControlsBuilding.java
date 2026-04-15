package software.coley.bentofx.building;

import javafx.geometry.Orientation;
import javafx.geometry.Side;
import org.jspecify.annotations.Nullable;
import software.coley.bentofx.control.ContentWrapper;
import software.coley.bentofx.control.Header;
import software.coley.bentofx.control.HeaderPane;
import software.coley.bentofx.control.Headers;
import software.coley.bentofx.control.canvas.PixelCanvas;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.container.DockContainerLeaf;

/**
 * Builders for various bento UI controls.
 *
 * @author Matt Coley
 */
public class ControlsBuilding implements HeaderPaneFactory, HeadersFactory, HeaderFactory, ContentWrapperFactory, CanvasFactory {
	private static final HeaderPaneFactory DEFAULT_HEADER_PANE_FACTORY = HeaderPane::new;
	private static final HeadersFactory DEFAULT_HEADERS_FACTORY = Headers::new;
	private static final HeaderFactory DEFAULT_HEADER_FACTORY = (dockable, parentPane) -> new Header(dockable, parentPane).withDragDrop();
	private static final ContentWrapperFactory DEFAULT_CONTENT_WRAPPER_FACTORY = ContentWrapper::new;
	private static final CanvasFactory DEFAULT_CANVAS_FACTORY = (parentPane) -> new PixelCanvas();
	private HeaderPaneFactory headerPaneFactory = DEFAULT_HEADER_PANE_FACTORY;
	private HeadersFactory headersFactory = DEFAULT_HEADERS_FACTORY;
	private HeaderFactory headerFactory = DEFAULT_HEADER_FACTORY;
	private ContentWrapperFactory contentWrapperFactory = DEFAULT_CONTENT_WRAPPER_FACTORY;
	private CanvasFactory canvasFactory = DEFAULT_CANVAS_FACTORY;

	/**
	 * @return Factory for creating {@link HeaderPane}.
	 */
	public HeaderPaneFactory getHeaderPaneFactory() {
		return headerPaneFactory;
	}

	/**
	 * @param headerPaneFactory
	 * 		Factory for creating {@link HeaderPane}.
	 *        {@code null} to use the default factory.
	 */
	public void setHeaderPaneFactory(@Nullable HeaderPaneFactory headerPaneFactory) {
		if (headerPaneFactory == null)
			headerPaneFactory = DEFAULT_HEADER_PANE_FACTORY;
		this.headerPaneFactory = headerPaneFactory;
	}

	/**
	 * @return Factory for creating {@link Headers}.
	 */
	public HeadersFactory getHeadersFactory() {
		return headersFactory;
	}

	/**
	 * @param headersFactory
	 * 		Factory for creating {@link Headers}.
	 *        {@code null} to use the default factory.
	 */
	public void setHeadersFactory(@Nullable HeadersFactory headersFactory) {
		if (headersFactory == null)
			headersFactory = DEFAULT_HEADERS_FACTORY;
		this.headersFactory = headersFactory;
	}

	/**
	 * @return Factory for creating {@link Header}.
	 */
	public HeaderFactory getHeaderFactory() {
		return headerFactory;
	}

	/**
	 * @param headerFactory
	 * 		Factory for creating {@link Header}.
	 *        {@code null} to use the default factory.
	 */
	public void setHeaderFactory(@Nullable HeaderFactory headerFactory) {
		if (headerFactory == null)
			headerFactory = DEFAULT_HEADER_FACTORY;
		this.headerFactory = headerFactory;
	}

	/**
	 * @return Factory for creating {@link ContentWrapper}.
	 */
	public ContentWrapperFactory getContentWrapperFactory() {
		return contentWrapperFactory;
	}

	/**
	 * @param contentWrapperFactory
	 * 		Factory for creating {@link ContentWrapper}.
	 *        {@code null} to use the default factory.
	 */
	public void setContentWrapperFactory(@Nullable ContentWrapperFactory contentWrapperFactory) {
		if (contentWrapperFactory == null)
			contentWrapperFactory = DEFAULT_CONTENT_WRAPPER_FACTORY;
		this.contentWrapperFactory = contentWrapperFactory;
	}

	/**
	 * @return Factory for creating {@link PixelCanvas}.
	 */
	public CanvasFactory getCanvasFactory() {
		return canvasFactory;
	}

	/**
	 * @param canvasFactory
	 * 		Factory for creating {@link PixelCanvas}.
	 *        {@code null} to use the default factory.
	 */
	public void setCanvasFactory(@Nullable CanvasFactory canvasFactory) {
		if (canvasFactory == null)
			canvasFactory = DEFAULT_CANVAS_FACTORY;
		this.canvasFactory = canvasFactory;
	}

	@Override
	public HeaderPane newHeaderPane(DockContainerLeaf container) {
		return headerPaneFactory.newHeaderPane(container);
	}

	@Override
	public Headers newHeaders(DockContainerLeaf container, Orientation orientation, Side side) {
		return headersFactory.newHeaders(container, orientation, side);
	}

	@Override
	public Header newHeader(Dockable dockable, HeaderPane parentPane) {
		return headerFactory.newHeader(dockable, parentPane);
	}

	@Override
	public ContentWrapper newContentWrapper(DockContainerLeaf container) {
		return contentWrapperFactory.newContentWrapper(container);
	}

	@Override
	public PixelCanvas newCanvas(DockContainerLeaf container) {
		return canvasFactory.newCanvas(container);
	}
}
