package fr.inria.corese.core.kgram.api.core;

/**
 * @author corby
 */
public interface ExprLabel {

    String SEPARATOR = "#";

    String EQUAL = "equal";
    String DIFF = "diff";
    String LESS = "less";
    String LESS_EQUAL = "lessEqual";
    String GREATER = "greater";
    String GREATER_EQUAL = "greaterEqual";

    String PLUS = "plus";
    String MINUS = "minus";
    String MULT = "mult";
    String DIV = "divis";

    String COMPARE = ExpType.KGRAM + "compare";

}
