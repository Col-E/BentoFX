package software.coley.bentofx.persistence.core.api.provider;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.LayoutRestorer;
import software.coley.bentofx.persistence.core.api.LayoutSaver;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DockingLayoutPersistenceProviderTest {

	private static final String LAYOUT_IDENTIFIER = "layout-1";

	@Test
	void saveLayoutByIdentifierDelegatesToTheProfileOverload() throws BentoStateException {
		final AtomicReference<LayoutPersistenceProfile> savedProfile = new AtomicReference<>();

		final DockingLayoutPersistenceProvider provider = new DockingLayoutPersistenceProvider() {
			@Override
			public LayoutSaver getLayoutSaver(
					final LayoutPersistenceProfile layoutPersistenceProfile,
					final BentoProvider bentoProvider
			) {
				throw new UnsupportedOperationException();
			}

			@Override
			public LayoutRestorer getLayoutRestorer(
					final LayoutPersistenceProfile layoutPersistenceProfile,
					final BentoProvider bentoProvider,
					final DockableStateProvider dockableStateProvider,
					final @Nullable StageIconImageProvider stageIconImageProvider,
					final @Nullable DockContainerLeafMenuFactoryProvider dockContainerLeafMenuFactoryProvider
			) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void saveLayout(
					final LayoutPersistenceProfile layoutPersistenceProfile,
					final BentoProvider bentoProvider
			) {
				savedProfile.set(layoutPersistenceProfile);
			}

			@Override
			public List<String> getStoredLayoutIdentifiers(
					final LayoutPersistenceProfile layoutPersistenceProfile
			) {
				throw new UnsupportedOperationException();
			}

			@Override
			public List<LayoutPersistenceProfile> getStoredLayouts(
					final LayoutPersistenceProfile layoutPersistenceProfile
			) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean isLayoutStored(
					final LayoutPersistenceProfile layoutPersistenceProfile
			) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean deleteLayout(
					final LayoutPersistenceProfile layoutPersistenceProfile
			) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean updateStoredLayoutNaming(
					final LayoutPersistenceProfile layoutPersistenceProfile
			) {
				throw new UnsupportedOperationException();
			}
		};

		provider.saveLayout(LAYOUT_IDENTIFIER, new DefaultBentoProvider());

		assertThat(savedProfile.get())
				.describedAs("profile saveLayout(String, BentoProvider) delegated with")
				.isEqualTo(LayoutPersistenceProfile.of(LAYOUT_IDENTIFIER));
	}
}
