package software.coley.gradle.task.extensions

import org.gradle.api.provider.Provider

import javax.inject.Inject

/**
 * Extension class for sharing lazily calculated values among precompiled
 * convention script plugins.
 */
abstract class BentoConventionExtension {
    /**
     * The fully qualified project name, normalized as a valid for use as an
     * artifact ID, JAR file name, etc.
     */
    final Provider<String> normalizedProjectName

    /**
     * @param normalizedProjectName The fully qualified project name,
     * normalized as a valid for use as an artifact ID, JAR file name, etc.
     */
    @Inject
    BentoConventionExtension(final Provider<String> normalizedProjectName) {
        this.normalizedProjectName = normalizedProjectName
    }
}
