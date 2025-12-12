package fr.inria.corese.core.next.kgram.core;

import fr.inria.corese.core.next.kgram.api.core.Edge;
import fr.inria.corese.core.next.kgram.api.core.Filter;
import fr.inria.corese.core.next.kgram.api.core.Node;
import fr.inria.corese.core.next.kgram.api.query.Producer;

import java.util.ArrayList;
import java.util.HashMap;

public interface BgpGenerator {

    HashMap<Edge, ArrayList<Producer>> getIndexEdgeProducers();

    void setIndexEdgeProducers(HashMap<Edge, ArrayList<Producer>> tmpEdgeProducers);

    HashMap<Edge, ArrayList<Node>> getIndexEdgeVariables();

    void setIndexEdgeVariables(HashMap<Edge, ArrayList<Node>> tmpEdgeVariables);

    HashMap<Edge, ArrayList<Filter>> getIndexEdgeFilters();

    void setIndexEdgeFilters(HashMap<Edge, ArrayList<Filter>> tmpEdgeFilters);

    Exp process(Exp exp);

    HashMap<Edge, Exp> getEdgeAndContext();

}
