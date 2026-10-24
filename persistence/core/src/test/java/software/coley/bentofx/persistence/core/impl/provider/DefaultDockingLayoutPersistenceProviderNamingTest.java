package software.coley.bentofx.persistence.core.impl.provider;

import org.junit.jupiter.api.Test;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.codec.PersistableLayout;
import software.coley.bentofx.persistence.core.api.provider.LayoutCodecProvider;
import software.coley.bentofx.persistence.core.api.provider.LayoutStorageProvider;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.storage.LayoutStorage;
import software.coley.bentofx.persistence.testfixtures.codec.InMemoryLayoutCodec;
import software.coley.bentofx.persistence.testfixtures.provider.InMemoryLayoutStorageProvider;
import software.coley.bentofx.persistence.testfixtures.storage.InMemoryLayoutStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers.ACTIVE_LAYOUT_IDENTIFIER;
import static software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers.GROUP_CATALOG_LAYOUT_IDENTIFIER;
import static software.coley.bentofx.persistence.testfixtures.codec.state.SampleBentoStateFactory.createBentoStates;

/**
 * Coverage for the operations that reach a stored layout without reading the
 * scene graph: rewriting what a layout is called, the catalog of groups, and
 * which layout is recorded as active.
 *
 * <p>These need storage that keeps what was written to it, which
 * {@code ConfigurableLayoutStorageProvider} deliberately does not - it hands out a fresh,
 * empty storage per call so that a test can watch what a component asks for.
 * {@link InMemoryLayoutStorageProvider} keeps bytes per layout identifier instead,
 * so a read after a write returns what the write put there, which is the whole
 * point of what is under test.</p>
 *
 * @author Phil Bryant
 */
class DefaultDockingLayoutPersistenceProviderNamingTest {

	private static final String LAYOUT_IDENTIFIER = "compact";

	@Test
	void updateStoredLayoutNamingRewritesTheNamingAndKeepsTheState()
			throws BentoStateException {
		final InMemoryLayoutStorageProvider storage =
				new InMemoryLayoutStorageProvider();
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final DefaultDockingLayoutPersistenceProvider provider =
				createPersistenceProvider(storage, codec);
		final List<BentoState> original = createBentoStates();

		store(storage, codec, LAYOUT_IDENTIFIER, new PersistableLayout("Wide", original));

		final boolean rewritten = provider.updateStoredLayoutNaming(
				LayoutPersistenceProfile.of(LAYOUT_IDENTIFIER)
						.withNaming("Renamed", "Debugging")
		);

		assertThat(rewritten)
				.describedAs("updateStoredLayoutNaming for a stored layout")
				.isTrue();

		final PersistableLayout stored = read(storage, codec, LAYOUT_IDENTIFIER);

		assertThat(stored.displayName())
				.describedAs("display name after the rewrite")
				.isEqualTo("Renamed");
		assertThat(stored.group())
				.describedAs("group after the rewrite")
				.isEqualTo("Debugging");
		assertThat(stored.bentoStates())
				.describedAs("docking state after the rewrite")
				.usingRecursiveComparison()
				.isEqualTo(original);
	}

	/**
	 * One layout that cannot be read must not hide the others - a menu built
	 * from this listing would otherwise show nothing at all, including the
	 * broken layout the user might want to delete.
	 */
	@Test
	void getStoredLayoutsListsEveryLayoutWhenOneCannotBeRead()
			throws BentoStateException {
		final InMemoryLayoutStorageProvider storage =
				new InMemoryLayoutStorageProvider();
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final DefaultDockingLayoutPersistenceProvider provider =
				createPersistenceProvider(storage, codec);

		store(storage, codec, LAYOUT_IDENTIFIER, new PersistableLayout("Wide", createBentoStates()));
		((InMemoryLayoutStorage) storage.getLayoutStorage("broken", codec.getIdentifier()))
				.write("not written by this codec".getBytes(StandardCharsets.UTF_8));

		final List<LayoutPersistenceProfile> listed =
				provider.getStoredLayouts(LayoutPersistenceProfile.of(LAYOUT_IDENTIFIER));

		assertThat(listed)
				.describedAs("layouts listed with one unreadable")
				.extracting(LayoutPersistenceProfile::layoutIdentifier, LayoutPersistenceProfile::displayName)
				.containsExactlyInAnyOrder(
						tuple(LAYOUT_IDENTIFIER, "Wide"),
						tuple("broken", null)
				);
	}

