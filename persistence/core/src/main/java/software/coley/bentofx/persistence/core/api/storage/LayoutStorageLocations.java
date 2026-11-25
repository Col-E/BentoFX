package software.coley.bentofx.persistence.core.api.storage;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

/**
 * Where a {@link LayoutStorage} implementation that keeps its data on this
 * machine - rather than, say, a remote database - should put it by default.
 *
 * <p>Every storage provider included with this framework is discovered by
 * {@code ServiceLoader}, which requires a public no-arg constructor or a
 * public static {@code provider()} factory: neither can take a caller's
 * configuration as an argument. This class is the alternative - ambient state
 * a provider reads for itself, the same way it already reads {@code user.home} -
 * so an application can still choose where its data goes and, when more than
 * one BentoFX-based application runs on the same machine, give each one a
 * directory of its own rather than have them overwrite each other's layouts
 * under one shared default.</p>
 *
 * <p>Each setting can be given either as a system property or as an
 * environment variable, checked in that order - a system property always wins
 * over an environment variable of the same name. An environment variable
 * needs no application code at all: set it before the process starts and this
 * class picks it up the first time a storage provider asks. Both are read
 * fresh on every call rather than cached, so that setting a property -
 * directly with {@link System#setProperty}, with a {@code -D} JVM flag, or
 * through {@link #configureHome} and {@link #configureNamespace} - takes
 * effect immediately as long as it happens before the first save, restore, or
 * catalog call. Calling either after a storage provider has already been used
 * has no effect on what that provider already opened.</p>
 *
 * @author Phil Bryant
 */
public final class LayoutStorageLocations {

	/**
	 * System property overriding the directory this framework's data lives
	 * under. Unset by default, in which case {@value #HOME_DIRECTORY_ENV_VARIABLE}
	 * is checked next, and if that is unset too, {@code user.home} plus
	 * {@value #BENTOFX_DIRECTORY_NAME} is used instead.
	 */
	public static final String HOME_DIRECTORY_PROPERTY = "bentofx.persistence.home";

	/**
	 * Environment variable checked when {@value #HOME_DIRECTORY_PROPERTY} is
	 * not set. Needs no application code: set it before launching the
	 * process.
	 */
	public static final String HOME_DIRECTORY_ENV_VARIABLE = "BENTOFX_PERSISTENCE_HOME";

	/**
	 * System property naming a subdirectory of the resolved home that this
	 * application's data lives under, distinguishing it from another
	 * BentoFX-based application's data. Unset by default, in which case
	 * {@value #NAMESPACE_ENV_VARIABLE} is checked next, and if that is unset
	 * too, nothing is inserted and every application shares one directory.
	 */
	public static final String NAMESPACE_PROPERTY = "bentofx.persistence.namespace";

	/**
	 * Environment variable checked when {@value #NAMESPACE_PROPERTY} is not
	 * set. Needs no application code: set it before launching the process.
	 */
	public static final String NAMESPACE_ENV_VARIABLE = "BENTOFX_PERSISTENCE_NAMESPACE";

	/**
	 * The directory name this framework's data lives under, below the
	 * resolved base directory. Public so that anything needing to describe or
	 * locate that directory - a test, a diagnostic, an uninstaller - has one
	 * place to read it from rather than repeating the literal.
	 */
	public static final String BENTOFX_DIRECTORY_NAME = ".bentofx";

	/**
	 * The JDK system property this class resolves the default base directory
	 * from. Public for the same reason as {@link #BENTOFX_DIRECTORY_NAME}:
	 * one place to read the name from instead of repeating the literal.
	 */
	public static final String USER_HOME_PROPERTY = "user.home";

