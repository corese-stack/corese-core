package fr.inria.corese.core.kgram.filter;

import fr.inria.corese.core.kgram.api.core.Expr;
import fr.inria.corese.core.kgram.api.query.Hierarchy;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * Manage extension functions 
 * Expr exp must have executed exp.local() to tag
 * local variables
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 *
 */
public interface Extension {
    
    void define(Expr exp);
    
    Expr get(String name);
    
    Expr get(Expr exp);
    
    Expr get(Expr exp, String name);
    
    Expr get(String label, int n);
    
    Expr getMetadata(String metadata, int n);
    
    Expr getMethod(String label, IDatatype type, IDatatype[] param);
    
    Hierarchy getHierarchy();
    
    void setHierarchy(Hierarchy hierarchy);
    
    void removeNamespace(String name);
    
    boolean isDefined(Expr exp);
    
    boolean isMethod();
    
    void setDebug(boolean b);
    
}
