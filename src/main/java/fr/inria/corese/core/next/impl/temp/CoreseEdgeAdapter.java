package fr.inria.corese.core.next.impl.temp;

import fr.inria.corese.core.kgram.api.core.Edge;

/**
 * Interface that defines a wrapper for classes that represent Corese edges.
 * This interface provides a method to retrieve the corresponding Corese {@link Edge} representation
 * for the implementing class.
 *
 */
public interface CoreseEdgeAdapter {

    /**
     * Retrieves the Corese {@link Edge} associated with the implementing class.
     *
     * @return the Corese {@link Edge} representation.
     */
    Edge getCoreseEdge();
}