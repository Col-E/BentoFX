package software.coley.gradle.plugin.analysis

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/**
 * Applies and centralizes settings-level dependency-analysis configuration.
 */
@SuppressWarnings("unused")
@CompileStatic
class DagpSettingsPlugin implements Plugin<Settings> {

	@Override
	void apply(final Settings settings) {
		settings.pluginManager.apply('com.autonomousapps.build-health')
		configureDependencyAnalysis(settings)
	}

	@CompileDynamic
	private static void configureDependencyAnalysis(final Settings settings) {
		settings.extensions.configure('dependencyAnalysis') { extension ->
			extension.usage {
				analysis {
					checkSuperClasses(true)
				}
			}
			extension.useTypesafeProjectAccessors(true)
		}
	}
}
