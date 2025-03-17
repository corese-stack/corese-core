package fr.inria.corese.core.approximate.algorithm.impl;

import fr.inria.corese.core.approximate.algorithm.ISimAlgorithm;
import fr.inria.corese.core.approximate.strategy.AlgType;

/**
 * Base Algorithm
 *
 * @author Fuqi Song, Wimmics Inria I3S
 * @date 24 sept. 2015
 */
public class BaseAlgorithm implements ISimAlgorithm {

    public final static String OPTION_URI = "uri";
    private final AlgType type;

    /**
     * Constructor with default type 'empty'
     */
    BaseAlgorithm() {
        this(AlgType.EMPTY);
    }

    /**
     * Constructor with given type
     *
     * @param type
     */
    public BaseAlgorithm(AlgType type) {
        this.type = type;
    }

    /**
     * Return type of algorithm
     *
     * @return
     */
    public AlgType getType() {
        return this.type;
    }

    @Override
    public double calculate(String s1, String s2, String parameters) {
        return MIN;
    }

}
