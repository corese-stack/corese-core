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

    public interface Constraint {
    }

    public enum OPERATOR implements Constraint {
        BOOLEAN_NOT,
        PLUS,
        MINUS,
        EQUALS,
        DIFFERENT,
        OR,
        AND,
        LOWER,
        LOWER_EQUAL,
        GREATER,
        GREATER_EQUAL,
        TIMES,
        DIVIDE
    }

    public enum FUNCTION_CALL implements Constraint {
        BOUND,
        DATATYPE,
        ISBLANK,
        ISLITERAL,
        ISIRI,
        LANG,
        LANGMATCHES,
        SAMETERM,
        STR,
        REGEX,
        FUNCTION
    }

    public enum OrderDirection {
        ASC,
        DESC
    }
}
