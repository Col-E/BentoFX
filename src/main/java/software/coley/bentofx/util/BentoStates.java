package software.coley.bentofx.util;

import javafx.css.PseudoClass;

/**
 * All pseudo-states used by bento controls.
 *
 * @author Matt Coley
 */
public class BentoStates {
	public static final PseudoClass PSEUDO_ROOT = PseudoClass.getPseudoClass("root");
	public static final PseudoClass PSEUDO_ACTIVE = PseudoClass.getPseudoClass("active");
	public static final PseudoClass PSEUDO_COLLAPSED = PseudoClass.getPseudoClass("collapsed");
	public static final PseudoClass PSEUDO_ORIENTATION_H = PseudoClass.getPseudoClass("horizontal");
	public static final PseudoClass PSEUDO_ORIENTATION_V = PseudoClass.getPseudoClass("vertical");
	public static final PseudoClass PSEUDO_SELECTED = PseudoClass.getPseudoClass("selected");
	public static final PseudoClass PSEUDO_HOVER = PseudoClass.getPseudoClass("hover");
	public static final PseudoClass PSEUDO_SIDE_TOP = PseudoClass.getPseudoClass("top");
	public static final PseudoClass PSEUDO_SIDE_BOTTOM = PseudoClass.getPseudoClass("bottom");
	public static final PseudoClass PSEUDO_SIDE_LEFT = PseudoClass.getPseudoClass("left");
	public static final PseudoClass PSEUDO_SIDE_RIGHT = PseudoClass.getPseudoClass("right");
}
