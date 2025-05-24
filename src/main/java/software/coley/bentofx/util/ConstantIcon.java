package software.coley.bentofx.util;

import jakarta.annotation.Nonnull;
import javafx.scene.Node;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableIconFactory;

public record ConstantIcon(@Nonnull Node icon) implements DockableIconFactory {
	@Override
	public Node build(@Nonnull Dockable dockable) {
		return icon;
	}
}
