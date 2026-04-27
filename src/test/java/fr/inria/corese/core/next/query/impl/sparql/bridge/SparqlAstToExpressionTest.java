package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.sparql.triple.parser.Expression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SparqlAstToExpressionTest {

    @Test
    void queryAstToQuery() {
    }

    @Test
    void iriAstToExpression() {
        IriAst iri = new IriAst("<http://ns.inria.fr/test/iri");
        Expression iriNode = SparqlAstToExpression.convert(iri);
        assertNotNull(iriNode);
        assertInstanceOf(Expression.class, iriNode);
    }

    @Test
    void literalAstToExpression() {
        LiteralAst lit = new LiteralAst("1234", "fr", null);
        Expression litNode = SparqlAstToExpression.convert(lit);
        assertNotNull(litNode);
        assertInstanceOf(Expression.class, litNode);
    }

    @Test
    void varAstToExpression() {
        VarAst var = new VarAst("var1");
        Expression varNode = SparqlAstToExpression.convert(var);
        assertNotNull(varNode);
        assertInstanceOf(Expression.class, varNode);
    }

//    @Test
//    void triplePatternAstToEdge() {
//        IriAst iri = new IriAst("<http://ns.inria.fr/test/iri>");
//        LiteralAst lit = new LiteralAst("1234", "fr", null);
//        VarAst var = new VarAst("var1");
//
//        TriplePatternAst iriiriiri = new TriplePatternAst(iri, iri, iri);
//        Edge iriiriiriEdge = SparqlAstToExpression.convert(iriiriiri);
//        assertNotNull(iriiriiriEdge);
//        assertInstanceOf(Edge.class, iriiriiriEdge);
//        assertInstanceOf(Expression.class, iriiriiriEdge.getSubjectNode());
//        assertInstanceOf(Expression.class, iriiriiriEdge.getPropertyNode());
//        assertInstanceOf(Expression.class, iriiriiriEdge.getObjectNode());
//
//        TriplePatternAst iriirilit = new TriplePatternAst(iri, iri, lit);
//        Edge iriirilitEdge = SparqlAstToExpression.convert(iriirilit);
//        assertNotNull(iriiriiriEdge);
//        assertInstanceOf(Edge.class, iriirilitEdge);
//        assertInstanceOf(Expression.class, iriirilitEdge.getSubjectNode());
//        assertInstanceOf(Expression.class, iriirilitEdge.getPropertyNode());
//        assertInstanceOf(Expression.class, iriirilitEdge.getObjectNode());
//
//        TriplePatternAst varirilit = new TriplePatternAst(var, iri, lit);
//        Edge varirilitEdge = SparqlAstToExpression.convert(varirilit);
//        assertNotNull(varirilitEdge);
//        assertInstanceOf(Edge.class, varirilitEdge);
//        assertInstanceOf(Expression.class, varirilitEdge.getSubjectNode());
//        assertInstanceOf(Expression.class, varirilitEdge.getPropertyNode());
//        assertInstanceOf(Expression.class, varirilitEdge.getObjectNode());
//    }
}
