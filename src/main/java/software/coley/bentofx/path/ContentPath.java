package software.coley.bentofx.path;

import jakarta.annotation.Nonnull;
import software.coley.bentofx.content.Content;
import software.coley.bentofx.layout.ContentLayout;
import software.coley.bentofx.layout.RootContentLayout;

import java.util.List;

public record ContentPath(@Nonnull RootContentLayout rootLayout,
                          @Nonnull List<ContentLayout> subLayouts,
                          @Nonnull Content content) {
}
