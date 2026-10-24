package software.coley.bentofx.persistence.core.api.storage;

import java.util.Objects;

/**
 * Why a pair of layout identifiers cannot be used, for a caller that wants to ask
 * rather than be refused.
 *
 * <p>{@link LayoutIdentifiers#requireValid} throws for application code that has no
 * business supplying an unusable identifier. A dialog naming a layout is the other
 * case: it needs an answer per keystroke and text to show, so
 * {@link LayoutIdentifiers#findProblem} returns one of these instead. Both come from
 * the same check, so the two can never disagree about what is usable.</p>
 *
 * <p>{@link #rule()} is what to switch on when an application renders its own text,
 * in its own language. {@link #message()} is the framework's rendering, which is also
 * what the throwing entry point reports, and is worth showing when an application has
 * nothing better to say.</p>
 *
 * @param rule which rule the identifiers broke.
 * @param parameter which of the two identifiers broke it.
 * @param message the framework's description of the problem, ready to show.
 *
 * @author Phil Bryant
 */
public record LayoutIdentifierProblem(
		Rule rule,
		Parameter parameter,
		String message
) {

	/**
	 * Creates a problem.
	 *
	 * @param rule which rule the identifiers broke.
	 * @param parameter which of the two identifiers broke it.
	 * @param message the framework's description of the problem.
	 */
	public LayoutIdentifierProblem {
		Objects.requireNonNull(rule, "rule");
		Objects.requireNonNull(parameter, "parameter");
		Objects.requireNonNull(message, "message");
	}

	/**
	 * The rules a pair of layout identifiers has to satisfy.
	 *
	 * <p>Adding a rule adds a constant here, so an application that switches over
	 * these should have a default arm that shows {@link #message()}.</p>
	 */
	public enum Rule {

		/** The identifier was {@code null}. */
		MISSING,

		/** The identifier was empty or only whitespace. */
		BLANK,

		/** The identifier held a path separator, so it named more than one name. */
		PATH,

		/** The identifier was {@code .} or {@code ..}, which name directories. */
		DIRECTORY,

		/**
		 * The identifier held a character no file name may hold: one of
		 * {@code < > : " | ? *}, or a control character.
		 */
		FORBIDDEN_CHARACTER,

		/**
		 * The identifier ended with a space or a period, which Windows drops, so the
		 * name asked for and the name stored would differ.
		 */
		TRAILING_SPACE_OR_PERIOD,

		/**
		 * The layout identifier is a name a filesystem resolves to a device rather
		 * than to a file.
		 */
		DEVICE_NAME,

		/**
		 * The layout identifier is one this framework has taken for itself, such as
		 * {@link LayoutIdentifiers#SESSION_LAYOUT_IDENTIFIER}. Reported only by
		 * {@link LayoutIdentifiers#findUserLayoutProblem}, because an application
		 * saving or restoring the session layout is doing something legitimate.
		 */
		RESERVED,

		/**
		 * The two identifiers exceed {@link LayoutIdentifiers#MAX_JOINED_LENGTH}
		 * together, which is the only rule that is about the pair rather than about
		 * one of them.
		 */
		TOO_LONG
	}

	/**
	 * Which identifier a rule was applied to.
	 */
	public enum Parameter {

		/** The identifier naming the layout. */
		LAYOUT_IDENTIFIER,

		/** The identifier naming the codec whose output is stored. */
		CODEC_IDENTIFIER,

		/** Both, for a rule about the pair rather than about either one. */
		BOTH
	}
}
