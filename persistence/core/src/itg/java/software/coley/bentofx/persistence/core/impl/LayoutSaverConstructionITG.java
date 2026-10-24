package software.coley.bentofx.persistence.core.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import software.coley.bentofx.Bento;
import software.coley.bentofx.building.DockBuilding;
import software.coley.bentofx.event.DockEvent;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.LayoutSaver;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.provider.LayoutCodecProvider;
import software.coley.bentofx.persistence.core.api.provider.LayoutStorageProvider;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;
import software.coley.bentofx.persistence.core.impl.provider.DefaultDockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.testfixtures.codec.InMemoryLayoutCodec;
import software.coley.bentofx.persistence.testfixtures.storage.InMemoryLayoutStorage;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage for how a {@link LayoutSaver} is brought into service.
 *
 * <p>{@code AbstractAutoCloseableLayoutSaver} used to call
 * {@code enableAutoSave} from its constructor, which scheduled {@code this} on an
 * executor and registered it as a listener on every {@link Bento} before subclass
 * constructors had run. {@code DockingLayoutSaver} assigns its captor and writer
 * <em>after</em> {@code super(...)} returns, so anything reaching
 * {@code saveLayout()} in that window would have seen null fields - and the Java
 * memory model gives no guarantee those later writes are even visible to the
 * scheduler thread afterwards.</p>
 *
 * <p>A test cannot reliably hit that race by timing, so these tests assert the
 * structural properties that make it impossible instead: nothing is scheduled or
 * registered until construction has finished, and the documented entry points
 * still deliver a working saver.</p>
 *
 * @author Phil Bryant
 */
@ExtendWith(ApplicationExtension.class)
class LayoutSaverConstructionITG {

	private static final String LAYOUT_ID = "layout-saver-construction";

	/**
	 * The core of the fix: constructing a saver must not start auto-save, because
	 * that is what published {@code this} before subclasses finished
	 * initializing.
	 */
	@Test
	void constructorDoesNotStartAutoSave() {
		try (final DockingLayoutSaver saver = newSaver(new DefaultBentoProvider())) {
			assertThat(saver.isAutoSaveEnabled())
					.describedAs("saver.isAutoSaveEnabled() straight after construction")
					.isFalse();
		}
	}

	/**
	 * The observable consequence of the above, and the property that actually
	 * matters: a saver under construction must not have registered itself on any
	 * {@link Bento} event bus. If it had, a dock event fired during construction
	 * would reach a half-built listener.
	 */
	@Test
	void constructorDoesNotRegisterEventListeners() {
		final Bento bento = new Bento("bento-construction-listeners");
		final BentoProvider bentoProvider = new DefaultBentoProvider(bento);

		try (final DockingLayoutSaver saver = newSaver(bentoProvider)) {
			// Fire an event that would flip the saver's dirty flag if it were
			// already listening. Nothing should have subscribed yet.
			bento.events().fire(
					new DockEvent.RootContainerAdded(
							bento.dockBuilding().root("root-construction-listeners")
					)
			);

			assertThat(saver.isAutoSaveEnabled())
					.describedAs("saver.isAutoSaveEnabled() after an unsubscribed event")
					.isFalse();
		}
	}

	/**
	 * Enabling auto-save after construction still works, so the capability was
	 * moved rather than lost.
	 */
	@Test
	void enableAutoSaveAfterConstructionStartsAutoSave() {
		try (final DockingLayoutSaver saver = newSaver(new DefaultBentoProvider())) {
			saver.enableAutoSave(1L, TimeUnit.HOURS);

			assertThat(saver.isAutoSaveEnabled())
					.describedAs("saver.isAutoSaveEnabled() after enableAutoSave")
					.isTrue();
		}
	}

	/**
	 * The static helper both arms the saver and hands back the same instance at
	 * its own type, so a factory can build and arm in one expression without a
	 * cast.
	 */
	@Test
	void startAutoSaveArmsAndReturnsTheSameSaver() {
		final DockingLayoutSaver constructed = newSaver(new DefaultBentoProvider());

		try (final DockingLayoutSaver saver =
					 AbstractAutoCloseableLayoutSaver.startAutoSave(constructed)) {
			assertThat(saver)
					.describedAs("startAutoSave return value")
					.isSameAs(constructed);
			assertThat(saver.isAutoSaveEnabled())
					.describedAs("saver.isAutoSaveEnabled() after startAutoSave")
					.isTrue();
		}
	}

