package fr.inria.corese.core.sparql.triple.parser;

/**
 *
 */
public interface Message {
    
    String BNODE_SCOPE      = "Scope error for bnode: %s in %s";
    String BNODE_SCOPE1     = "Scope error for bnode: %s";
    String SELECT_DUPLICATE = "Duplicate select: %s as %s ";
    String PREFIX_UNDEFINED = "Undefined prefix: %s";
    String ARITY_ERROR      = "Arity error: %s";
    String SCOPE_ERROR      ="Scope error: %s";
    String PARAMETER_DUPLICATE = "Duplicate parameter: %s in:\n%s";
    String ORDER_GROUP_UNDEFINED ="OrderBy GroupBy undefined: %s";
    String VALUES_ERROR      = "Values error: nb variables != nb values";
    String VARIABLE_UNDEFINED = "Undefined variable: %s %s";
}
