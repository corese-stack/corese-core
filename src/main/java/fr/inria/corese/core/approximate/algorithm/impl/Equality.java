package fr.inria.corese.core.approximate.algorithm.impl;

import static fr.inria.corese.core.approximate.strategy.AlgType.EQ;

/**
 * Equality.java
 *
 * @author Fuqi Song, Wimmics Inria I3S
 * @date 17 nov. 2015
 */
public class Equality extends BaseAlgorithm {

    public Equality() {
        super(EQ);
    }

    @Override
    public double calculate(String s1, String s2, String paramter) {
        double sim = MAX;
        return sim;
    }
}
