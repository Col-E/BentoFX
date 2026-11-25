package software.coley.gradle.buildservice

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

@SuppressWarnings("unused") // Used by convention script plugins
abstract class SimpleBuildService implements BuildService<BuildServiceParameters.None> {
}
