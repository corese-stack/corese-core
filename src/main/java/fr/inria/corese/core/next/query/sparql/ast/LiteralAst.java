package fr.inria.corese.core.next.query.sparql.ast;

/**
 * RDF literal in a triple pattern (lexical form, optional language tag or datatype).
 */
public record LiteralAst(String lexical, String lang, String datatype) implements TermAst {
    public LiteralAst {
        if (lexical == null) {
            throw new IllegalArgumentException("Literal lexical is null");
        }
    }
}