	private LayoutStorageLocations() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * {@return the absolute, normalized directory this framework's storage
	 * providers should keep their data under.}
	 *
	 * <p>{@code <user.home>/.bentofx}, unless {@value #HOME_DIRECTORY_PROPERTY}
	 * (or {@value #HOME_DIRECTORY_ENV_VARIABLE}) overrides the base directory
	 * and/or {@value #NAMESPACE_PROPERTY} (or {@value #NAMESPACE_ENV_VARIABLE})
	 * names a subdirectory of it.</p>
	 *
	 * @throws IllegalArgumentException when the resolved namespace setting
	 * cannot name a single directory - see
	 * {@link LayoutIdentifiers#findUserLayoutProblem(String)} for the exact
	 * rule.
	 */
	public static Path resolveBentoFxHome() {
		final String homeOverride = readSetting(HOME_DIRECTORY_PROPERTY, HOME_DIRECTORY_ENV_VARIABLE);

		final Path home = homeOverride != null
				? Path.of(homeOverride)
				: Path.of(System.getProperty(USER_HOME_PROPERTY), BENTOFX_DIRECTORY_NAME);

		final String namespace = readSetting(NAMESPACE_PROPERTY, NAMESPACE_ENV_VARIABLE);

		final Path bentoFxHome = namespace != null && !namespace.isBlank()
				? home.resolve(requireUsableNamespace(namespace))
				: home;

		return bentoFxHome.toAbsolutePath().normalize();
	}

	/**
	 * {@return the value of {@code systemProperty}, or {@code environmentVariable}
	 * when the property is not set, or {@code null} when neither is.}
	 *
	 * @param systemProperty the system property to check first.
	 * @param environmentVariable the environment variable to fall back to.
	 */
	static @Nullable String readSetting(
			final String systemProperty,
			final String environmentVariable
	) {
		return readSetting(systemProperty, environmentVariable, System::getenv);
	}

	/**
	 * {@return the value of {@code systemProperty}, or {@code envLookup}'s
	 * response for {@code environmentVariable} when the property is not set,
	 * or {@code null} when neither is.}
	 *
	 * <p>Package-private seam: tests pass a map-backed {@code envLookup} to
	 * exercise the fallback branch deterministically on every platform,
	 * without touching the real process environment - which a JVM cannot
	 * change once it is already running.</p>
	 *
	 * @param systemProperty the system property to check first.
	 * @param environmentVariable the environment variable name to pass to {@code envLookup}.
	 * @param envLookup the function that resolves environment variable names to values.
	 */
	static @Nullable String readSetting(
			final String systemProperty,
			final String environmentVariable,
			final Function<String, @Nullable String> envLookup
	) {
		final String propertyValue = System.getProperty(systemProperty);
		return propertyValue != null ? propertyValue : envLookup.apply(environmentVariable);
	}

	/**
	 * Sets {@value #HOME_DIRECTORY_PROPERTY}, a typed alternative to calling
	 * {@link System#setProperty} directly.
	 *
	 * @param home the directory this framework's storage providers should
	 * keep their data under, in place of {@code <user.home>/.bentofx}.
	 */
	public static void configureHome(final Path home) {
		System.setProperty(
				HOME_DIRECTORY_PROPERTY,
				Objects.requireNonNull(home, "home").toString()
		);
	}

	/**
	 * Sets {@value #NAMESPACE_PROPERTY}, a typed alternative to calling
	 * {@link System#setProperty} directly.
	 *
	 * @param namespace the subdirectory of the resolved home this
	 * application's data should live under.
	 * @throws IllegalArgumentException when {@code namespace} cannot name a
	 * single directory - see
	 * {@link LayoutIdentifiers#findUserLayoutProblem(String)} for the exact
	 * rule.
	 */
	public static void configureNamespace(final String namespace) {
		System.setProperty(
				NAMESPACE_PROPERTY,
				requireUsableNamespace(Objects.requireNonNull(namespace, "namespace"))
		);
	}

	/**
	 * {@return {@code namespace}, unchanged, once it has been confirmed usable.}
	 *
	 * <p>Reuses {@link LayoutIdentifiers#findUserLayoutProblem(String)} rather
	 * than a namespace-specific check: a namespace becomes a single path
	 * component exactly the way a layout identifier does, so the same rule
	 * applies.</p>
	 *
	 * @param namespace the value to check.
	 * @throws IllegalArgumentException when {@code namespace} cannot name a
	 * single directory.
	 */
	private static String requireUsableNamespace(final String namespace) {
		LayoutIdentifiers.findUserLayoutProblem(namespace).ifPresent(problem -> {
			throw new IllegalArgumentException(
					NAMESPACE_PROPERTY + " must be usable as a directory name (violated "
							+ problem.rule() + "), but was '" + namespace + "'."
			);
		});

		return namespace;
	}
}
