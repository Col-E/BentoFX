package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import javafx.stage.Stage;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import software.coley.bentofx.persistence.core.api.DockingLayout;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.LayoutRestorer;
import software.coley.bentofx.persistence.core.api.LayoutSaver;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.provider.DockContainerLeafMenuFactoryProvider;
import software.coley.bentofx.persistence.core.api.provider.DockableStateProvider;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.core.api.provider.DockingLayoutRestorable;
import software.coley.bentofx.persistence.core.api.provider.LayoutCodecProvider;
import software.coley.bentofx.persistence.core.api.provider.StageIconImageProvider;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;
import software.coley.bentofx.persistence.core.impl.provider.DefaultDockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.testfixtures.codec.InMemoryLayoutCodec;
import software.coley.bentofx.persistence.testfixtures.provider.InMemoryLayoutStorageProvider;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage for {@link DefaultArrangementState}, the default-arrangement tracking.
 *
 * <p>Runs as an integration-test graphical because the constructor schedules a
 * recording through {@link javafx.application.Platform#runLater} when no
 * session layout is stored, so the FX toolkit must be running.</p>
 *
 * @author Phil Bryant
 */
@ExtendWith(ApplicationExtension.class)
class DefaultArrangementStateITG {

	@Start
	@SuppressWarnings("unused") // Called by ApplicationExtension
	private void start(final Stage ignored) {
		// No stage needed. ApplicationExtension starts the FX toolkit for us.
	}

	/**
	 * Happy path: {@link DefaultArrangementState#isDefaultLayoutShowing()} returns
	 * {@code true} when the extended provider has no stored default arrangement, the
	 * common initial state before any default arrangement has been recorded.
	 */
	@Test
	void isDefaultLayoutShowingReturnsTrueWhenNoDefaultArrangementIsRecorded() {
		final DefaultArrangementState state =
				new DefaultArrangementState(createRestorable(createPersistenceProvider()));

		assertThat(state.isDefaultLayoutShowing())
				.describedAs("isDefaultLayoutShowing with no default arrangement stored")
				.isTrue();
	}

	/**
	 * Edge case: {@link DefaultArrangementState#isDefaultLayoutShowing()} returns
	 * {@code true} when the provider does not implement
	 * {@link software.coley.bentofx.persistence.core.api.provider.PersistedDockingLayoutOrganizationProvider},
	 * because there is no way to read back what the default arrangement is.
	 */
	@Test
	void isDefaultLayoutShowingReturnsTrueWhenProviderIsNotExtended() {
		final DefaultArrangementState state =
				new DefaultArrangementState(nonExtendedRestorable());

		assertThat(state.isDefaultLayoutShowing())
				.describedAs("isDefaultLayoutShowing with non-extended provider")
				.isTrue();
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * {@return a provider backed by in-memory storage and a single in-memory codec.}
	 */
	private static DefaultDockingLayoutPersistenceProvider createPersistenceProvider() {
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		return new DefaultDockingLayoutPersistenceProvider(
				List.of(new SingleCodecProvider(codec)),
				List.of(new InMemoryLayoutStorageProvider())
		);
	}

	/**
	 * {@return a {@link DockingLayoutRestorable} backed by the supplied extended
	 * provider, throwing {@link UnsupportedOperationException} for every method
	 * {@link DefaultArrangementState} does not use.}
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
				// Returned non-throwing so the FX-thread task
				// DefaultArrangementState schedules at construction can run
				// cleanly rather than reach the FX uncaught handler.
				return new DefaultBentoProvider();
			}
		};
	}

	/**
	 * {@return a {@link DockingLayoutRestorable} backed by a provider that does
	 * NOT implement
	 * {@link software.coley.bentofx.persistence.core.api.provider.PersistedDockingLayoutOrganizationProvider}.}
	 */
	private static DockingLayoutRestorable nonExtendedRestorable() {
		return createRestorable(new NonExtendedProvider());
	}

	/**
	 * A {@link LayoutCodecProvider} that hands out one codec instance, so that what
	 * one call encoded a later call can decode.
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

	/**
	 * A {@link DockingLayoutPersistenceProvider} that does NOT implement
	 * {@link software.coley.bentofx.persistence.core.api.provider.PersistedDockingLayoutOrganizationProvider},
	 * used to exercise the short-circuit path in
	 * {@link DefaultArrangementState#isDefaultLayoutShowing()}.
	 */
	private record NonExtendedProvider() implements DockingLayoutPersistenceProvider {

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
			throw new UnsupportedOperationException();
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
			return false;
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
	}
}
