package software.coley.bentofx.impl.layout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import software.coley.bentofx.Bento;
import software.coley.bentofx.Identifiable;
import software.coley.bentofx.space.DockSpace;
import software.coley.bentofx.space.EmptyDockSpace;
import software.coley.bentofx.space.SingleDockSpace;
import software.coley.bentofx.space.TabbedDockSpace;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockLayout;
import software.coley.bentofx.layout.LeafDockLayout;
import software.coley.bentofx.path.SpacePath;
import software.coley.bentofx.path.DockablePath;
import software.coley.bentofx.path.LayoutPath;
import software.coley.bentofx.path.PathBuilder;
import software.coley.bentofx.util.BentoUtils;

import java.util.Collections;
import java.util.List;

public class ImplLeafDockLayout extends BorderPane implements LeafDockLayout {
	private final Bento bento;
	private final String identifier;
	private DockSpace space;

	public ImplLeafDockLayout(@Nonnull Bento bento, @Nullable DockSpace space) {
		this(bento, space, BentoUtils.newIdentifier());
	}

	public ImplLeafDockLayout(@Nonnull Bento bento, @Nullable DockSpace space, @Nonnull String identifier) {
		this.bento = bento;
		this.space = space;
		this.identifier = identifier;

		BentoUtils.disableWhenNoParent(this);

		getStyleClass().addAll("layout-leaf");

		setSpace(space);
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
	public SpacePath findSpace(@Nonnull PathBuilder builder, @Nonnull String id) {
		DockSpace space = getSpace();
		if (id.equals(space.getIdentifier()))
			return builder.withSpace(space)
					.buildSpacePath();
		return null;
	}

	@Nullable
	@Override
	public DockablePath findDockable(@Nonnull PathBuilder builder, @Nonnull String id) {
		DockSpace space = getSpace();
		switch (space) {
			case SingleDockSpace single -> {
				Dockable dockable = single.getDockable();
				if (id.equals(dockable.getIdentifier())) {
					return builder.withSpace(space)
							.withDockable(dockable)
							.buildDockablePath();
				}
			}
			case TabbedDockSpace tabbed -> {
				for (Dockable dockable : tabbed.getDockables()) {
					if (id.equals(dockable.getIdentifier())) {
						return builder.withSpace(space)
								.withDockable(dockable)
								.buildDockablePath();
					}
				}
			}
			case EmptyDockSpace ignored -> {/* no-op */}
		}
		return null;
	}

	@Override
	public boolean removeDockable(@Nonnull Dockable dockable) {
		DockSpace space = getSpace();
		return switch (space) {
			case SingleDockSpace single -> {
				if (single.getDockable() == dockable) {
					setSpace(null);
					yield true;
				}
				yield false;
			}
			case TabbedDockSpace tabbed -> tabbed.removeDockable(dockable);
			case EmptyDockSpace ignored -> false;
		};
	}

	@Override
	public boolean closeDockable(@Nonnull Dockable dockable) {
		DockSpace space = getSpace();
		return switch (space) {
			case TabbedDockSpace tabbed -> tabbed.closeDockable(dockable);
			case SingleDockSpace ignored -> false;
			case EmptyDockSpace ignored -> false;
		};
	}

	@Override
	public boolean replaceChildLayout(@Nonnull DockLayout child, @Nonnull DockLayout replacement) {
		// There are no children in this layout to replace.
		return false;
	}

	@Override
	public boolean removeChildLayout(@Nonnull DockLayout child) {
		// There are no children in this layout to remove.
		return false;
	}

	@Nonnull
	@Override
	public List<DockLayout> getChildLayouts() {
		return Collections.emptyList();
	}

	@Nonnull
	@Override
	public DockSpace getSpace() {
		return space;
	}

	@Override
	public void setSpace(@Nullable DockSpace space) {
		if (space == null)
			space = bento.newEmptySpace(this);
		this.space = space;
		setCenter(space.getBackingRegion());
	}

	@Nonnull
	@Override
	public Bento getBento() {
		return bento;
	}
}
