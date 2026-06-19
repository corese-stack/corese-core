package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

/**
 * RDF literal in a triple pattern (lexical form, optional language tag or datatype).
 */
public record LiteralAst(String lexical, String lang, String datatype) implements TermAst {
    public LiteralAst {
        if (lexical == null) {
            throw new IllegalArgumentException("Literal lexical is null");
        }
    }

    @Override
    public String getName() {
        if (datatype != null && !datatype.isBlank()) {
            return lexical + "^^" + datatype;
        }
        if (lang != null && !lang.isBlank()) {
            return lexical + "@" + lang;
        }
        return lexical;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}