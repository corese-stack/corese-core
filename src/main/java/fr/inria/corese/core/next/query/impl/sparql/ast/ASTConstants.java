package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * This class contains constants used in the generation of SPARQL ASTs
 */
public class ASTConstants {

    public enum QUERY_TYPE {
        ASK,
        CONSTRUCT,
        DESCRIBE,
        SELECT,

        UNDEFINED
    }
}
