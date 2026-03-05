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

    public enum OPERATOR {
        BOOLEAN_NOT,
        PLUS,
        MINUS,
        BOUND,
        IS_IRI,
        IS_BLANK,
        IS_LITERAL,
        STR,
        LANG,
        DATATYPE,
        OR,
        AND,
        EQUALS,
        DIFFERENT,
        LOWER,
        LOWER_EQUAL,
        GREATER,
        GREATER_EQUAL,
        TIMES,
        DIVIDE,
        SAMETERM,
        LANGMATCHES,
        REGEX
    }
}
