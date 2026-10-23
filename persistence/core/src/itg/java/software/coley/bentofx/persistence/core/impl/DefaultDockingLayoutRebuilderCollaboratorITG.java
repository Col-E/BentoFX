package software.coley.bentofx.persistence.core.impl;

import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import software.coley.bentofx.Bento;
import software.coley.bentofx.control.DragDropStage;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerLeafMenuFactory;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.persistence.core.api.BentoLayout;
import software.coley.bentofx.persistence.core.api.DockingLayout;
import software.coley.bentofx.persistence.core.api.state.BentoState;
import software.coley.bentofx.persistence.core.api.state.DockContainerBranchState.DockContainerBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerLeafState.DockContainerLeafStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockContainerRootBranchState.DockContainerRootBranchStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DockableState;
import software.coley.bentofx.persistence.core.api.state.DockableState.DockableStateBuilder;
import software.coley.bentofx.persistence.core.api.state.DragDropStageState;
import software.coley.bentofx.persistence.core.api.state.DragDropStageState.DragDropStageStateBuilder;
import software.coley.bentofx.persistence.core.impl.provider.DefaultBentoProvider;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Side.LEFT;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ApplicationExtension.class)
class DefaultDockingLayoutRebuilderCollaboratorITG {

	private static final String BENTO_ID = "bento-state-restorer";
	private static final String ROOT_BRANCH_ID = "root-state-restorer";
	private static final String BRANCH_ID = "branch-state-restorer";
	private static final String LEAF_ID = "leaf-state-restorer";
	private static final String DOCKABLE_ID = "dockable-state-restorer";
	private static final String MISSING_DOCKABLE_ID = "missing-dockable-state-restorer";
	private static final String DOCKABLE_TITLE = "Restored Dockable";
	private static final String DOCKABLE_TOOLTIP_TEXT = "Restored Dockable Tooltip";

	@Test
	void restoreDockingLayoutRestoresNestedContainersAndDockableProperties(
			final FxRobot robot
	) {
		final RestoredDockingLayout restoredDockingLayout =
				restoreNestedDockingLayout(robot);

		assertDockingLayout(restoredDockingLayout.dockingLayout());
		final BentoLayout bentoLayout =
				restoredDockingLayout.dockingLayout()
						.getBentoLayouts()
						.getFirst();
		assertBentoLayout(bentoLayout);

		final DockContainerRootBranch rootBranch =
				bentoLayout.getRootBranches().getFirst();
		assertRootBranch(rootBranch);

		final DockContainerBranch branch =
				getOnlyChildContainer(rootBranch, DockContainerBranch.class);
		assertBranch(branch);

		final DockContainerLeaf leaf =
				getOnlyChildContainer(branch, DockContainerLeaf.class);
		assertLeaf(leaf);

		final Dockable dockable = leaf.getDockables().getFirst();
		assertDockable(dockable);
		assertSelectedDockable(leaf, dockable);
		assertThat(restoredDockingLayout.dockableConsumerCalled())
				.describedAs("restoredDockingLayout.dockableConsumerCalled()")
				.isTrue();
	}

	@Test
	void restoreDockingLayoutSkipsUnresolvedDockableStates(
			final FxRobot robot
	) {
		final AtomicReference<DockingLayout> dockingLayoutReference =
				new AtomicReference<>();

		robot.interact(() -> {
			final DockableState missingDockableState =
					new DockableStateBuilder(MISSING_DOCKABLE_ID)
							.setDockableNode(new Label(MISSING_DOCKABLE_ID))
							.build();

			final DockContainerLeafStateBuilder leafStateBuilder =
					new DockContainerLeafStateBuilder(LEAF_ID);
			leafStateBuilder.addChildDockableState(missingDockableState);

			final DockContainerRootBranchStateBuilder rootBranchStateBuilder =
					new DockContainerRootBranchStateBuilder(ROOT_BRANCH_ID);
			rootBranchStateBuilder.addDockContainerState(leafStateBuilder.build());

			final BentoState bentoState =
					new BentoState.BentoStateBuilder(BENTO_ID)
							.addRootBranchState(rootBranchStateBuilder.build())
							.build();

			dockingLayoutReference.set(
					new DefaultDockingLayoutRebuilder(
							new DefaultBentoProvider(new Bento(BENTO_ID)),
							actualId -> Optional.empty(),
							null,
							null
					).rebuild(List.of(bentoState))
			);
		});

		final DockContainerRootBranch rootBranch = dockingLayoutReference.get()
				.getBentoLayouts()
				.getFirst()
				.getRootBranches()
				.getFirst();
		final DockContainerLeaf leaf =
				getOnlyChildContainer(rootBranch, DockContainerLeaf.class);

		assertThat(leaf.getIdentifier())
				.describedAs("leaf.getIdentifier()")
				.isEqualTo(LEAF_ID);
		assertThat(leaf.getDockables())
				.describedAs("leaf.getDockables()")
				.isEmpty();
	}

