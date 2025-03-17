package fr.inria.corese.core.api;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.sparql.exceptions.EngineException;

/**
 * @author Olivier Corby, Wimmics INRIA 2012
 */
public interface Engine {

    public enum Type {
        RDFS_ENGINE,
        RULE_ENGINE,
        QUERY_ENGINE,
        WORKFLOW_ENGINE
    }

    boolean isActivate();

    // temporarily desactivate
    void setActivate(boolean b);

    void init();

    // return true if some new entailment have been performed
    boolean process() throws EngineException;

    // remove entailments
    void remove();

    // some edges have been deleted
    void onDelete();

    // edge inserted
    void onInsert(Node gNode, Edge edge);

    // graph have been cleared
    void onClear();

    Type type();


}
