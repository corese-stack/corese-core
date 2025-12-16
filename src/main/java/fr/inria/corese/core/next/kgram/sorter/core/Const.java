package fr.inria.corese.core.next.kgram.sorter.core;

import fr.inria.corese.core.next.kgram.api.core.ExpType;

/**
 * Constants
 *
 * @author Fuqi Song, Wimmics Inria I3S
 */
public final class Const {

    public static final int ALL = 0;
    public static final int SUBJECT = 1;
    public static final int PREDICATE = 2;
    public static final int OBJECT = 3;
    public static final int TRIPLE = 4;

    public final static int BOUND = 0, LIST = 0, UNBOUND = Integer.MAX_VALUE;

    public static final ExpType.Type[] EVALUABLE_TYPES = {ExpType.Type.EDGE, ExpType.Type.GRAPH};
    public static final ExpType.Type[] NOT_EVALUABLE_TYPES = {ExpType.Type.FILTER, ExpType.Type.VALUES, ExpType.Type.BIND, ExpType.Type.OPTIONAL};

    public static boolean plannable(ExpType.Type type) {
        for (ExpType.Type e : NOT_EVALUABLE_TYPES) {
            if (type == e) {
                return true;
            }
        }

        return evaluable(type);
    }

    public static boolean evaluable(ExpType.Type type) {
        for (ExpType.Type e : EVALUABLE_TYPES) {
            if (type == e) {
                return true;
            }
        }

        return false;
    }
}
