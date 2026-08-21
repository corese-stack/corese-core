package fr.inria.corese.core.next.query.impl.kgram.sorter.impl.qpv1;

import fr.inria.corese.core.next.query.impl.kgram.sorter.core.IProducerQP;

import static fr.inria.corese.core.next.query.impl.kgram.sorter.core.QuerySorterConst.*;

/**
 * Generate the basic patterns ordering by the selectivity acorrding to the size
 * of Subject, Predicate and Object Ns, Np and No
 *
 * @author Fuqi Song, Wimmics Inria I3S
 */
public class BasicPatternGenerator {

    public static int[][] BASIC_PATTERN = null;
    private final static int P = 1, S = 0, O = 2;
    private final static int[] default_order = {P, S, O};//p<s<o
    private final static int TRIPLE_LEN = 3, PATTERN_LEN = 8;


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
    public static int[][] generateBasicPattern(IProducerQP producer, boolean regen) {
        if (BASIC_PATTERN != null && !regen) {
            return BASIC_PATTERN;
        }

        //1 step: get the order of s, p, o according to the number of distinct s, p, o
        // using default settings if the numbers of s, p, o are not available
        int[] order = default_order;
        int[] numbers = getNumbers(producer);
        if (numbers != null && numbers.length == TRIPLE_LEN) {
            order = order3Numbers(numbers);
        }

        //2 step: generate the patterns
        //!! TODO LIST needs to be considered in future, at the moment
        //LIST is considered as BOUND
        BASIC_PATTERN = new int[PATTERN_LEN][TRIPLE_LEN];
        BASIC_PATTERN[0] = new int[]{BOUND, BOUND, BOUND};
        for (int i = 0; i < order.length; i++) {
            for (int j = 0; j < TRIPLE_LEN; j++) {
                //1 first three patterns with two constants
                //2 second three patterns with ONE constants
                if (j == order[i]) {
                    BASIC_PATTERN[i + 1][j] = UNBOUND;
                    BASIC_PATTERN[PATTERN_LEN - 2 - i][j] = BOUND;
                } else {
                    BASIC_PATTERN[i + 1][j] = BOUND;
                    BASIC_PATTERN[PATTERN_LEN - 2 - i][j] = UNBOUND;
                }
            }
        }
        BASIC_PATTERN[PATTERN_LEN - 1] = new int[]{UNBOUND, UNBOUND, UNBOUND};

        return BASIC_PATTERN;
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
