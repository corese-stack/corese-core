package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.ExistsAst;
import fr.inria.corese.core.sparql.triple.parser.Constant;
import fr.inria.corese.core.sparql.triple.parser.Expression;
import fr.inria.corese.core.sparql.triple.parser.Variable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SparqlAstToExpressionTest {

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

    @Test
    void existsFilterFailsWithUnsupportedQueryFeatureException() {
        ExistsAst exists = new ExistsAst(new GroupGraphPatternAst(List.of()));

        UnsupportedQueryFeatureException error = assertThrows(
                UnsupportedQueryFeatureException.class,
                () -> SparqlAstToExpression.convert(exists));

        assertTrue(error.getMessage().contains("EXISTS filters"));
    }
}