	/**
	 * The persisted showing flag has to reach the caller, because this module
	 * never shows a stage. Without it an application can only show every restored
	 * stage, which reopens a window the user had closed.
	 */
	@Test
	void restoreDockingLayoutCarriesThePersistedShowingFlagToTheCaller(
			final FxRobot robot
	) {
		final AtomicReference<BentoLayout> bentoLayoutReference =
				new AtomicReference<>();

		robot.interact(() -> {
			final BentoState bentoState =
					new BentoState.BentoStateBuilder(BENTO_ID)
							.addDragDropStageState(
									dragDropStageState("hidden", false)
							)
							.addDragDropStageState(
									dragDropStageState("visible", true)
							)
							.addDragDropStageState(
									dragDropStageState("unspecified", null)
							)
							.build();

			bentoLayoutReference.set(
					new DefaultDockingLayoutRebuilder(
							new DefaultBentoProvider(new Bento(BENTO_ID)),
							actualId -> Optional.empty(),
							null,
							null
					)
							.rebuild(List.of(bentoState))
							.getBentoLayouts()
							.getFirst()
			);
		});

		final BentoLayout bentoLayout = bentoLayoutReference.get();
		final List<DragDropStage> stages = bentoLayout.getDragDropStages();

		assertThat(stages)
				.describedAs("bentoLayout.getDragDropStages()")
				.hasSize(3);
		assertThat(bentoLayout.wasShowing(stages.get(0)))
				.describedAs("wasShowing(stage persisted as not showing)")
				.isFalse();
		assertThat(bentoLayout.wasShowing(stages.get(1)))
				.describedAs("wasShowing(stage persisted as showing)")
				.isTrue();
		// An absent flag counts as showing, so a layout written before this was
		// honoured keeps restoring its detached windows.
		assertThat(bentoLayout.wasShowing(stages.get(2)))
				.describedAs("wasShowing(stage with no persisted showing flag)")
				.isTrue();
	}

	@Test
	void restoreDockingLayoutAppliesTheLeafMenuFactoryWhenProviderSupplied(
			final FxRobot robot
	) {
		final DockContainerLeafMenuFactory menuFactory = container -> null;
		final AtomicReference<DockContainerLeaf> leafReference =
				new AtomicReference<>();

		robot.interact(() -> {
			final DockContainerLeafStateBuilder leafStateBuilder =
					new DockContainerLeafStateBuilder(LEAF_ID);

			final DockContainerRootBranchStateBuilder rootBranchStateBuilder =
					new DockContainerRootBranchStateBuilder(ROOT_BRANCH_ID);
			rootBranchStateBuilder.addDockContainerState(leafStateBuilder.build());

			final BentoState bentoState =
					new BentoState.BentoStateBuilder(BENTO_ID)
							.addRootBranchState(rootBranchStateBuilder.build())
							.build();

			final DockingLayout dockingLayout =
					new DefaultDockingLayoutRebuilder(
							new DefaultBentoProvider(new Bento(BENTO_ID)),
							actualId -> Optional.empty(),
							null,
							leafIdentifier -> leafIdentifier.equals(LEAF_ID)
									? Optional.of(menuFactory)
									: Optional.empty()
					).rebuild(List.of(bentoState));

			final DockContainerRootBranch rootBranch =
					dockingLayout.getBentoLayouts()
							.getFirst()
							.getRootBranches()
							.getFirst();
			leafReference.set(
					getOnlyChildContainer(rootBranch, DockContainerLeaf.class)
			);
		});

		assertThat(leafReference.get().getMenuFactory())
				.describedAs("leaf.getMenuFactory()")
				.isSameAs(menuFactory);
	}

