package fr.inria.corese.core.next.query.impl.engine.sorter;

import fr.inria.corese.core.next.query.impl.engine.sorter.IProducerQP;

import static fr.inria.corese.core.next.query.impl.engine.sorter.QuerySorterConst.*;

/**
 * Generate the basic patterns ordering by the selectivity acorrding to the size
 * of Subject, Predicate and Object Ns, Np and No
 *
 * @author Fuqi Song, Wimmics Inria I3S
 */
public final class BasicPatternGenerator {

    private static final int P = 1;
    private static final int S = 0;
    private static final int O = 2;
    private static final int[] DEFAULT_ORDER = {P, S, O};
    private static final int TRIPLE_LENGTH = 3;
    private static final int PATTERN_LENGTH = 8;

    private BasicPatternGenerator() {
    }


    private static int[] getNumbers(IProducerQP ip) {
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
     * Generate the basic patterns using the numbers of distince subject,
     * predicate and objects
     *
     */
    public static int[][] generateBasicPattern(IProducerQP producer) {
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
