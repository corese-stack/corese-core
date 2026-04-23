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
        // --- Binary operators ---

        /**
         * Addition
         */
        ADD,

        /**
         * Subtraction
         */
        SUB,

        /**
         * Multiplication
         */
        MUL,

        /**
         * Division
         */
        DIV,

        /**
         * Equality
         */
        EQ,

        /**
         * Inequality
         */
        NE,

        /**
         * Less than
         */
        LT,

        /**
         * Less than or equal
         */
        LE,

        /**
         * Greater than
         */
        GT,

        /**
         * Greater than or equal
         */
        GE,


        /**
         * Logical conjunction
         */
        AND,

        /**
         * Logical disjunction
         */
        OR,

        // --- Unary operators ---
        /**
         * Logical negation: {@code !operand}.
         */
        NOT,

        /**
         * Unary plus: {@code +operand}. No-op for numeric values, but enforces numeric type.
         */
        PLUS,

        /**
         * Unary minus: {@code -operand}. Negates a numeric value.
         */
        MINUS
    }

    public enum FUNCTION_CALL implements Constraint {
        /**
         * {@code STR(term)}: returns the string form of a literal or IRI.
         * For a literal, returns its lexical form; for an IRI, returns its IRI string.
         */
        STR,

        /**
         * {@code LANG(literal)}: returns the language tag of a plain literal,
         * or an empty string if the literal has no language tag.
         */
        LANG,

        /**
         * {@code LANGMATCHES(lang, range)}: returns {@code true} if the language tag
         * {@code lang} matches the language range {@code range} per RFC 4647.
         */
        LANGMATCHES,

        /**
         * {@code CONTAINS(string1, string2)}: returns {@code true} if {@code string1}
         * contains {@code string2}.
         */
        CONTAINS,

        /**
         * {@code STRSTARTS(string1, string2)}: returns {@code true} if {@code string1}
         * starts with {@code string2}.
         */
        STRSTARTS,

        /**
         * {@code STRENDS(string1, string2)}: returns {@code true} if {@code string1}
         * ends with {@code string2}.
         */
        STRENDS,

        /**
         * {@code UCASE(string)}: returns an uppercased copy of {@code string}.
         */
        UCASE,

        /**
         * {@code LCASE(string)}: returns a lowercased copy of {@code string}.
         */
        LCASE,

        /**
         * {@code STRDT(string, datatype)}: constructs a typed literal from a lexical
         * string and a datatype IRI.
         */
        STRDT,

        /**
         * {@code STRLANG(string, language)}: constructs a plain literal with a language
         * tag from a lexical string and a language tag string.
         */
        STRLANG,

        /**
         * {@code SUBSTR(string, start)} or {@code SUBSTR(string, start, length)}:
         * returns the substring of {@code string} starting at {@code start} with an
         * optional {@code length}.
         */
        SUBSTR,

        /**
         * {@code CONCAT(expr1, expr2, ...)}: concatenates the lexical forms of its arguments
         * into a single string result.
         */
        CONCAT,

        /**
         * {@code STRBEFORE(string1, string2)}: returns the part of {@code string1}
         * that precedes the first occurrence of {@code string2}.
         */
        STRBEFORE,

        /**
         * {@code STRAFTER(string1, string2)}: returns the part of {@code string1}
         * that follows the first occurrence of {@code string2}.
         */
        STRAFTER,

        /**
         * {@code REPLACE(arg, pattern, replacement)} and
         * {@code REPLACE(arg, pattern, replacement, flags)}: returns the string
         * obtained by replacing each non-overlapping match of {@code pattern} in
         * {@code arg} with {@code replacement}. Matching may be affected by
         * regular-expression {@code flags}.
         */
        REPLACE,

        /**
         * {@code DATATYPE(literal)}: returns the datatype IRI of a typed literal.
         * For a plain literal with no language tag, returns {@code xsd:string}.
         */
        DATATYPE,

        /**
         * {@code IRI(term)} or {@code URI(term)}: returns an IRI built from the lexical form
         * of {@code term}.
         */
        IRI,

        /**
         * {@code BNODE()} or {@code BNODE(label)}: returns a fresh blank node, optionally
         * stable for the given lexical label within the solution.
         */
        BNODE,

        /**
         * {@code REGEX(string, pattern)} and {@code REGEX(string, pattern, flags)}: Returns {@code true} if {@code string} matches the given {@code regex}. the matching is influenced by the {@code flags} when they are given.
         */
        REGEX,

        // --- Existence function ---

        /**
         * {@code BOUND(?var)}: returns {@code true} if the variable {@code ?var}
         * is bound (has a value) in the current solution mapping.
         */
        BOUND,

        // --- Type-testing functions ---

        /**
         * {@code isIRI(term)} or {@code isUri(term)}: returns {@code true} if {@code term} is an IRI.
         */
        IS_IRI,

        /**
         * {@code isBlank(term)}: returns {@code true} if {@code term} is a blank node.
         */
        IS_BLANK,

        /**
         * {@code isLiteral(term)}: returns {@code true} if {@code term} is a literal.
         */
        IS_LITERAL,

        /**
         * {@code sameterm(termA, termB)}: Returns {@code true} if termA and termB are the same RDF term (including their lang and datatypes)
         */
        SAMETERM

    }

    /**
     * Sort direction used in a SPARQL {@code ORDER BY} clause.
     */
    public enum OrderDirection {

        /**
         * Ascending order: smallest values first.
         */
        ASC,

        /**
         * Descending order: largest values first.
         */
        DESC
    }
}