	/**
	 * The dockable is built from the identifier that was asked for, not from
	 * the resolved state's own - see {@code restoreDockable}'s javadoc.
	 */
	@Test
	void restoreDockingLayoutAppliesTheStateUnderTheRequestedIdentifierWhenProviderAnswersWithADifferentOne(
			final FxRobot robot
	) {
		final AtomicReference<Dockable> dockableReference =
				new AtomicReference<>();

		robot.interact(() -> {
			final DockableState requestedDockableState =
					new DockableStateBuilder(DOCKABLE_ID).build();
			final DockableState mismatchedDockableState =
					new DockableStateBuilder("some-other-identifier")
							.setTitle(DOCKABLE_TITLE)
							.build();

			final DockContainerLeafStateBuilder leafStateBuilder =
					new DockContainerLeafStateBuilder(LEAF_ID);
			leafStateBuilder.addChildDockableState(requestedDockableState);

			final DockContainerRootBranchStateBuilder rootBranchStateBuilder =
					new DockContainerRootBranchStateBuilder(ROOT_BRANCH_ID);
			rootBranchStateBuilder.addDockContainerState(leafStateBuilder.build());

			final BentoState bentoState =
					new BentoState.BentoStateBuilder(BENTO_ID)
							.addRootBranchState(rootBranchStateBuilder.build())
							.build();

			final DockingLayout dockingLayout =
					new DefaultDockingLayoutRebuilder(
							new DefaultBentoProvider(new Bento(BENTO_ID)),
							actualId -> Optional.of(mismatchedDockableState),
							null,
							null
					).rebuild(List.of(bentoState));

			final DockContainerRootBranch rootBranch =
					dockingLayout.getBentoLayouts()
							.getFirst()
							.getRootBranches()
							.getFirst();
			final DockContainerLeaf leaf =
					getOnlyChildContainer(rootBranch, DockContainerLeaf.class);
			dockableReference.set(leaf.getDockables().getFirst());
		});

		assertThat(dockableReference.get().getIdentifier())
				.describedAs("dockable.getIdentifier()")
				.isEqualTo(DOCKABLE_ID);
		assertThat(dockableReference.get().getTitle())
				.describedAs("dockable.getTitle()")
				.isEqualTo(DOCKABLE_TITLE);
	}

	/**
	 * {@code setContainerCollapsed} refuses to collapse a leaf with no
	 * sibling to divide space with, so a leaf persisted as collapsed but
	 * restored as the sole child of its branch stays uncollapsed.
	 */
	@Test
	void restoreDockingLayoutLeavesASoleLeafUncollapsedWhenPersistedAsCollapsed(
			final FxRobot robot
	) {
		final AtomicReference<DockContainerLeaf> leafReference =
				new AtomicReference<>();

		robot.interact(() -> {
			final DockContainerLeafStateBuilder leafStateBuilder =
					new DockContainerLeafStateBuilder(LEAF_ID);
			leafStateBuilder.setSide(LEFT);
			leafStateBuilder.setCollapsed(true);

			final DockContainerBranchStateBuilder branchStateBuilder =
					new DockContainerBranchStateBuilder(BRANCH_ID);
			branchStateBuilder.addDockContainerState(leafStateBuilder.build());

			final DockContainerRootBranchStateBuilder rootBranchStateBuilder =
					new DockContainerRootBranchStateBuilder(ROOT_BRANCH_ID);
			rootBranchStateBuilder.addDockContainerState(branchStateBuilder.build());

			final BentoState bentoState =
					new BentoState.BentoStateBuilder(BENTO_ID)
							.addRootBranchState(rootBranchStateBuilder.build())
							.build();

			final DockingLayout dockingLayout =
					new DefaultDockingLayoutRebuilder(
							new DefaultBentoProvider(new Bento(BENTO_ID)),
							actualId -> Optional.empty(),
							null,
							null
					).rebuild(List.of(bentoState));

			final DockContainerRootBranch rootBranch =
					dockingLayout.getBentoLayouts()
							.getFirst()
							.getRootBranches()
							.getFirst();
			final DockContainerBranch branch =
					getOnlyChildContainer(rootBranch, DockContainerBranch.class);
			leafReference.set(
					getOnlyChildContainer(branch, DockContainerLeaf.class)
			);
		});

		assertThat(leafReference.get().isCollapsed())
				.describedAs("leaf.isCollapsed() after a collapse setContainerCollapsed refused")
				.isFalse();
	}

