package software.coley.bentofx.persistence.core.ui.layoutsmenu;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import software.coley.bentofx.Bento;
import software.coley.bentofx.building.DockBuilding;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.persistence.core.api.BentoStateException;
import software.coley.bentofx.persistence.core.api.LayoutPersistenceProfile;
import software.coley.bentofx.persistence.core.api.codec.LayoutCodec;
import software.coley.bentofx.persistence.core.api.provider.BentoProvider;
import software.coley.bentofx.persistence.core.api.provider.LayoutCodecProvider;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.impl.provider.DefaultDockingLayoutPersistenceProvider;
import software.coley.bentofx.persistence.testfixtures.codec.InMemoryLayoutCodec;
import software.coley.bentofx.persistence.testfixtures.provider.InMemoryLayoutStorageProvider;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Side.BOTTOM;
import static org.assertj.core.api.Assertions.assertThat;
import static software.coley.bentofx.persistence.core.api.storage.LayoutIdentifiers.DEFAULT_LAYOUT_IDENTIFIER;

/**
 * Coverage for {@link LayoutArrangements#isShowing}, which decides whether the
 * {@code Default} menu item is marked.
 *
 * <p>Each test shows an arrangement in a real window, records it through a real
 * persistence provider and reads it back as state, then changes what is showing
 * and compares.</p>
 *
 * @author Phil Bryant
 */
@ExtendWith(ApplicationExtension.class)
class LayoutArrangementsITG {

	private static final LayoutPersistenceProfile DEFAULT_PROFILE =
			LayoutPersistenceProfile.of(DEFAULT_LAYOUT_IDENTIFIER);

	private final Bento bento = new Bento("arrangements-bento");
	private final BentoProvider bentoProvider = BentoProvider.of(bento);
	private final DefaultDockingLayoutPersistenceProvider persistenceProvider =
			new DefaultDockingLayoutPersistenceProvider(
					List.of(codecProvider(new InMemoryLayoutCodec())),
					List.of(new InMemoryLayoutStorageProvider())
			);

	@SuppressWarnings("NullAway.Init")
	private Stage stage;

	@SuppressWarnings("NullAway.Init")
	private DockContainerRootBranch shownRoot;

	@SuppressWarnings("NullAway.Init")
	private List<BentoState> recordedArrangement;

	@BeforeEach
	void setUp(final FxRobot robot) throws BentoStateException {
		robot.interact(() -> {
			shownRoot = buildRoot();
			stage = new Stage();
			stage.setScene(new Scene(shownRoot, 800, 600));
			stage.show();
		});

		persistenceProvider.saveLayout(DEFAULT_PROFILE, bentoProvider);
		recordedArrangement = persistenceProvider.getStoredBentoStates(DEFAULT_PROFILE)
				.orElseThrow();
	}

	@AfterEach
	void tearDown(final FxRobot robot) {
		robot.interact(() -> stage.close());
	}

	@Test
	void getStoredBentoStatesIsEmptyWhenNothingIsStored() throws BentoStateException {
		assertThat(
				persistenceProvider.getStoredBentoStates(
						LayoutPersistenceProfile.of("nothing-here")
				)
		)
				.describedAs("getStoredBentoStates() for a layout never saved")
				.isEmpty();
	}

	@Test
	void isShowingWhileNothingHasChanged(final FxRobot robot) {
		assertThat(isShowing(robot))
				.describedAs("isShowing() for the arrangement just recorded")
				.isTrue();
	}

	/**
	 * The bug this guards: a restored session layout that a user rearranged
	 * was reported as the default layout.
	 */
	@Test
	void isNotShowingOnceADockableIsMovedToAnotherLeaf(final FxRobot robot) {
		robot.interact(() -> {
			final Dockable movedDockable = leaf(0).getDockables().getLast();
			leaf(0).removeDockable(movedDockable);
			leaf(1).addDockable(movedDockable);
		});

		assertThat(isShowing(robot))
				.describedAs("isShowing() after moving a dockable")
				.isFalse();
	}

	@Test
	void isNotShowingOnceADifferentTabIsSelected(final FxRobot robot) {
		robot.interact(() ->
				leaf(0)
						.selectDockable(
								leaf(0)
										.getDockables()
										.getLast()
						)
		);

		assertThat(isShowing(robot))
				.describedAs("isShowing() after selecting a different tab")
				.isFalse();
	}

	@Test
	void isShowingAgainOnceTheArrangementIsPutBack(final FxRobot robot) {
		robot.interact(() -> {
					final DockContainerLeaf leaf = leaf(0);
					final ObservableList<Dockable> dockables =
							leaf.getDockables();
					leaf.selectDockable(dockables.getLast());
					leaf.selectDockable(dockables.getFirst());
				}
		);

		assertThat(isShowing(robot))
				.describedAs(
						"isShowing() after selecting a tab and " +
								"selecting the original back"
				)
				.isTrue();
	}

	/**
	 * Divider positions move whenever the window is resized, so they are not
	 * part of the arrangement.
	 */
	@Test
	void isStillShowingAfterADividerMoves(final FxRobot robot) {
		robot.interact(() ->
				shownRoot.setDividerPosition(0, 0.2)
		);

		assertThat(isShowing(robot))
				.describedAs("isShowing() after moving a divider")
				.isTrue();
	}

	/**
	 * Replacing the scene's root is what takes a root branch out of its
	 * {@link Bento}, the way an application swaps one layout for another.
	 */
	@Test
	void isNotShowingWhenTheRecordedRootIsNoLongerShown(final FxRobot robot) {
		robot.interact(() -> stage.getScene().setRoot(new Pane()));

		assertThat(isShowing(robot))
				.describedAs(
						"isShowing() with the recorded layout taken" +
								" off the screen"
				)
				.isFalse();
	}

	/**
	 * {@return whether the recorded arrangement is showing, asked on the JavaFX
	 * Application Thread.}
	 *
	 * @param robot runs the check on the JavaFX Application Thread.
	 */
	private boolean isShowing(final FxRobot robot) {
		final AtomicBoolean isShowing = new AtomicBoolean();

		robot.interact(() -> isShowing.set(
				LayoutArrangements.isShowing(
						recordedArrangement,
						bentoProvider
				)
		));

		return isShowing.get();
	}

	/**
	 * {@return the shown root's leaf at the index.}
	 *
	 * @param index the leaf's position in the root.
	 */
	private DockContainerLeaf leaf(final int index) {
		return (DockContainerLeaf) shownRoot.getChildContainers().get(index);
	}

	/**
	 * {@return a new root: two leaves stacked vertically, the bottom one on the
	 * bottom side and collapsed.}
	 */
	private DockContainerRootBranch buildRoot() {
		final DockBuilding builder = bento.dockBuilding();
		final DockContainerRootBranch root = builder.root("root");
		final DockContainerLeaf topLeaf = builder.leaf("top");
		final DockContainerLeaf bottomLeaf = builder.leaf("bottom");

		root.setOrientation(VERTICAL);
		root.addContainers(topLeaf, bottomLeaf);
		bottomLeaf.setSide(BOTTOM);

		topLeaf.addDockable(builder.dockable("editor-1"));
		topLeaf.addDockable(builder.dockable("editor-2"));
		bottomLeaf.addDockable(builder.dockable("terminal"));

		root.setContainerCollapsed(bottomLeaf, true);

		return root;
	}

	/**
	 * {@return a provider yielding the codec, so a save and a read share it.}
	 *
	 * @param codec the codec to yield.
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
}
