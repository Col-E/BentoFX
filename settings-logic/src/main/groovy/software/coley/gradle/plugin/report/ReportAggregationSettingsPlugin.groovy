package software.coley.gradle.plugin.report

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.IsolatedAction
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

/**
 * Project isolation-safe plugin to determine paths for the projects used for
 * report aggregation.
 *
 * <p><b>Uses IsolatedAction, which are all incubating as of Gradle 9.7.1,
 * so this plugin can break on a Gradle upgrade even though nothing in the
 * build changed.</b></p>
 *
 * <p>The paths are computed once the settings have been evaluated and handed to
 * the {@code :report-aggregation} project as the
 * {@value #PROJECT_PATHS_PROPERTY} extra property. Handing them over through
 * {@link org.gradle.api.invocation.Gradle#getLifecycle()} rather than through an
 * extension on {@code Gradle} keeps this compatible with Isolated Projects,
 * which forbids a project from reading shared build state.
 */
@SuppressWarnings("unused")
@CompileStatic
class ReportAggregationSettingsPlugin implements Plugin<Settings> {

	/**
	 * Name of the extra property carrying the aggregated project paths, set on
	 * {@value #REPORT_AGGREGATION_PROJECT_PATH} only. Its value is an immutable
	 * {@code List<String>} of project paths, sorted.
	 */
	static final String PROJECT_PATHS_PROPERTY = 'reportAggregationProjects'

	private static final String REPORT_AGGREGATION_PROJECT_PATH = ':report-aggregation'

	@Override
	void apply(final Settings settings) {
		settings.gradle.settingsEvaluated(new Action<Settings>() {
			@Override
			void execute(final Settings evaluatedSettings) {
				final List<String> aggregatedPaths = evaluatedSettings.rootProject.children
						.collectMany { ProjectDescriptor descriptor -> walk(descriptor) }
						.findAll { ProjectDescriptor descriptor ->
							descriptor.path != REPORT_AGGREGATION_PROJECT_PATH &&
									!descriptor.path.startsWith(':demos') &&
									!descriptor.path.endsWith(':test-fixtures') &&
									descriptor.children.empty &&
									hasMainJvmSources(descriptor.projectDir)
						}
						.collect { ProjectDescriptor descriptor -> descriptor.path }
						.sort()

				evaluatedSettings.gradle.lifecycle.beforeProject(
						new ExposeAggregatedProjectPaths(aggregatedPaths)
				)
			}
		})
	}

	/**
	 * Sets the aggregated project paths on the report aggregation project.
	 *
	 * <p>Isolated Projects isolates every action registered against
	 * {@code GradleLifecycle}, so this is a static class holding nothing but an
	 * immutable list of strings: a closure would capture the enclosing plugin
	 * and fail to isolate. IsolatedAction are all incubating as of Gradle 9.7.1,
	 * so this plugin can break on a Gradle upgrade even though nothing in the
	 * build changed.</p>
	 *
	 * <p>IsolatedAction are all incubating as of Gradle 9.7.1, so this
	 * implementation can break on a Gradle upgrade even though nothing in the
	 * build changed.</p>
	 */
	@CompileStatic
	private static class ExposeAggregatedProjectPaths implements IsolatedAction<Project> {

		private final List<String> projectPaths

		ExposeAggregatedProjectPaths(final List<String> projectPaths) {
			this.projectPaths = List.copyOf(projectPaths)
		}

		// This method, defined in IsolatedAction, is incubating as of
		// Gradle 9.7.1
		@SuppressWarnings("UnstableApiUsage")
		@Override
		void execute(final Project project) {
			if (project.path == REPORT_AGGREGATION_PROJECT_PATH) {
				project.extensions.extraProperties.set(
						PROJECT_PATHS_PROPERTY,
						projectPaths
				)
			}
		}
	}

	private static List<ProjectDescriptor> walk(final ProjectDescriptor descriptor) {
		final List<ProjectDescriptor> descriptors = [descriptor]
		descriptor.children.each { ProjectDescriptor child ->
			descriptors.addAll(walk(child))
		}
		return descriptors
	}

	private static boolean hasMainJvmSources(final File projectDir) {
		return containsFiles(new File(projectDir, 'src/main/kotlin'), 'kt') ||
				containsFiles(new File(projectDir, 'src/main/java'), 'java')
	}

	private static boolean containsFiles(final File dir, final String extension) {
		if (!dir.directory) {
			return false
		}

		try (Stream<Path> paths = Files.walk(dir.toPath())) {
			return paths.anyMatch { Path path ->
				Files.isRegularFile(path) && path.fileName.toString().endsWith(".${extension}".toString())
			}
		}
	}
}
