package software.coley.gradle.artifacts

import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule

abstract class TestFxAlignmentRule implements ComponentMetadataRule {
    @Override
    void execute(ComponentMetadataContext ctx) {
        def id = ctx.details.id
        if (id.group == 'org.testfx' && id.name != 'openjfx-monocle') {
            ctx.details.belongsTo("org.testfx:testfx-virtual-bom:${id.version}")
        }
    }
}
