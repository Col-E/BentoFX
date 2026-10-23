package software.coley.bentofx.persistence.core.api.storage;

import org.jspecify.annotations.Nullable;
import software.coley.bentofx.persistence.core.api.storage.LayoutIdentifierProblem.Parameter;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static software.coley.bentofx.persistence.core.api.storage.LayoutIdentifierProblem.Parameter.*;
import static software.coley.bentofx.persistence.core.api.storage.LayoutIdentifierProblem.Rule.*;

/**
 * The rule for the two identifiers that name a stored docking layout, in one
 * place because a pair has to satisfy every {@link LayoutStorage}
 * implementation at once.
 *
 * <p>A layout is addressed by a layout identifier and a codec identifier.
 * Database-backed storage keeps them in two columns; file-backed storage joins
 * them into one file name. A pair that suits one and not the other is not a
 * usable pair, so the rule here is the intersection: what a column will hold,
 * and what a filesystem will accept as a single path component.</p>
 *
 * <p>Providers apply this where the application's strings arrive, so that a
 * layout which cannot be stored is refused at the call that named it rather than
 * at the save that needed it.</p>
 *
 * <p>The naming rules below are Windows' by origin, and deliberately applied
 * everywhere. A POSIX filesystem reserves no names at all and forbids only
 * {@code /} and a zero byte, so a rule wide enough for Windows is wide enough for
 * Linux and macOS - while a rule that only held on the platform it was written on
 * would let an application name a layout it cannot restore on the next machine
 * it runs on.</p>
 *
 * @author Phil Bryant
 * @see <a href="https://learn.microsoft.com/en-us/windows/win32/fileio/naming-a-file">
 * Naming Files, Paths, and Namespaces</a>, which is where the reserved names and
 * characters come from.
 */
public final class LayoutIdentifiers {

	/**
	 * The most characters the two identifiers may take together, counting the
	 * separator between them.
	 *
	 * <p>File-backed storage joins the pair into one path component, and every
	 * mainstream filesystem stops at 255 characters per component. That is the
	 * tightest ceiling any implementation imposes, so it is the one they all
	 * publish - a database column wide enough for this is wide enough.</p>
	 */
	public static final int MAX_JOINED_LENGTH = 255;

	/**
	 * The layout an application saves to while it runs and restores when it
	 * starts.
	 *
	 * <p>Reserved, not invalid. Every operation accepts it, because saving to
	 * it, restoring it and deleting it are all things an application
	 * legitimately does; what is reserved is a user's freedom to take the name
	 * for a layout of their own. See {@link #isReserved(String)}.</p>
	 */
	public static final String SESSION_LAYOUT_IDENTIFIER = "session";

	/**
	 * The entry that records which groups exist.
	 */
	public static final String GROUP_CATALOG_LAYOUT_IDENTIFIER = "groups";

	/**
	 * The entry that records which named layout is active, so an application
	 * can show it selected again after a restart even though it restores the
	 * session layout rather than the named one directly.
	 */
	public static final String ACTIVE_LAYOUT_IDENTIFIER = "activeLayout";

	/**
	 * The entry that records how the default layout was arranged when it was
	 * last applied, so an application can tell whether what is showing is
	 * still the default layout, including after a restart.
	 */
	public static final String DEFAULT_LAYOUT_IDENTIFIER = "defaultLayout";

	/**
	 * Identifiers this framework has taken for itself.
	 *
	 * <p>Compared without case, because a file name is case-insensitive on
	 * Windows and macOS.</p>
	 */
	private static final Set<String> RESERVED_LAYOUT_IDENTIFIERS = Set.of(
			SESSION_LAYOUT_IDENTIFIER.toUpperCase(Locale.ROOT),
			GROUP_CATALOG_LAYOUT_IDENTIFIER.toUpperCase(Locale.ROOT),
			ACTIVE_LAYOUT_IDENTIFIER.toUpperCase(Locale.ROOT),
			DEFAULT_LAYOUT_IDENTIFIER.toUpperCase(Locale.ROOT)
	);

