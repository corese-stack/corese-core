package fr.inria.corese.core.sparql.api;

import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import fr.inria.corese.core.kgram.core.Query;

public interface QueryVisitor {
	
	default void visit(ASTQuery ast) {}
	
        default void visit(Query query) {}
        
        default void visit(Query q, Graph g) {}
        
        default void before(Query q) {}
        
        default void after(Mappings map) {}
        
}
