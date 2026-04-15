package fr.inria.corese.core.next.query.impl.sparql;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.impl.common.util.IRIUtils;
import fr.inria.corese.core.next.data.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.data.impl.temp.CoreseNodeAdapter;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.core.Query;
import fr.inria.corese.core.edge.EdgeImpl;
import fr.inria.corese.core.sparql.triple.parser.Variable;
import fr.inria.corese.core.next.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * Adapts SPARQL query AST to KGRAM Exp/Query.
 */
public final class CoreseQueryArtifactAdapter {

    private static final CoreseAdaptedValueFactory coreseAdaptedValueFactory = new CoreseAdaptedValueFactory();

    private CoreseQueryArtifactAdapter() {
    }

    public static Query queryAstToQueryArtifact(QueryAst ast) {
        return null; // @TODO
    }

    public static Node termAstToNode(TermAst ast) {
        switch (ast) {
            case ConstraintAst constraintAst -> {
                return null; // @TODO
            }
            case IriAst iriAst -> {
                return iriAstToCoreseIri(iriAst);
            }
            case LiteralAst literalAst -> {
                return literalAstToCoreseLiteral(literalAst);
            }
            case VarAst varAst -> {
                return varAstToVariable(varAst);
            }
        }
    }

    private static Node varAstToVariable(VarAst varAst) {
        return (Node) Variable.create(varAst.name());
    }

    private static Node iriAstToCoreseIri(IriAst ast) {
        return ((CoreseNodeAdapter) coreseAdaptedValueFactory.createIRI(StringUtils.trimChevronIRIs(ast.raw()))).getCoreseNode();
    }

    private static Node literalAstToCoreseLiteral(LiteralAst literalAst) {
        if(literalAst.datatype() != null && IRIUtils.isStandardIRI(literalAst.datatype())) {
            IRI datatypeIri = coreseAdaptedValueFactory.createIRI(literalAst.datatype());
            return ((CoreseNodeAdapter) coreseAdaptedValueFactory.createLiteral(literalAst.lexical(), datatypeIri)).getCoreseNode();
        }
        return ((CoreseNodeAdapter) coreseAdaptedValueFactory.createLiteral(literalAst.lexical(), literalAst.lang())).getCoreseNode();
    }

    public static Edge triplePatternAstToEdge(TriplePatternAst ast) {
        Objects.requireNonNull(ast, "TriplePatternAst cannot be null");
        Node subject = termAstToNode(ast.subject());
        Node predicate = termAstToNode(ast.predicate());
        Node object = termAstToNode(ast.object());
        Objects.requireNonNull(subject, "Converted Node subject cannot be null");
        Objects.requireNonNull(predicate, "Converted Node predicate cannot be null");
        Objects.requireNonNull(object, "Converted Node object cannot be null");
        return EdgeImpl.create(subject, predicate, List.of(object));
    }
}