	/**
	 * Names that Windows resolves to a device rather than to a file, whichever
	 * directory they are used in. Matched against the identifier and against what
	 * precedes its first {@code .}, because {@code NUL.txt} and {@code NUL.tar.gz}
	 * are both the device.
	 *
	 * <p>The superscript forms are reserved as well: Windows reads the ISO/IEC
	 * 8859-1 superscript digits as digits, so {@code COM} followed by a superscript
	 * one is a device too. They are written as escapes to keep this source ASCII,
	 * because a literal mangled by a source encoding would silently stop
	 * matching.</p>
	 */
	private static final Set<String> RESERVED_DEVICE_NAMES = Set.of(
			"CON", "PRN", "AUX", "NUL",
			"COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
			"LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
			"COM\u00B9", "COM\u00B2", "COM\u00B3",
			"LPT\u00B9", "LPT\u00B2", "LPT\u00B3"
	);

	/**
	 * Characters no file name may contain. {@code /} and {@code \} are handled
	 * separately, so that a name containing one is reported as a path rather than
	 * as a bad character.
	 */
	private static final String FORBIDDEN_CHARACTERS = "<>:\"|?*";

	private static final Set<String> DIRECTORY_NAMES = Set.of(".", "..");

	private LayoutIdentifiers() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * {@return {@code true} when the identifier is one this framework has taken
	 * for itself; otherwise, {@code false}.}
	 *
	 * <p>Reserved identifiers are worth leaving out of a menu of layouts a user
	 * may restore. However, {@code getStoredLayoutIdentifiers} reports them
	 * like any other, because a catalog that hid a stored layout would
	 * misreport what the destination holds.</p>
	 *
	 * @param layoutIdentifier the identifier to test. Compared without regard
	 * to case.
	 */
	public static boolean isReserved(final String layoutIdentifier) {
		requireNonNull(layoutIdentifier, "layoutIdentifier");

		return RESERVED_LAYOUT_IDENTIFIERS.contains(
				layoutIdentifier.toUpperCase(Locale.ROOT)
		);
	}

	/**
	 * Accepts a pair of identifiers that every storage implementation can use,
	 * and rejects the rest.
	 *
	 * <p>The codec identifier may carry a leading {@code .}, since that is how
	 * an extension is often written and file-backed storage strips it.</p>
	 *
	 * @param layoutIdentifier identifies the layout. Additionally may not be a
	 * name reserved for a device, because file-backed storage makes it the
	 * beginning of a file name.
	 * @param codecIdentifier identifies the codec whose output is stored.
	 * @throws NullPointerException if either identifier is {@code null}, naming
	 * the one that was missing.
	 * @throws IllegalArgumentException if either identifier is blank, contains a
	 * path separator, names a directory, holds a character no file name may hold
	 * ({@code < > : " | ? *} or a control character), ends with a space or a
	 * period, or if the two together exceed {@link #MAX_JOINED_LENGTH}.
	 */
	public static void requireValid(
			final @Nullable String layoutIdentifier,
			final @Nullable String codecIdentifier
	) {
		// 'throw' at the call site rather than inside 'ifPresent', so that null
		// analysis (Sonar's S2259) can see that neither identifier is null after
		// this returns.
		final Optional<LayoutIdentifierProblem> problem =
				findProblem(layoutIdentifier, codecIdentifier);

		if (problem.isPresent()) {
			throw toException(problem.get());
		}
	}

	/**
	 * Accepts a layout identifier that every storage implementation can use, and
	 * rejects the rest.
	 *
	 * <p>Does not check whether the identifier is one this framework reserves,
	 * because reserved identifiers are legitimate for the layouts this framework
	 * writes itself. The {@link #findUserLayoutProblem(String)} overload adds the
	 * reserved check for user-chosen identifiers.</p>
	 *
	 * <p>Does not check joined length, because that rule needs the codec
	 * identifier the layout will pair with; storage implementations check it as
	 * the pair arrives via {@link #requireValid}.</p>
	 *
	 * @param layoutIdentifier identifies the layout. Additionally may not be a
	 * name reserved for a device, because file-backed storage makes it the
	 * beginning of a file name.
	 * @throws NullPointerException if the identifier is {@code null}.
	 * @throws IllegalArgumentException if the identifier is blank, contains a
	 * path separator, names a directory, holds a character no file name may hold
	 * ({@code < > : " | ? *} or a control character), ends with a space or a
	 * period, or is a name reserved for a device.
	 */
	public static void requireValidLayoutIdentifier(
			final @Nullable String layoutIdentifier
	) {
		final Optional<LayoutIdentifierProblem> problem = findProblem(layoutIdentifier);

		if (problem.isPresent()) {
			throw toException(problem.get());
		}
	}

