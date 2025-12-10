package fr.inria.corese.core;

import fr.inria.corese.core.next.kgram.api.core.Edge;

/**
 *
 */
public interface EventHandler {

    default void delete(Edge query, Edge target) {
    }

    default void insert(Edge edge) {
    }

}
