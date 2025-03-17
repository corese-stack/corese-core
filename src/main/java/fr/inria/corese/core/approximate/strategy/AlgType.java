package fr.inria.corese.core.approximate.strategy;

import java.util.Arrays;
import java.util.List;

/**
 * Enumeration of types of similarity measurement algorithms
 *
 * @author fsong
 */
public enum AlgType {

    EMPTY,//no algorithm implemented
    NG, //n-gram
    JW, //jaro-winkler (edit distance)
    CH, //class hierarchy (empty now)
    WN, //wordnet
    EQ, //equality
    MULT; //combiend algorithm

    /**
     * Return the list of all types of single algorithms
     *
     * @return
     */
    public static List<AlgType> allValues() {
        return Arrays.asList(NG, JW, CH, WN, EQ);
    }
}
