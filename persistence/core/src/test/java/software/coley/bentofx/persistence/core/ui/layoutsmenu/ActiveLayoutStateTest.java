package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.DockingLayout;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutRestorable;
import software.coley.bentofx.persistence.core.api.provider.LayoutCodecProvider;
import software.coley.bentofx.persistence.core.impl.provider.DefaultDockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.testfixtures.codec.InMemoryLayoutCodec;
import software.coley.bentofx.persistence.testfixtures.provider.InMemoryLayoutStorageProvider;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers.SESSION_LAYOUT_IDENTIFIER;

/**
 * Coverage for {@link ActiveLayoutState}: the active-layout tracking extracted
 * from {@link LayoutsMenu}.
 *
 * <p>The state itself needs no JavaFX toolkit, so tests run headless.</p>
 *
 * @author Phil Bryant
 */
class ActiveLayoutStateTest {

	/**
	 * Happy path: switching to a named layout records its identifier through
	 * {@link software.coley.bentofx.persistence.core.api.provider.PersistedDockingLayoutOrganizationProvider#setActiveLayoutIdentifier}.
	 */
	@Test
	void setActiveCustomLayoutProfileRecordsTheIdentifierThroughTheProvider()
			throws BentoStateException {
		final DefaultDockingLayoutPersistenceProvider provider = createPersistenceProvider();
		final ActiveLayoutState state = new ActiveLayoutState(createRestorable(provider));
		final LayoutPersistenceProfile profile =
				LayoutPersistenceProfile.of("my-layout").withNaming("My Layout", null);

		state.setActiveCustomLayoutProfile(profile);

		assertThat(provider.getActiveLayoutIdentifier(
				LayoutPersistenceProfile.of(SESSION_LAYOUT_IDENTIFIER)
		))
				.describedAs("active layout identifier after setActiveCustomLayoutProfile")
				.contains("my-layout");
	}

	/**
	 * Edge case: {@link ActiveLayoutState#isActiveLayout} answers {@code false}
	 * when no named layout has been activated.
	 */
	@Test
	void isActiveLayoutReturnsFalseWhenNoLayoutIsActive() {
		final ActiveLayoutState state =
				new ActiveLayoutState(createRestorable(createPersistenceProvider()));
		final LayoutPersistenceProfile profile =
				LayoutPersistenceProfile.of("any-layout");

		assertThat(state.isActiveLayout(profile))
				.describedAs("isActiveLayout with no active profile set")
				.isFalse();
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * {@return a provider backed by in-memory storage and a single in-memory
	 * codec.}
	 */
	private static DefaultDockingLayoutPersistenceProvider createPersistenceProvider() {
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		return new DefaultDockingLayoutPersistenceProvider(
				List.of(new SingleCodecProvider(codec)),
				List.of(new InMemoryLayoutStorageProvider())
		);
	}

	/**
	 * {@return a {@link DockingLayoutRestorable} that returns the supplied
	 * provider and throws {@link UnsupportedOperationException} for every
	 * method not needed by {@link ActiveLayoutState}.}
	 *
	 * @param provider what {@link DockingLayoutRestorable#getPersistenceProvider()}
	 * hands back.
	 */
	private static DockingLayoutRestorable createRestorable(
			final DockingLayoutPersistenceProvider provider
	) {
		return new DockingLayoutRestorable() {
			@Override
			public DockingLayout getDefaultDockingLayout() {
				throw new UnsupportedOperationException();
			}

			@Override
			public DockingLayout getDockingLayout(
					final LayoutPersistenceProfile layoutPersistenceProfile,
					final Supplier<DockingLayout> fallbackLayoutSupplier
			) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean switchToLayout(final Supplier<DockingLayout> dockingLayoutSupplier) {
				throw new UnsupportedOperationException();
			}

			@Override
			public DockingLayoutPersistenceProvider getPersistenceProvider() {
				return provider;
			}

			@Override
			public BentoProvider getBentoProvider() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * A {@link LayoutCodecProvider} that hands out one codec instance, so that
	 * what one call encoded a later call can decode.
	 */
	private record SingleCodecProvider(InMemoryLayoutCodec codec)
			implements LayoutCodecProvider {

		@Override
		public String getIdentifier() {
			return codec.getIdentifier();
		}

		@Override
		public boolean isDefault() {
			return true;
		}

		@Override
		public LayoutCodec getLayoutCodec() {
			return codec;
		}
	}
}
