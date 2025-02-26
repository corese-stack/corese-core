package fr.inria.corese.core.kgram.api.query;

/**
 * Olivier Corby - Wimmics INRIA I3S - 2014
 */
public interface Graphable {

    String toGraph();

    Object getGraph();

    void setGraph(Object obj);


}
