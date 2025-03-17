package fr.inria.corese.core.approximate.algorithm;

/**
 * Interface for implementing the similarity measurement algorithms
 *
 * @author Fuqi Song, WImmics Inria I3S
 * @date 27 août 2015
 */
public interface ISimAlgorithm {

    int NA = Integer.MIN_VALUE;//not calculated

    double MIN = 0.0d;
    double MAX = 1.0d;

    /**
     * Calculate the similarity between strings s1 and s2
     *
     * @param s1
     * @param s2
     * @param parameters
     * @return
     */
    double calculate(String s1, String s2, String parameters);
}
