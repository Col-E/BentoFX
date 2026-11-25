package software.coley.gradle.artifacts

import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.internal.os.OperatingSystem

/**
 * Normalizes JavaFX-adjacent dependency metadata
 */
@SuppressWarnings("unused") // Used in bento.project.project-convention.gradle
abstract class JavaFxDependencyMetadataRule implements ComponentMetadataRule {
	private static final String OPENJFX_GROUP = 'org.openjfx'
	private static final String OPENJFX_ARTIFACT_PREFIX = 'javafx-'
	private static final String TESTFX_GROUP = 'org.testfx'
	private static final String TESTFX_MONOCLE_ARTIFACT = 'openjfx-monocle'
	private static final String TESTFX_VIRTUAL_BOM_GROUP = 'org.testfx'
	private static final String TESTFX_VIRTUAL_BOM_ARTIFACT = 'testfx-virtual-bom'

	@Override
	void execute(final ComponentMetadataContext context) {
		alignTestFxModules(context)
		selectOpenJfxPlatformArtifact(context)
	}

	protected static void alignTestFxModules(final ComponentMetadataContext context) {
		final ModuleVersionIdentifier id = context.details.id
		if (id.group == TESTFX_GROUP && id.name != TESTFX_MONOCLE_ARTIFACT) {
			context.details.belongsTo("${TESTFX_VIRTUAL_BOM_GROUP}:${TESTFX_VIRTUAL_BOM_ARTIFACT}:${id.version}")
		}
	}

	protected void selectOpenJfxPlatformArtifact(final ComponentMetadataContext context) {
		final ModuleVersionIdentifier id = context.details.id
		if (id.group != OPENJFX_GROUP || !id.name.startsWith(OPENJFX_ARTIFACT_PREFIX)) {
			return
		}

		final String platformClassifier = currentPlatformClassifier()
		usePlatformArtifact(context, 'compile', platformClassifier)
		usePlatformArtifact(context, 'runtime', platformClassifier)
	}

	protected void usePlatformArtifact(
			final ComponentMetadataContext context,
			final String variantName,
			final String platformClassifier
	) {
		context.details.withVariant(variantName) {
			withFiles {
				removeAllFiles()
				addFile("${context.details.id.name}-${context.details.id.version}-${platformClassifier}.jar")
			}
		}
	}

	protected static String currentPlatformClassifier() {
		final OperatingSystem operatingSystem = OperatingSystem.current()
		if (operatingSystem.isWindows()) {
			return 'win'
		}
		if (operatingSystem.isMacOsX()) {
			return 'mac'
		}
		return 'linux'
	}
}