	private static DragDropStageState dragDropStageState(
			final String suffix,
			final @Nullable Boolean isShowing
	) {
		return new DragDropStageStateBuilder(true)
				.setShowing(isShowing)
				.setDockContainerRootBranchState(
						new DockContainerRootBranchStateBuilder(
								ROOT_BRANCH_ID + "-" + suffix
						).build()
				)
				.build();
	}

	private static RestoredDockingLayout restoreNestedDockingLayout(
			final FxRobot robot
	) {
		final AtomicBoolean dockableConsumerCalled = new AtomicBoolean();
		final AtomicReference<DockingLayout> dockingLayoutReference =
				new AtomicReference<>();

		robot.interact(() -> {
			final DockableState dockableState =
					new DockableStateBuilder(DOCKABLE_ID)
							.setTitle(DOCKABLE_TITLE)
							.setTooltipText(DOCKABLE_TOOLTIP_TEXT)
							.setDockableNode(new Label(DOCKABLE_ID))
							.setClosable(false)
							.setDockableConsumer(ignored -> dockableConsumerCalled.set(true))
							.build();

			final BentoState bentoState = createNestedBentoState(dockableState);

			dockingLayoutReference.set(
					new DefaultDockingLayoutRebuilder(
							new DefaultBentoProvider(new Bento(BENTO_ID)),
							actualId -> actualId.equals(DOCKABLE_ID)
									? Optional.of(dockableState)
									: Optional.empty(),
							null,
							null
					).rebuild(List.of(bentoState))
			);
		});

		return new RestoredDockingLayout(
				dockingLayoutReference.get(),
				dockableConsumerCalled.get()
		);
	}

	private static BentoState createNestedBentoState(
			final DockableState dockableState
	) {
		final DockContainerLeafStateBuilder leafStateBuilder =
				new DockContainerLeafStateBuilder(LEAF_ID);
		leafStateBuilder.setSide(LEFT);
		leafStateBuilder.setCanSplit(false);
		leafStateBuilder.setResizableWithParent(false);
		leafStateBuilder.setSelectedDockableStateIdentifier(DOCKABLE_ID);
		leafStateBuilder.setPruneWhenEmpty(false);
		leafStateBuilder.addChildDockableState(dockableState);

		final DockContainerBranchStateBuilder branchStateBuilder =
				new DockContainerBranchStateBuilder(BRANCH_ID);
		branchStateBuilder.setOrientation(VERTICAL);
		branchStateBuilder.setPruneWhenEmpty(true);
		branchStateBuilder.addDockContainerState(leafStateBuilder.build());

		final DockContainerRootBranchStateBuilder rootBranchStateBuilder =
				new DockContainerRootBranchStateBuilder(ROOT_BRANCH_ID);
		rootBranchStateBuilder.setOrientation(HORIZONTAL);
		rootBranchStateBuilder.setPruneWhenEmpty(false);
		rootBranchStateBuilder.addDockContainerState(branchStateBuilder.build());

		return new BentoState.BentoStateBuilder(BENTO_ID)
				.addRootBranchState(rootBranchStateBuilder.build())
				.build();
	}

	private static void assertDockingLayout(final DockingLayout dockingLayout) {
		assertThat(dockingLayout.getBentoLayouts())
				.describedAs("dockingLayout.getBentoLayouts()")
				.hasSize(1);
	}