	/**
	 * A rewrite is encoded in full before storage is opened, so a codec that
	 * fails part way through leaves the stored layout as it was instead of
	 * committing what it had written so far.
	 */
	@Test
	void updateStoredLayoutNamingLeavesTheLayoutAloneWhenEncodingFails()
			throws BentoStateException {
		final InMemoryLayoutStorageProvider storage =
				new InMemoryLayoutStorageProvider();
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final DefaultDockingLayoutPersistenceProvider provider =
				createPersistenceProvider(storage, new EncodeFailingCodec(codec));

		store(storage, codec, LAYOUT_IDENTIFIER, new PersistableLayout("Wide", createBentoStates()));

		assertThatThrownBy(() -> provider.updateStoredLayoutNaming(
				LayoutPersistenceProfile.of(LAYOUT_IDENTIFIER).withNaming("Renamed", null)
		))
				.describedAs("updateStoredLayoutNaming with a codec that fails to encode")
				.isInstanceOf(BentoStateException.class);

		assertThat(read(storage, codec, LAYOUT_IDENTIFIER).displayName())
				.describedAs("stored display name after a failed rewrite")
				.isEqualTo("Wide");
	}

	/**
	 * The layout is read to completion before anything is opened for writing.
	 *
	 * <p>Opening for write truncates a file-backed layout, so reading and writing
	 * through one open storage would empty the layout being renamed - it would
	 * decode nothing and store that. Asserting on the order catches a reordering
	 * here, which no assertion on the result can: an in-memory double survives
	 * either order.</p>
	 */
	@Test
	void updateStoredLayoutNamingReadsBeforeItOpensAnythingForWriting()
			throws BentoStateException {
		final RecordingStorageProvider storage = new RecordingStorageProvider();
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final DefaultDockingLayoutPersistenceProvider provider =
				createPersistenceProvider(storage, codec);

		store(storage, codec, LAYOUT_IDENTIFIER,
				new PersistableLayout("Wide", createBentoStates()));
		storage.streamsOpened().clear();

		provider.updateStoredLayoutNaming(
				LayoutPersistenceProfile.of(LAYOUT_IDENTIFIER)
						.withNaming("Renamed", null)
		);

		assertThat(storage.streamsOpened())
				.describedAs("streams opened while rewriting the naming")
				.containsExactly(
						"read:" + LAYOUT_IDENTIFIER,
						"write:" + LAYOUT_IDENTIFIER
				);
	}

	/**
	 * A rename has to be able to clear a group, which is how a layout is taken out
	 * of one, so a {@code null} is written rather than skipped.
	 */
	@Test
	void updateStoredLayoutNamingClearsAGroupWhenGivenNone()
			throws BentoStateException {
		final InMemoryLayoutStorageProvider storage =
				new InMemoryLayoutStorageProvider();
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final DefaultDockingLayoutPersistenceProvider provider =
				createPersistenceProvider(storage, codec);

		store(storage, codec, LAYOUT_IDENTIFIER,
				new PersistableLayout(
						"Wide",
						createBentoStates(),
						"Debugging",
						List.of()
				));

		provider.updateStoredLayoutNaming(
				LayoutPersistenceProfile.of(LAYOUT_IDENTIFIER)
						.withNaming("Wide", null)
		);

		assertThat(read(storage, codec, LAYOUT_IDENTIFIER).group())
				.describedAs("group after being cleared")
				.isNull();
	}

	/**
	 * Nothing stored means nothing written. Creating an entry here would leave a
	 * layout holding no docking state, which nothing could restore.
	 */
	@Test
	void updateStoredLayoutNamingWritesNothingWhenNothingIsStored()
			throws BentoStateException {
		final InMemoryLayoutStorageProvider storage =
				new InMemoryLayoutStorageProvider();
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final DefaultDockingLayoutPersistenceProvider provider =
				createPersistenceProvider(storage, codec);

		final boolean rewritten = provider.updateStoredLayoutNaming(
				LayoutPersistenceProfile.of(LAYOUT_IDENTIFIER)
						.withNaming("Renamed", null)
		);

		assertThat(rewritten)
				.describedAs("updateStoredLayoutNaming for a layout not stored")
				.isFalse();
		assertThat(storage.getLayoutIdentifiers(codec.getIdentifier()))
				.describedAs("layouts in storage afterwards")
				.isEmpty();
	}

	@Test
	void groupCatalogRoundTripsThroughItsReservedEntry()
			throws BentoStateException {
		final InMemoryLayoutStorageProvider storage =
				new InMemoryLayoutStorageProvider();
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final DefaultDockingLayoutPersistenceProvider provider =
				createPersistenceProvider(storage, codec);
		final LayoutPersistenceProfile profile =
				LayoutPersistenceProfile.of(LAYOUT_IDENTIFIER);

		provider.setStoredGroups(profile, List.of("Debugging", "Presentation"));

		assertThat(provider.getStoredGroups(profile))
				.describedAs("group catalog read back")
				.containsExactly("Debugging", "Presentation");
		assertThat(storage.getLayoutIdentifiers(codec.getIdentifier()))
				.describedAs("what the catalog is stored under")
				.containsExactly(GROUP_CATALOG_LAYOUT_IDENTIFIER);
	}

