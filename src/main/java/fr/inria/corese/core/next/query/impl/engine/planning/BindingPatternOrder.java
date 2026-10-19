package fr.inria.corese.core.next.query.impl.engine.planning;
import static fr.inria.corese.core.next.query.impl.engine.planning.PlanningConstants.BOUND;
import static fr.inria.corese.core.next.query.impl.engine.planning.PlanningConstants.NA;
import static fr.inria.corese.core.next.query.impl.engine.planning.PlanningConstants.OBJECT;
import static fr.inria.corese.core.next.query.impl.engine.planning.PlanningConstants.PREDICATE;
import static fr.inria.corese.core.next.query.impl.engine.planning.PlanningConstants.SUBJECT;
import static fr.inria.corese.core.next.query.impl.engine.planning.PlanningConstants.UNBOUND;

/**
 * Ranks subject, predicate, and object binding patterns using distinct-term counts.
 * Falls back to the default predicate/subject/object order when counts are unavailable.
 *
 * @author Fuqi Song, Wimmics Inria I3S
 */
public final class BindingPatternOrder {

    private static final int P = 1;
    private static final int S = 0;
    private static final int O = 2;
    private static final int[] DEFAULT_ORDER = {P, S, O};
    private static final int TRIPLE_LENGTH = 3;
    private static final int PATTERN_LENGTH = 8;

    private BindingPatternOrder() {
    }


    private static int[] getNumbers(QueryStatistics ip) {
        int[] numbers = null;
        if (ip != null
                && ip.getSize(SUBJECT) != NA
                && ip.getSize(OBJECT) != NA) {

            numbers = new int[]{
                ip.getSize(SUBJECT),
                ip.getSize(PREDICATE),
                ip.getSize(OBJECT)};
        }

        return numbers;
    }

    /**
     * Generates the eight binding patterns in estimated selectivity order.
     *
     * @param producer distinct-term statistics, or {@code null} for the default order
     * @return ordered subject/predicate/object binding patterns
     */
    public static int[][] generateBasicPattern(QueryStatistics producer) {
        //1 step: get the order of s, p, o according to the number of distinct s, p, o
        // using default settings if the numbers of s, p, o are not available
        int[] order = DEFAULT_ORDER;
        int[] numbers = getNumbers(producer);
        if (numbers != null && numbers.length == TRIPLE_LENGTH) {
            order = order3Numbers(numbers);
        }

        //2 step: generate the patterns
        //!! Note: LIST needs to be considered in future, at the moment
        //LIST is considered as BOUND
        int[][] patterns = new int[PATTERN_LENGTH][TRIPLE_LENGTH];
        patterns[0] = new int[]{BOUND, BOUND, BOUND};
        for (int i = 0; i < order.length; i++) {
            for (int j = 0; j < TRIPLE_LENGTH; j++) {
                //1 first three patterns with two constants
                //2 second three patterns with ONE constants
                if (j == order[i]) {
                    patterns[i + 1][j] = UNBOUND;
                    patterns[PATTERN_LENGTH - 2 - i][j] = BOUND;
                } else {
                    patterns[i + 1][j] = BOUND;
                    patterns[PATTERN_LENGTH - 2 - i][j] = UNBOUND;
                }
            }
        }
        patterns[PATTERN_LENGTH - 1] = new int[]{UNBOUND, UNBOUND, UNBOUND};

        return patterns;
    }

    private static int[] order3Numbers(int[] numbers) {
        int[] order = {0, 1, 2};  // Simpler syntax

        if (numbers[0] > numbers[1]) {
            swap(numbers, 0, 1);
            swap(order, 0, 1);
        }
        if (numbers[1] > numbers[2]) {
            swap(numbers, 1, 2);
            swap(order, 1, 2);
        }
        if (numbers[0] > numbers[1]) {
            swap(numbers, 0, 1);
            swap(order, 0, 1);
        }

        return order;
    }

    private static void swap(int[] arr, int a, int b) {
        int x = arr[a];
        arr[a] = arr[b];
        arr[b] = x;
    }
}
