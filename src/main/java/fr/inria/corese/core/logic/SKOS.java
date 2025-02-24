package fr.inria.corese.core.logic;

import fr.inria.corese.core.sparql.triple.parser.NSManager;


public interface SKOS {
    
    String NS = NSManager.SKOS;
    
    String BROADER  = NS + "broader";  // has for broader = subConceptOf
    String NARROWER = NS + "narrower"; // has for narrower
    
}
