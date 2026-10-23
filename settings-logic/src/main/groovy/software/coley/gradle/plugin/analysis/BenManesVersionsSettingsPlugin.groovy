package software.coley.gradle.plugin.analysis

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/**
 * Applies and centralizes settings-level dependency-analysis configuration.
 */
@SuppressWarnings("unused")
@CompileStatic
class BenManesVersionsSettingsPlugin implements Plugin<Settings> {

	@Override
	void apply(final Settings settings) {
		settings.pluginManager.apply('io.github.ben-manes.versions.settings')
	}
}
