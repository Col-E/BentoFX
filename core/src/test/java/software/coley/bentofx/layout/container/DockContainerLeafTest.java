package software.coley.bentofx.layout.container;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import software.coley.bentofx.Bento;
import software.coley.bentofx.building.DockBuilding;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage for {@link DockContainerLeaf#toggleCollapse(software.coley.bentofx.dockable.Dockable)}.
 *
 * <p>A leaf has no parent until it is added to a branch or root container.
 * {@code toggleCollapse} dereferenced that parent unconditionally, so toggling
 * an unparented leaf threw {@code NullPointerException} instead of reporting
 * that there is nothing to collapse against.</p>
 *
 * <p>This module has no separate graphical-integration-test suite to isolate this
 * in, so the {@code @ResourceLock} below excludes it from the {@code test} suite's
 * default parallel execution instead - the JavaFX toolkit {@link ApplicationExtension}
 * starts is a single JVM-wide instance, and its per-class setup/teardown is not
 * designed to run concurrently with another test doing the same.</p>
 *
 * @author Phil Bryant
 */
@ExtendWith(ApplicationExtension.class)
@ResourceLock(value = "javafx-toolkit", mode = ResourceAccessMode.READ_WRITE)
class DockContainerLeafTest {

    private static final String LEAF_ID = "leaf-toggle-collapse";

    /**
     * The defect: an unparented leaf must report its own (unchanged) collapsed
     * state rather than throw.
     */
    @Test
    void toggleCollapseReturnsFalseRatherThanThrowingWhenLeafHasNoParent(final FxRobot robot) {
        robot.interact(() -> {
            final Bento bento = new Bento();
            final DockBuilding dockBuilding = bento.dockBuilding();

            final DockContainerLeaf leaf = dockBuilding.leaf(LEAF_ID);

            assertThat(leaf.toggleCollapse(null))
                    .describedAs("toggleCollapse result for an unparented leaf")
                    .isFalse();
        });
    }
}