	/**
	 * A group with no layouts in it is the case the catalog exists for, so an
	 * empty catalog has to be a real answer rather than a fault.
	 */
	@Test
	void reportsNoGroupsWhenTheCatalogWasNeverWritten()
			throws BentoStateException {
		final DefaultDockingLayoutPersistenceProvider provider =
				createPersistenceProvider(new InMemoryLayoutStorageProvider(), new InMemoryLayoutCodec());

		assertThat(provider.getStoredGroups(
				LayoutPersistenceProfile.of(LAYOUT_IDENTIFIER)
		))
				.describedAs("group catalog that was never written")
				.isEmpty();
	}

	@Test
	void emptyingTheCatalogLeavesNoGroups() throws BentoStateException {
		final DefaultDockingLayoutPersistenceProvider provider =
				createPersistenceProvider(new InMemoryLayoutStorageProvider(), new InMemoryLayoutCodec());
		final LayoutPersistenceProfile profile =
				LayoutPersistenceProfile.of(LAYOUT_IDENTIFIER);

		provider.setStoredGroups(profile, List.of("Debugging"));
		provider.setStoredGroups(profile, List.of());

		assertThat(provider.getStoredGroups(profile))
				.describedAs("group catalog after being emptied")
				.isEmpty();
	}

	@Test
	void activeLayoutRoundTripsThroughItsReservedEntry()
			throws BentoStateException {
		final InMemoryLayoutStorageProvider storage =
				new InMemoryLayoutStorageProvider();
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final DefaultDockingLayoutPersistenceProvider provider =
				createPersistenceProvider(storage, codec);
		final LayoutPersistenceProfile profile =
				LayoutPersistenceProfile.of(LAYOUT_IDENTIFIER);

		provider.setActiveLayoutIdentifier(profile, "compact-wide");

		assertThat(provider.getActiveLayoutIdentifier(profile))
				.describedAs("active layout identifier read back")
				.contains("compact-wide");
		assertThat(storage.getLayoutIdentifiers(codec.getIdentifier()))
				.describedAs("what the active layout entry is stored under")
				.containsExactly(ACTIVE_LAYOUT_IDENTIFIER);
	}

	/**
	 * No named layout has ever been made active is the ordinary starting
	 * state, so it has to be a real answer rather than a fault.
	 */
	@Test
	void reportsNoActiveLayoutWhenNeverWritten() throws BentoStateException {
		final DefaultDockingLayoutPersistenceProvider provider =
				createPersistenceProvider(new InMemoryLayoutStorageProvider(), new InMemoryLayoutCodec());

		assertThat(provider.getActiveLayoutIdentifier(
				LayoutPersistenceProfile.of(LAYOUT_IDENTIFIER)
		))
				.describedAs("active layout identifier that was never written")
				.isEmpty();
	}

	/**
	 * Switching back to the default layout clears the recorded active one,
	 * which is what a {@code null} does here.
	 */
	@Test
	void clearingTheActiveLayoutLeavesNoneRecorded() throws BentoStateException {
		final DefaultDockingLayoutPersistenceProvider provider =
				createPersistenceProvider(new InMemoryLayoutStorageProvider(), new InMemoryLayoutCodec());
		final LayoutPersistenceProfile profile =
				LayoutPersistenceProfile.of(LAYOUT_IDENTIFIER);

		provider.setActiveLayoutIdentifier(profile, "compact-wide");
		provider.setActiveLayoutIdentifier(profile, null);

		assertThat(provider.getActiveLayoutIdentifier(profile))
				.describedAs("active layout identifier after being cleared")
				.isEmpty();
	}

	/**
	 * The group lives inside the layout, so listing has to decode to find it. A
	 * caller building a menu reads it off the profiles it gets back.
	 */
	@Test
	void listingStoredLayoutsCarriesTheGroupOntoEachProfile()
			throws BentoStateException {
		final InMemoryLayoutStorageProvider storage =
				new InMemoryLayoutStorageProvider();
		final InMemoryLayoutCodec codec = new InMemoryLayoutCodec();
		final DefaultDockingLayoutPersistenceProvider provider =
				createPersistenceProvider(storage, codec);

		store(storage, codec, LAYOUT_IDENTIFIER,
				new PersistableLayout(
						"TCP/IP Debug",
						createBentoStates(),
						"Debugging",
						List.of()
				));

		assertThat(provider.getStoredLayouts(
				LayoutPersistenceProfile.of(LAYOUT_IDENTIFIER)
		))
				.describedAs("stored layouts listed as profiles")
				.singleElement()
				.satisfies(profile -> {
					assertThat(profile.displayName())
							.describedAs("listed display name")
							.isEqualTo("TCP/IP Debug");
					assertThat(profile.group())
							.describedAs("listed group")
							.isEqualTo("Debugging");
				});
	}