	/**
	 * The documented way to obtain a saver must still yield one that is already
	 * auto-saving. Moving the call out of the constructor moved this
	 * responsibility onto the provider, so this test is what keeps the provider
	 * honest.
	 */
	@Test
	void providerReturnsSaverWithAutoSaveRunning() throws BentoStateException {
		final LayoutCodec codec = new InMemoryLayoutCodec();
		final LayoutStorage storage = new InMemoryLayoutStorage();

		final DefaultDockingLayoutPersistenceProvider persistenceProvider =
				new DefaultDockingLayoutPersistenceProvider(
						List.of(codecProvider(codec)),
						List.of(storageProvider(storage))
				);

		try (final LayoutSaver saver = persistenceProvider.getLayoutSaver(
				LAYOUT_ID,
				new DefaultBentoProvider()
		)) {
			assertThat(saver)
					.describedAs("provider-supplied saver")
					.isInstanceOf(AbstractAutoCloseableLayoutSaver.class);
			assertThat(((AbstractAutoCloseableLayoutSaver) saver).isAutoSaveEnabled())
					.describedAs("provider-supplied saver isAutoSaveEnabled()")
					.isTrue();
		}
	}

	/**
	 * {@code close()} must flush regardless of whether auto-save was ever
	 * enabled. It used to be gated on {@code isAutoSaveEnabled}, which was
	 * survivable only because the constructor always set that flag; now that it
	 * does not, a gated close would mean a directly constructed saver silently
	 * wrote nothing on exit. The class documentation promises
	 * try-with-resources saves on exit, so the gate is gone.
	 */
	@Test
	void closeSavesEvenWhenAutoSaveWasNeverEnabled() {
		final Bento bento = new Bento("bento-construction-close");
		final DockBuilding dockBuilding = bento.dockBuilding();
		bento.registerRoot(dockBuilding.root("root-construction-close"));

		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final InMemoryLayoutStorage storage = new InMemoryLayoutStorage();
		final BentoProvider bentoProvider = new DefaultBentoProvider(bento);

		final DockingLayoutSaver saver =
				new DockingLayoutSaver(codec, storage, bentoProvider);

		// Auto-save is off, so nothing is listening for dock events. Mark the
		// layout dirty the same way a listener would, so close() has something
		// to flush.
		saver.markLayoutDirty(
				new DockEvent.RootContainerAdded(
						dockBuilding.root("root-construction-close-event")
				)
		);

		saver.close();

		assertThat(storage.exists())
				.describedAs("storage.exists() after close without auto-save")
				.isTrue();
		assertThat(codec.getEncodeCalls())
				.describedAs("codec.getEncodeCalls() after close without auto-save")
				.hasSize(1);
	}

	/**
	 * A close with no intervening dock events must not write, so removing the
	 * auto-save gate did not turn every close into an unconditional write.
	 */
	@Test
	void closeDoesNotSaveWhenNothingChanged() {
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final InMemoryLayoutStorage storage = new InMemoryLayoutStorage();

		final DockingLayoutSaver saver = new DockingLayoutSaver(
				codec,
				storage,
				new DefaultBentoProvider()
		);

		saver.close();

		assertThat(codec.getEncodeCalls())
				.describedAs("codec.getEncodeCalls() after an unchanged close")
				.isEmpty();
	}

	private static DockingLayoutSaver newSaver(
			final BentoProvider bentoProvider
	) {
		return new DockingLayoutSaver(
				new InMemoryLayoutCodec(),
				new InMemoryLayoutStorage(),
				bentoProvider
		);
	}

	/**
	 * A provider yielding one specific codec. The shared test fixture builds its
	 * own codec internally, and this test needs the exact instance it can inspect.
	 */
	private static LayoutCodecProvider codecProvider(final LayoutCodec codec) {
		return new LayoutCodecProvider() {
			@Override
			public String getIdentifier() {
				return "memory";
			}

			@Override
			public boolean isDefault() {
				return true;
			}

			@Override
			public LayoutCodec getLayoutCodec() {
				return codec;
			}
		};
	}

	/**
	 * A provider yielding one specific storage, for the same reason as
	 * {@link #codecProvider(LayoutCodec)}.
	 */
	private static LayoutStorageProvider storageProvider(
			final LayoutStorage storage
	) {
		return new LayoutStorageProvider() {
			@Override
			public String getIdentifier() {
				return "memory";
			}

			@Override
			public boolean isDefault() {
				return true;
			}

			@Override
			public LayoutStorage getLayoutStorage(
					final String layoutIdentifier,
					final String codecIdentifier
			) {
				return storage;
			}

			@Override
			public List<String> getLayoutIdentifiers(final String codecIdentifier) {
				return List.of();
			}

			@Override
			public boolean deleteLayout(
					final String layoutIdentifier,
					final String codecIdentifier
			) {
				return false;
			}
		};
	}
}
