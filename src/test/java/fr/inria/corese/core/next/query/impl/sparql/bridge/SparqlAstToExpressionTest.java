package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.sparql.triple.parser.Constant;
import fr.inria.corese.core.sparql.triple.parser.Expression;
import fr.inria.corese.core.sparql.triple.parser.Variable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SparqlAstToExpressionTest {

    @Test
    void queryAstToQuery() {
    }

    @Test
    void iriAstToExpression() {
        IriAst iri = new IriAst("<http://ns.inria.fr/test/iri");
        Expression iriNode = SparqlAstToExpression.convert(iri);
        assertNotNull(iriNode);
        assertInstanceOf(Constant.class, iriNode);
        assertTrue(iriNode.isURI());
        assertEquals("http://ns.inria.fr/test/iri", ((Constant)iriNode).getLabel());
    }

    @Test
    void literalAstToExpression() {
        LiteralAst lit = new LiteralAst("1234", "fr", null);
        Expression litNode = SparqlAstToExpression.convert(lit);
        assertNotNull(litNode);
        assertInstanceOf(Constant.class, litNode);
        assertTrue(litNode.isLiteral());
        assertEquals("1234", litNode.getLabel());
        assertEquals("fr", litNode.getLang());
    }

    @Test
    void varAstToExpression() {
        VarAst var = new VarAst("var1");
        Expression varNode = SparqlAstToExpression.convert(var);
        assertNotNull(varNode);
        assertInstanceOf(Variable.class, varNode);
        assertEquals("var1", varNode.getLabel());
    }

//    @Test
//    void triplePatternAstIriIriIriToEdge() {
//        IriAst iri = new IriAst("<http://ns.inria.fr/test/iri>");
//
//        TriplePatternAst iriiriiri = new TriplePatternAst(iri, iri, iri);
//        Edge iriiriiriEdge = SparqlAstToExpression.convert(iriiriiri);
//        assertNotNull(iriiriiriEdge);
//        assertInstanceOf(Edge.class, iriiriiriEdge);
//        assertInstanceOf(Constant.class, iriiriiriEdge.getSubjectNode());
//        assertInstanceOf(Constant.class, iriiriiriEdge.getPropertyNode());
//        assertInstanceOf(Constant.class, iriiriiriEdge.getObjectNode());
//    }
//
//    @Test
//    void triplePatternAstIriIriLitToEdge() {
//        IriAst iri = new IriAst("<http://ns.inria.fr/test/iri>");
//        LiteralAst lit = new LiteralAst("1234", "fr", null);
//        VarAst var = new VarAst("var1");
//
//        TriplePatternAst iriirilit = new TriplePatternAst(iri, iri, lit);
//        Edge iriirilitEdge = SparqlAstToExpression.convert(iriirilit);
//        assertNotNull(iriirilitEdge);
//        assertInstanceOf(Edge.class, iriirilitEdge);
//        assertInstanceOf(Constant.class, iriirilitEdge.getSubjectNode());
//        assertInstanceOf(Constant.class, iriirilitEdge.getPropertyNode());
//        assertInstanceOf(Constant.class, iriirilitEdge.getObjectNode());
//    }
//
//    @Test
//    void triplePatternAstVarIriLitToEdge() {
//        IriAst iri = new IriAst("<http://ns.inria.fr/test/iri>");
//        LiteralAst lit = new LiteralAst("1234", "fr", null);
//        VarAst var = new VarAst("var1");
//
//        TriplePatternAst varirilit = new TriplePatternAst(var, iri, lit);
//        Edge varirilitEdge = SparqlAstToExpression.convert(varirilit);
//        assertNotNull(varirilitEdge);
//        assertInstanceOf(Edge.class, varirilitEdge);
//        assertInstanceOf(Variable.class, varirilitEdge.getSubjectNode());
//        assertInstanceOf(Constant.class, varirilitEdge.getPropertyNode());
//        assertInstanceOf(Constant.class, varirilitEdge.getObjectNode());
//    }
//
//    @Test
//    void triplePatternAstIriVarLitToEdge() {
//        IriAst iri = new IriAst("<http://ns.inria.fr/test/iri>");
//        LiteralAst lit = new LiteralAst("1234", "fr", null);
//        VarAst var = new VarAst("var1");
//
//        TriplePatternAst irivarlit = new TriplePatternAst(iri, var, lit);
//        Edge irivarlitEdge = SparqlAstToExpression.convert(irivarlit);
//        assertNotNull(irivarlitEdge);
//        assertInstanceOf(Edge.class, irivarlitEdge);
//        assertInstanceOf(Constant.class, irivarlitEdge.getSubjectNode());
//        assertInstanceOf(Variable.class, irivarlitEdge.getPropertyNode());
//        assertInstanceOf(Constant.class, irivarlitEdge.getObjectNode());
//    }
//
//    @Test
//    void triplePatternAstVarIriVarToEdge() {
//        IriAst iri = new IriAst("<http://ns.inria.fr/test/iri>");
//        VarAst var1 = new VarAst("var1");
//        VarAst var2 = new VarAst("var2");
//
//        TriplePatternAst varirivar = new TriplePatternAst(var1, iri, var2);
//        Edge varirivarEdge = SparqlAstToExpression.convert(varirivar);
//        assertNotNull(varirivarEdge);
//        assertInstanceOf(Edge.class, varirivarEdge);
//        assertInstanceOf(Variable.class, varirivarEdge.getSubjectNode());
//        assertInstanceOf(Constant.class, varirivarEdge.getPropertyNode());
//        assertInstanceOf(Variable.class, varirivarEdge.getObjectNode());
//    }
//
//    @Test
//    void triplePatternAstVarVarVarToEdge() {
//        VarAst var1 = new VarAst("var1");
//        VarAst var2 = new VarAst("var2");
//        VarAst var3 = new VarAst("var3");
//
//        TriplePatternAst varirivar = new TriplePatternAst(var1, var2, var3);
//        Edge varirivarEdge = SparqlAstToExpression.convert(varirivar);
//        assertNotNull(varirivarEdge);
//        assertInstanceOf(Edge.class, varirivarEdge);
//        assertInstanceOf(Variable.class, varirivarEdge.getSubjectNode());
//        assertInstanceOf(Variable.class, varirivarEdge.getPropertyNode());
//        assertInstanceOf(Variable.class, varirivarEdge.getObjectNode());
//    }
}
