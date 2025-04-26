package software.coley.bentofx.impl.layout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Dockable;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.content.EmptyContent;
import software.coley.bentofx.content.SingleContent;
import software.coley.bentofx.content.TabbedContent;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.LeafContentLayout;
import software.coley.bentofx.path.ContentPath;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.path.LayoutPath;
import software.coley.bentofx.path.PathBuilder;
import software.coley.bentofx.util.BentoUtils;

import java.util.Collections;
import java.util.List;

public class ImplLeafContentLayout extends BorderPane implements LeafContentLayout {
	private final Bento bento;
	private final String identifier;
	private Content content;

	public ImplLeafContentLayout(@Nonnull Bento bento, @Nullable Content content) {
		this(bento, content, BentoUtils.newIdentifier());
	}

	public ImplLeafContentLayout(@Nonnull Bento bento, @Nullable Content content, @Nonnull String identifier) {
		this.bento = bento;
		this.content = content;
		this.identifier = identifier;

		setContent(content);
	}

	@Nonnull
	@Override
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public boolean matchesIdentity(@Nullable Identifiable other) {
		if (other == null) return false;
		return identifier.equals(other.getIdentifier());
	}

	@Nonnull
	@Override
	public Region getBackingRegion() {
		return this;
	}

	@Nullable
	@Override
	public LayoutPath findLayout(@Nonnull PathBuilder builder, @Nonnull String id) {
		if (identifier.equals(id))
			// Parent already called 'builder.inside(...)'
			return builder.buildLayoutPath();
		return null;
	}

	@Nullable
	@Override
	public ContentPath findContent(@Nonnull PathBuilder builder, @Nonnull String id) {
		Content content = getContent();
		if (id.equals(content.getIdentifier()))
			return builder.withContent(content)
					.buildContentPath();
		return null;
	}

	@Nullable
	@Override
	public DockablePath findDockable(@Nonnull PathBuilder builder, @Nonnull String id) {
		Content content = getContent();
		switch (content) {
			case SingleContent singleContent -> {
				Dockable dockable = singleContent.getDockable();
				if (id.equals(dockable.getIdentifier())) {
					return builder.withContent(content)
							.withDockable(dockable)
							.buildDockablePath();
				}
			}
			case TabbedContent tabbedContent -> {
				for (Dockable dockable : tabbedContent.getDockables()) {
					if (id.equals(dockable.getIdentifier())) {
						return builder.withContent(content)
								.withDockable(dockable)
								.buildDockablePath();
					}
				}
			}
			case EmptyContent ignored -> {/* no-op */}
		}
		return null;
	}

	@Override
	public boolean removeDockable(@Nonnull Dockable dockable) {
		Content content = getContent();
		switch (content) {
			case SingleContent singleContent -> {
				if (singleContent.getDockable() == dockable) {
					setContent(null);
					return true;
				}
			}
			case TabbedContent tabbedContent -> {
				return tabbedContent.removeDockable(dockable);
			}
			case EmptyContent ignored -> {/* no-op */}
		}
		return false;
	}

	@Override
	public boolean replaceChildLayout(@Nonnull ContentLayout child, @Nonnull ContentLayout replacement) {
		// There are no children in this layout to replace.
		return false;
	}

	@Override
	public boolean removeChildLayout(@Nonnull ContentLayout child) {
		// There are no children in this layout to remove.
		return false;
	}

	@Nonnull
	@Override
	public List<ContentLayout> getChildLayouts() {
		return Collections.emptyList();
	}

	@Nonnull
	@Override
	public Content getContent() {
		return content;
	}

	@Override
	public void setContent(@Nullable Content content) {
		if (content == null)
			content = bento.newEmptyContent(this);
		this.content = content;
		setCenter(content.getBackingRegion());
	}

	@Nonnull
	@Override
	public Bento getBento() {
		return bento;
	}
}