	/**
	 * {@return why the layout identifier cannot be used, or an empty
	 * {@link Optional} when it can.}
	 *
	 * <p>Checks the format rules {@link #requireValid} applies to the layout
	 * identifier alone, without the joined-length rule that needs a codec and
	 * without the reserved-name rule for user-chosen identifiers. Never throws,
	 * including for {@code null}, which is reported as
	 * {@link LayoutIdentifierProblem.Rule#MISSING}.</p>
	 *
	 * @param layoutIdentifier identifies the layout.
	 */
	public static Optional<LayoutIdentifierProblem> findProblem(
			final @Nullable String layoutIdentifier
	) {
		final LayoutIdentifierProblem nameProblem =
				findUsableNameProblem(layoutIdentifier, LAYOUT_IDENTIFIER);

		if (nameProblem != null) {
			return Optional.of(nameProblem);
		}

		// Non-null once its own check has passed.
		final String layout = requireNonNull(layoutIdentifier);

		return Optional.ofNullable(findDeviceNameProblem(layout));
	}

	/**
	 * {@return why the pair cannot be used, or an empty {@link Optional} when it
	 * can.}
	 *
	 * <p>The same rules {@link #requireValid} enforces, reported rather than thrown,
	 * for a caller that is asking: a dialog validating what a user typed, or code
	 * choosing between identifiers. Never throws, including for {@code null}, which
	 * is reported as {@link LayoutIdentifierProblem.Rule#MISSING}.</p>
	 *
	 * <p>A reserved identifier is <em>not</em> reported here, because it is valid -
	 * see {@link #findUserLayoutProblem} for the check a "save as" dialog wants.</p>
	 *
	 * @param layoutIdentifier identifies the layout.
	 * @param codecIdentifier identifies the codec whose output is stored.
	 */
	public static Optional<LayoutIdentifierProblem> findProblem(
			final @Nullable String layoutIdentifier,
			final @Nullable String codecIdentifier
	) {
		final LayoutIdentifierProblem layoutProblem =
				findUsableNameProblem(layoutIdentifier, LAYOUT_IDENTIFIER);

		if (layoutProblem != null) {
			return Optional.of(layoutProblem);
		}

		final LayoutIdentifierProblem codecProblem =
				findUsableNameProblem(codecIdentifier, CODEC_IDENTIFIER);

		if (codecProblem != null) {
			return Optional.of(codecProblem);
		}

		// Both are non-null once their own checks have passed.
		final String layout = requireNonNull(layoutIdentifier);
		final String codec = requireNonNull(codecIdentifier);

		final LayoutIdentifierProblem deviceProblem = findDeviceNameProblem(layout);

		if (deviceProblem != null) {
			return Optional.of(deviceProblem);
		}

		return Optional.ofNullable(findJoinedLengthProblem(layout, codec));
	}

	/**
	 * {@return why the pair cannot be used for a layout a user named, or an empty
	 * {@link Optional} when it can.}
	 *
	 * <p>{@link #findProblem}'s rules plus
	 * {@link LayoutIdentifierProblem.Rule#RESERVED}, which is the one difference
	 * between an identifier an application chose and one a user did. This is the
	 * check for a "save as" dialog, and for whatever turns a display name into an
	 * identifier.</p>
	 *
	 * @param layoutIdentifier identifies the layout the user named.
	 * @param codecIdentifier identifies the codec whose output is stored.
	 */
	public static Optional<LayoutIdentifierProblem> findUserLayoutProblem(
			final @Nullable String layoutIdentifier,
			final @Nullable String codecIdentifier
	) {
		final Optional<LayoutIdentifierProblem> problem =
				findProblem(layoutIdentifier, codecIdentifier);

		// A null layoutIdentifier always reaches here with problem already
		// present - findProblem's own first check, findUsableNameProblem,
		// reports MISSING for it - so this can only ever be a non-null
		// identifier once problem.isPresent() is false.
		if (problem.isPresent()) {
			return problem;
		}

		return findReservedProblem(requireNonNull(layoutIdentifier));
	}

