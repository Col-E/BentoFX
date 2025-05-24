package software.coley.bentofx.util;

import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import software.coley.bentofx.Bento;
import software.coley.bentofx.dockable.DockableDestination;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.layout.RootContentLayout;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Stage subtype created by {@link Bento#newStageForDroppedHeader(DockableDestination, Header)}.
 *
 * @author Matt Coley
 */
public class DragDropStage extends Stage {
	private final boolean autoCloseWhenEmpty;
	private WeakReference<Parent> content;

	/**
	 * @param autoCloseWhenEmpty
	 * 		Flag to determine if this stage should auto-close if its sole content is removed.
	 * 		See	{@link #isAutoCloseWhenEmpty()} for more details.
	 */
	public DragDropStage(boolean autoCloseWhenEmpty) {
		this.autoCloseWhenEmpty = autoCloseWhenEmpty;

		// Cancel closure if headers that are not closable exist.
		addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, e -> {
			Parent root = getScene().getRoot();
			if (root != null) {
				boolean canClose = true;

				// Try to close all headers and track if any remain.
				List<Header> headersInScene = BentoUtils.getCastChildren(root, Header.class);
				for (Header header : headersInScene)
					if (!header.removeFromParent(Header.RemovalReason.CLOSING))
						canClose = false;

				// If some headers remain, abort the close.
				if (!canClose)
					e.consume();
			}
		});

		// Add event filters to clear/restore the scene contents when hiding/showing.
		// Each respective action will trigger the root contents listeners that handle registering/unregistering.
		addEventFilter(WindowEvent.WINDOW_HIDDEN, e -> {
			Parent parent = getScene().getRoot();
			if (parent != null)
				content = new WeakReference<>(parent);
			getScene().setRoot(new Region());
		});
		addEventFilter(WindowEvent.WINDOW_SHOWN, e -> {
			if (content != null) {
				getScene().setRoot(content.get());
				content.clear();
				content = null;
			}
		});
	}

	/**
	 * <b>Context:</b>These stages are created when a user drags a {@link Header} into empty space.
	 * <p/>
	 * Most of the time, if a user drags the {@link Header} from this stage into some other place in another stage,
	 * leaving this stage with nothing in {@link RootContentLayout#getLayout()} it would be ideal to automatically
	 * close this window.
	 * <p/>
	 * When this is {@code tre} we do just that.
	 *
	 * @return {@code true} when this stage should auto-close if its {@link RootContentLayout#getLayout()} is cleared/removed.
	 */
	public boolean isAutoCloseWhenEmpty() {
		return autoCloseWhenEmpty;
	}
}
