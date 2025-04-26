package software.coley.bentofx.path;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.RootContentLayout;

import java.util.List;

public record LayoutPath(@Nonnull RootContentLayout rootLayout,
                         @Nonnull List<ContentLayout> layouts) {
}
