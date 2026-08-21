package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

/**
 * Represents the ADD operation as defined in the <a href="https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#add">SPARQL 1.1 recommendation</a>.
 * @param source source graph ({@code GRAPH <iri>} or {@code DEFAULT})
 * @param destination destination graph ({@code GRAPH <iri>} or {@code DEFAULT})
 * @param silent Determine if the resolution of the query must be resolved silently or not.
 */
public record AddRequestAst(GraphRefAst source, GraphRefAst destination, boolean silent) implements UpdateRequestUnitAst {
    /**
     * Construct an ADD query with the silent flag set to false.
     * @param source source graph
     * @param destination destination graph
     */
    public AddRequestAst(GraphRefAst source, GraphRefAst destination) {
        this(source, destination, false);
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.source.accept(visitor);
        this.destination.accept(visitor);
    }
}
