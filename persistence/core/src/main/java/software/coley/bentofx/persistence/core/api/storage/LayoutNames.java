package software.coley.bentofx.persistence.core.api.storage;

import java.util.Locale;
import java.util.Objects;

/**
 * Turns the name a user types for a docking layout into the identifier the
 * layout is stored under.
 *
 * <p>The counterpart to {@link LayoutIdentifiers}, which says whether an
 * identifier may be used. This says how to arrive at one in the first place,
 * which the framework deliberately leaves to whoever collects the name.</p>
 *
 * @author Phil Bryant
 */
public final class LayoutNames {

	/**
	 * The most characters a derived layout identifier may run to.
	 *
	 * <p>Well short of what storage allows, so that a long name is shortened
	 * here rather than refused there.</p>
	 */
	private static final int MAX_LAYOUT_IDENTIFIER_LENGTH = 64;

	private LayoutNames() {
		throw new UnsupportedOperationException(
				"Utility classes should not be instantiated."
		);
	}

	/**
	 * {@return the derived layout identifier for a name a user typed, which is
	 * empty when the name held nothing an identifier can be made of.}
	 *
	 * <p>"Multi-Monitor" becomes "multi-monitor". The framework stores the
	 * typed name as a display name but addresses the layout by this identifier,
	 * and derives nothing itself.</p>
	 *
	 * <p>Only letters and digits survive, so the result cannot be a path, a
	 * directory, a name holding a character no file name may hold, or a name
	 * ending in a space or a period. What it can still be is nothing at all,
	 * the identifier this framework reserves, or a name Windows resolves to a
	 * device, which is why the result is still put to
	 * {@link LayoutIdentifiers#findUserLayoutProblem(String)}.</p>
	 *
	 * @param displayName the name the user typed.
	 * @throws NullPointerException if {@code displayName} is {@code null}.
	 */
	public static String toIdentifier(final String displayName) {
		Objects.requireNonNull(displayName, "displayName");

		// Cut before normalizing rather than after. Normalizing only ever
		// shortens, so cutting first bounds the identifier without leaving the
		// dash that a cut through a run of punctuation would.
		final String cutName =
				displayName.length() <= MAX_LAYOUT_IDENTIFIER_LENGTH ?
						displayName :
						displayName.substring(0, MAX_LAYOUT_IDENTIFIER_LENGTH);

		// A single pass instead of chained regexes: quantified regexes applied
		// one after another over the same input can backtrack super-linearly
		// on pathological input, and a plain scan is both faster and immune to
		// that by construction.
		final String lowercase = cutName.toLowerCase(Locale.ROOT);
		final StringBuilder identifier = new StringBuilder(lowercase.length());
		boolean pendingSeparator = false;

		for (final int codePoint : lowercase.codePoints().toArray()) {
			if (Character.isAlphabetic(codePoint) || Character.isDigit(codePoint)) {
				if (pendingSeparator && !identifier.isEmpty()) {
					identifier.append('-');
				}
				identifier.appendCodePoint(codePoint);
				pendingSeparator = false;
			} else {
				pendingSeparator = true;
			}
		}

		return identifier.toString();
	}
}
