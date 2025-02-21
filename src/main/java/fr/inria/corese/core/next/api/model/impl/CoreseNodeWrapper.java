package fr.inria.corese.core.next.api.model.impl;

import fr.inria.corese.core.kgram.api.core.Node;

/**
 * Wrapper for objects that are represented as Node in Corese code, e.g. IRI, Literal, etc.
 * Intended to be used to be able to pass Corese objects to the API and vice-versa
 *
 */
public interface CoreseNodeWrapper {

     /**
      *
      * @return should return a Corese node that a guaranteed kind, i.e. Literal IRI, etc
      */
     Node getCoreseNode();

}
