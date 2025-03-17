package fr.inria.corese.core.approximate.ext;

import fr.inria.corese.core.approximate.strategy.StrategyType;
import fr.inria.corese.core.sparql.triple.parser.Atom;
import fr.inria.corese.core.sparql.triple.parser.Triple;
import fr.inria.corese.core.sparql.triple.parser.Variable;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.corese.core.approximate.ext.ASTRewriter.*;

/**
 * TripleWrapper.java
 *
 * @author Fuqi Song, Wimmics Inria I3S
 * @date 30 nov. 2015
 */
class TripleWrapper {

    private final Triple triple;
    private final int position;
    private final List<StrategyType> strategy;

    public TripleWrapper(Triple triple, int position, List<StrategyType> strategy) {
        this.triple = triple;
        this.position = position;
        this.strategy = strategy;
    }

    public TripleWrapper(Triple triple, int position) {
        this(triple, position, new ArrayList<>());
    }

    public Atom getAtom() {
        switch (position) {
            case S:
                return this.triple.getSubject();
            case P:
                return this.triple.getPredicate();
            case O:
                return this.triple.getObject();
            default:
                return null;
        }
    }

    public void setAtom(Variable variable) {
        switch (position) {
            case S:
                triple.setSubject(variable);
                break;
            case P:
                triple.setPredicate(variable);
                break;
            case O:
                triple.setObject(variable);
                break;
            default:
        }
    }

    public void addStrategy(StrategyType strategy) {
        this.strategy.add(strategy);
    }

    public int getPosition() {
        return position;
    }

    public Triple getTriple() {
        return triple;
    }

    public List<StrategyType> getStrategies() {
        return strategy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(this.triple).append("\t");
        sb.append(this.getAtom()).append(",\t");
        sb.append(this.getPosition()).append(",\t");
        sb.append("[");
        for (StrategyType st : strategy) {
            sb.append(st.name()).append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