	/**
	 * {@return a provider wired to the supplied storage and to one codec that keeps
	 * what it encoded.}
	 *
	 * @param storage the storage to write through.
	 * @param codec the codec whose output is stored.
	 */
	private static DefaultDockingLayoutPersistenceProvider createPersistenceProvider(
			final LayoutStorageProvider storage,
			final LayoutCodec codec
	) {
		return new DefaultDockingLayoutPersistenceProvider(
				List.of(new SingleCodecProvider(codec)),
				List.of(storage)
		);
	}

	/**
	 * Encodes and writes a layout the way an earlier save would have left it, so a
	 * test can start from a stored layout.
	 */
	private static void store(
			final LayoutStorageProvider storage,
			final LayoutCodec codec,
			final String layoutIdentifier,
			final PersistableLayout layout
	) throws BentoStateException {
		try (final OutputStream outputStream =
					 storage.getLayoutStorage(layoutIdentifier, codec.getIdentifier())
							 .openOutputStream()) {

			codec.encode(layout, outputStream);
		} catch (final IOException e) {
			throw new BentoStateException("Could not store the layout.", e);
		}
	}

	/**
	 * {@return the stored layout, decoded.}
	 */
	private static PersistableLayout read(
			final LayoutStorageProvider storage,
			final LayoutCodec codec,
			final String layoutIdentifier
	) throws BentoStateException {
		try (final InputStream inputStream =
					 storage.getLayoutStorage(layoutIdentifier, codec.getIdentifier())
							 .openInputStream()) {

			return codec.decode(inputStream);
		} catch (final IOException e) {
			throw new BentoStateException("Could not read the layout.", e);
		}
	}

	/**
	 * Wraps {@link InMemoryLayoutStorageProvider} to record the order in which
	 * streams are opened, so a test can assert a read happened before a write
	 * rather than only that both did. The recording is why this is not the general
	 * fixture: only the read-before-write test needs it.
	 */
	private static final class RecordingStorageProvider implements LayoutStorageProvider {

		private final InMemoryLayoutStorageProvider delegate =
				new InMemoryLayoutStorageProvider();
		private final List<String> streamsOpened = new ArrayList<>();

		@Override
		public String getIdentifier() {
			return delegate.getIdentifier();
		}

		@Override
		public boolean isDefault() {
			return delegate.isDefault();
		}

		@Override
		public List<String> getLayoutIdentifiers(final String codecIdentifier) {
			return delegate.getLayoutIdentifiers(codecIdentifier);
		}

		@Override
		public boolean isLayoutStored(
				final String layoutIdentifier,
				final String codecIdentifier
		) {
			return delegate.isLayoutStored(layoutIdentifier, codecIdentifier);
		}

		@Override
		public boolean deleteLayout(
				final String layoutIdentifier,
				final String codecIdentifier
		) {
			return delegate.deleteLayout(layoutIdentifier, codecIdentifier);
		}

		@Override
		public LayoutStorage getLayoutStorage(
				final String layoutIdentifier,
				final String codecIdentifier
		) {
			final LayoutStorage inner =
					delegate.getLayoutStorage(layoutIdentifier, codecIdentifier);

			return new LayoutStorage() {
				@Override
				public boolean exists() {
					return inner.exists();
				}

				@Override
				public OutputStream openOutputStream() throws IOException {
					streamsOpened.add("write:" + layoutIdentifier);
					return inner.openOutputStream();
				}

				@Override
				public InputStream openInputStream() throws IOException {
					streamsOpened.add("read:" + layoutIdentifier);
					return inner.openInputStream();
				}
			};
		}

		private List<String> streamsOpened() {
			return streamsOpened;
		}
	}

	/**
	 * Decodes through another codec, but writes part of a layout and then fails
	 * every encode - the way a codec that breaks mid-stream behaves.
	 */
	private record EncodeFailingCodec(LayoutCodec delegate) implements LayoutCodec {

		@Override
		public String getIdentifier() {
			return delegate.getIdentifier();
		}

		@Override
		public void encode(
				final PersistableLayout layout,
				final OutputStream outputStream
		) throws BentoStateException {
			try {
				outputStream.write("partial".getBytes(StandardCharsets.UTF_8));
			} catch (final IOException e) {
				throw new BentoStateException("Could not write.", e);
			}
			throw new BentoStateException("Encoding failed part way through.");
		}

		@Override
		public PersistableLayout decode(final InputStream inputStream)
				throws BentoStateException {
			return delegate.decode(inputStream);
		}
	}

	/**
	 * A {@link LayoutCodecProvider} handing out one codec instance, so that what
	 * one call encoded a later call can decode.
	 */
	private record SingleCodecProvider(LayoutCodec codec)
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
