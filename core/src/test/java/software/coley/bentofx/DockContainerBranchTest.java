package software.coley.bentofx;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.bentofx.layout.container.DockContainerBranch;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DockContainerBranchTest {

	@BeforeAll
	static void startToolkit() throws Exception {
		FutureTask<Void> startup = new FutureTask<>(() -> {
			Platform.setImplicitExit(false);
			return null;
		});
		Platform.startup(startup);
		startup.get(10, TimeUnit.SECONDS);
	}

	@Test
	void indexedInsertionKeepsModelAndVisualOrderAligned() throws Exception {
		onFxThread(() -> {
			var building = new Bento().dockBuilding();
			var root = building.root("root");
			var center = building.branch("center");
			var first = building.branch("first");
			var middle = building.branch("middle");
			var last = building.branch("last");
			root.addContainer(center);
			root.addContainer(0, first);
			root.addContainer(1, middle);
			root.addContainer(last);

			assertThat(root.getChildContainers()).containsExactly(first, middle, center, last);
			assertAligned(root);
			assertThat(middle.getParentContainer()).isSameAs(root);
		});
	}

	@Test
	void pruningAnInsertedBranchPreservesItsSibling() throws Exception {
		onFxThread(() -> {
			var building = new Bento().dockBuilding();
			var root = building.root("root");
			var center = building.branch("center");
			var sidebar = building.branch("sidebar");
			root.addContainer(center);
			root.addContainer(0, sidebar);
			var retained = building.leaf("retained");
			var removed = building.leaf("removed");
			sidebar.addContainers(retained, removed);
			assertThat(sidebar.removeContainer(removed)).isTrue();

			assertThat(root.getChildContainers()).containsExactly(retained, center);
			assertAligned(root);
			assertThat(retained.getParentContainer()).isSameAs(root);
			assertThat(center.getParentContainer()).isSameAs(root);
			assertThat(sidebar.getParentContainer()).isNull();
		});
	}

	private static void assertAligned(DockContainerBranch branch) {
		assertThat(branch.getItems()).containsExactlyElementsOf(
				branch.getChildContainers().stream().map(container -> container.asRegion()).toList());
	}

	private static void onFxThread(Runnable action) throws Exception {
		FutureTask<Void> task = new FutureTask<>(action, null);
		Platform.runLater(task);
		task.get(10, TimeUnit.SECONDS);
	}
}
