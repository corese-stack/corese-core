package fr.inria.corese.core.next.query.impl.engine.sorter;

import fr.inria.corese.core.next.query.impl.engine.model.ExpType;

/**
 * Constants
 *
 * @author Fuqi Song, Wimmics Inria I3S
 */
public final class QuerySorterConst {

    private QuerySorterConst() {
        // Utility class
    }

    static final int ALL = 0;
    static final int SUBJECT = 1;
    static final int PREDICATE = 2;
    static final int OBJECT = 3;
    static final int TRIPLE = 4;
    static final int NA = -1;

    static final int BOUND = 0;
    static final int LIST = 0;
    static final int UNBOUND = Integer.MAX_VALUE;

    static final ExpType.Type[] EVALUABLE_TYPES = {ExpType.Type.EDGE, ExpType.Type.GRAPH};
    static final ExpType.Type[] NOT_EVALUABLE_TYPES = {ExpType.Type.FILTER, ExpType.Type.VALUES, ExpType.Type.BIND, ExpType.Type.OPTIONAL};

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