	private static void assertBentoLayout(final BentoLayout bentoLayout) {
		assertThat(bentoLayout.getIdentifier())
				.describedAs("bentoLayout.getIdentifier()")
				.isEqualTo(BENTO_ID);
		assertThat(bentoLayout.getRootBranches())
				.describedAs("bentoLayout.getRootBranches()")
				.hasSize(1);
		assertThat(bentoLayout.getDragDropStages())
				.describedAs("bentoLayout.getDragDropStages()")
				.isEmpty();
	}

	private static void assertRootBranch(
			final DockContainerRootBranch rootBranch
	) {
		assertThat(rootBranch.getIdentifier())
				.describedAs("rootBranch.getIdentifier()")
				.isEqualTo(ROOT_BRANCH_ID);
		assertThat(rootBranch.getOrientation())
				.describedAs("rootBranch.getOrientation()")
				.isEqualTo(HORIZONTAL);
		assertThat(rootBranch.doPruneWhenEmpty())
				.describedAs("rootBranch.doPruneWhenEmpty()")
				.isFalse();
		assertThat(rootBranch.getChildContainers())
				.describedAs("rootBranch.getChildContainers()")
				.hasSize(1);
	}

	private static void assertBranch(final DockContainerBranch branch) {
		assertThat(branch.getIdentifier())
				.describedAs("branch.getIdentifier()")
				.isEqualTo(BRANCH_ID);
		assertThat(branch.orientationProperty().get())
				.describedAs("branch.orientationProperty().get()")
				.isEqualTo(VERTICAL);
		assertThat(branch.doPruneWhenEmpty())
				.describedAs("branch.doPruneWhenEmpty()")
				.isTrue();
		assertThat(branch.getChildContainers())
				.describedAs("branch.getChildContainers()")
				.hasSize(1);
	}

	private static void assertLeaf(final DockContainerLeaf leaf) {
		assertThat(leaf.getIdentifier())
				.describedAs("leaf.getIdentifier()")
				.isEqualTo(LEAF_ID);
		assertThat(leaf.getSide())
				.describedAs("leaf.getSide()")
				.isEqualTo(LEFT);
		assertThat(leaf.isCanSplit())
				.describedAs("leaf.isCanSplit()")
				.isFalse();
		assertThat(SplitPane.isResizableWithParent(leaf))
				.describedAs("SplitPane.isResizableWithParent(leaf)")
				.isFalse();
		assertThat(leaf.doPruneWhenEmpty())
				.describedAs("leaf.doPruneWhenEmpty()")
				.isFalse();
		assertThat(leaf.getDockables())
				.describedAs("leaf.getDockables()")
				.hasSize(1);
	}

	private static void assertDockable(final Dockable dockable) {
		assertThat(dockable.getIdentifier())
				.describedAs("dockable.getIdentifier()")
				.isEqualTo(DOCKABLE_ID);
		assertThat(dockable.getTitle())
				.describedAs("dockable.getTitle()")
				.isEqualTo(DOCKABLE_TITLE);
		assertThat(dockable.getTooltip())
				.describedAs("dockable.getTooltip()")
				.isInstanceOf(Tooltip.class);
		assertThat(requireNonNull(dockable.getTooltip()).getText())
				.describedAs("dockable.getTooltip().getText()")
				.isEqualTo(DOCKABLE_TOOLTIP_TEXT);
		assertThat(dockable.isClosable())
				.describedAs("dockable.isClosable()")
				.isFalse();
	}

	private static void assertSelectedDockable(
			final DockContainerLeaf leaf,
			final Dockable dockable
	) {
		assertThat(leaf.getSelectedDockable())
				.describedAs("leaf.getSelectedDockable()")
				.isSameAs(dockable);
	}

	private static <T extends DockContainer> T getOnlyChildContainer(
			final DockContainerBranch branch,
			final Class<T> childContainerType
	) {
		final DockContainer childContainer =
				branch.getChildContainers().getFirst();
		assertThat(childContainer)
				.describedAs("childContainer")
				.isInstanceOf(childContainerType);
		return childContainerType.cast(childContainer);
	}

	private record RestoredDockingLayout(
			DockingLayout dockingLayout,
			boolean dockableConsumerCalled
	) {
	}
}