	/**
	 * {@return why the layout identifier alone cannot be used for a layout a
	 * user named, or an empty {@link Optional} when it can.}
	 *
	 * <p>Every rule {@link #findUserLayoutProblem(String, String)} applies
	 * except {@link LayoutIdentifierProblem.Rule#TOO_LONG}, which measures the
	 * two identifiers together and so cannot be judged from one of them. For an
	 * application that leaves the codec to this framework's own selection, that
	 * is the check a "save as" dialog can actually make: it has a name from the
	 * user and no codec identifier to pair it with.</p>
	 *
	 * <p>Skipping that one rule loses nothing that matters, because it is not
	 * this method a save relies on. Storage implementations call
	 * {@link #requireValid} as the pair arrives, with the codec identifier that
	 * was really selected, so a pair too long together is still refused there,
	 * with the right numbers. What is lost is only the earlier and friendlier
	 * telling.</p>
	 *
	 * @param layoutIdentifier identifies the layout the user named.
	 */
	public static Optional<LayoutIdentifierProblem> findUserLayoutProblem(
			final @Nullable String layoutIdentifier
	) {
		final LayoutIdentifierProblem nameProblem =
				findUsableNameProblem(layoutIdentifier, LAYOUT_IDENTIFIER);

		if (nameProblem != null) {
			return Optional.of(nameProblem);
		}

		// Non-null once its own check has passed.
		final String layout = requireNonNull(layoutIdentifier);

		final LayoutIdentifierProblem deviceProblem =
				findDeviceNameProblem(layout);

		if (deviceProblem != null) {
			return Optional.of(deviceProblem);
		}

		return findReservedProblem(layout);
	}

	/**
	 * {@return why the identifier is one this framework reserves, or an empty
	 * {@link Optional} when it is not.}
	 *
	 * <p>Shared by both {@code findUserLayoutProblem} overloads so the sentence
	 * a user reads for a reserved name is written once.</p>
	 *
	 * @param layoutIdentifier identifies the layout the user named.
	 */
	private static Optional<LayoutIdentifierProblem> findReservedProblem(
			final String layoutIdentifier
	) {
		if (!isReserved(layoutIdentifier)) {
			return Optional.empty();
		}

		return Optional.of(new LayoutIdentifierProblem(
				RESERVED,
				LAYOUT_IDENTIFIER,
				"layoutIdentifier must not be one this framework reserves, "
						+ "but was '" + layoutIdentifier + "'."
		));
	}

	/**
	 * {@return the exception a problem describes, for the caller to throw.}
	 *
	 * <p>A missing identifier is a {@link NullPointerException} naming the parameter,
	 * because that is what a missing argument is; everything else is an
	 * {@link IllegalArgumentException} carrying the problem's own message, so that
	 * what is thrown and what {@link #findProblem} reports are the same sentence.</p>
	 *
	 * @param problem the problem to describe.
	 */
	private static RuntimeException toException(final LayoutIdentifierProblem problem) {
		return problem.rule() == MISSING ?
				new NullPointerException(problem.message()) :
				new IllegalArgumentException(problem.message());
	}

