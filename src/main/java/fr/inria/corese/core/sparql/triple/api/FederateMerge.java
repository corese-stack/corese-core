package fr.inria.corese.core.sparql.triple.api;

import fr.inria.corese.core.sparql.triple.parser.Triple;

/**
 * Criteria to merge several triple into one connected bgp
 */
public interface FederateMerge {
    
    default boolean merge(Triple t) {
        return false;
    } 
    
}