	/**
	 * {@return why the identifier is not usable as part of a name, or {@code null}
	 * when it is.}
	 *
	 * @param identifier the identifier to check.
	 * @param parameter which identifier this is.
	 */
	private static @Nullable LayoutIdentifierProblem findUsableNameProblem(
			final @Nullable String identifier,
			final Parameter parameter
	) {
		final String parameterName = nameOf(parameter);

		if (identifier == null) {
			return new LayoutIdentifierProblem(MISSING, parameter, parameterName);
		}

		if (identifier.isBlank()) {
			return new LayoutIdentifierProblem(
					BLANK,
					parameter,
					parameterName + " must not be blank."
			);
		}

		if (identifier.indexOf('/') >= 0 || identifier.indexOf('\\') >= 0) {
			return new LayoutIdentifierProblem(
					PATH,
					parameter,
					parameterName + " must be one name rather than a path, but was '"
							+ identifier + "'."
			);
		}

		if (DIRECTORY_NAMES.contains(identifier)) {
			return new LayoutIdentifierProblem(
					DIRECTORY,
					parameter,
					parameterName + " must name a layout rather than a directory, but was '"
							+ identifier + "'."
			);
		}

		final LayoutIdentifierProblem characterProblem =
				findForbiddenCharacterProblem(identifier, parameter);

		if (characterProblem != null) {
			return characterProblem;
		}

		// Windows keeps neither: the shell and the interface drop a trailing space
		// or period, so the name asked for and the name stored are not the same
		// name. A leading period is fine, and is how a hidden file is written.
		final char lastCharacter = identifier.charAt(identifier.length() - 1);

		if (lastCharacter == ' ' || lastCharacter == '.') {
			return new LayoutIdentifierProblem(
					TRAILING_SPACE_OR_PERIOD,
					parameter,
					parameterName + " must not end with a space or a period, but was '"
							+ identifier + "'."
			);
		}

		return null;
	}

	/**
	 * {@return why the identifier holds a character no file name may hold, or
	 * {@code null} when it holds none.}
	 *
	 * @param identifier the identifier to check.
	 * @param parameter which identifier this is.
	 */
	private static @Nullable LayoutIdentifierProblem findForbiddenCharacterProblem(
			final String identifier,
			final Parameter parameter
	) {
		for (int index = 0; index < identifier.length(); index++) {
			final char character = identifier.charAt(index);

			// Everything below a space is a control character, which Windows
			// rejects outright and a zero byte ends a POSIX name early.
			if (character >= ' ' && FORBIDDEN_CHARACTERS.indexOf(character) < 0) {
				continue;
			}

			final String description = character < ' ' ?
					"the character with code " + (int) character :
					"'" + character + "'";

			return new LayoutIdentifierProblem(
					FORBIDDEN_CHARACTER,
					parameter,
					nameOf(parameter) + " must not contain " + description
							+ ", but has one at index " + index + "."
			);
		}

		return null;
	}

	/**
	 * {@return why the identifier names a device, or {@code null} when it does not.}
	 *
	 * @param layoutIdentifier the identifier to check.
	 */
	private static @Nullable LayoutIdentifierProblem findDeviceNameProblem(
			final String layoutIdentifier
	) {
		final int firstDot = layoutIdentifier.indexOf('.');

		final String baseName = firstDot < 0 ?
				layoutIdentifier :
				layoutIdentifier.substring(0, firstDot);

		if (!RESERVED_DEVICE_NAMES.contains(baseName.toUpperCase(Locale.ROOT))) {
			return null;
		}

		return new LayoutIdentifierProblem(
				DEVICE_NAME,
				LAYOUT_IDENTIFIER,
				"layoutIdentifier must not be a name reserved for a device, but was '"
						+ layoutIdentifier + "'."
		);
	}

	/**
	 * {@return why the two identifiers are too long together, or {@code null} when
	 * they are not.}
	 *
	 * @param layoutIdentifier identifies the layout.
	 * @param codecIdentifier identifies the codec whose output is stored.
	 */
	private static @Nullable LayoutIdentifierProblem findJoinedLengthProblem(
			final String layoutIdentifier,
			final String codecIdentifier
	) {
		// The two arrive as one path component joined by a '.', so what has to
		// fit is both of them plus that separator.
		final int joinedLength =
				layoutIdentifier.length() + 1 + codecIdentifier.length();

		if (joinedLength <= MAX_JOINED_LENGTH) {
			return null;
		}

		return new LayoutIdentifierProblem(
				TOO_LONG,
				BOTH,
				"layoutIdentifier and codecIdentifier must take at most "
						+ MAX_JOINED_LENGTH + " characters together, but '"
						+ layoutIdentifier + "' and '" + codecIdentifier
						+ "' take " + joinedLength + "."
		);
	}

	/**
	 * {@return the parameter's name, as a message naming it should spell it.}
	 *
	 * @param parameter the parameter to name.
	 */
	private static String nameOf(final Parameter parameter) {
		return parameter == CODEC_IDENTIFIER ?
				"codecIdentifier" :
				"layoutIdentifier";
	}
}
