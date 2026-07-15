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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparqlAstToExpressionTest {

    @Test
    @DisplayName("IRI terms convert to Corese constant URI expressions")
    void convertsIriAstToConstantUri() {
        IriAst iri = new IriAst("<http://ns.inria.fr/test/iri");

        Expression expression = SparqlAstToExpression.convert(iri);

        Constant constant = assertInstanceOf(Constant.class, expression);
        assertTrue(constant.isURI());
        assertEquals("http://ns.inria.fr/test/iri", constant.getLabel());
    }

    @Test
    @DisplayName("Literal terms preserve lexical value and language tag")
    void convertsLiteralAstToConstantLiteral() {
        LiteralAst literal = new LiteralAst("1234", "fr", null);

        Expression expression = SparqlAstToExpression.convert(literal);

        Constant constant = assertInstanceOf(Constant.class, expression);
        assertTrue(constant.isLiteral());
        assertEquals("1234", constant.getLabel());
        assertEquals("fr", constant.getLang());
    }

    @Test
    @DisplayName("Variable terms convert to Corese variable expressions")
    void convertsVarAstToVariable() {
        VarAst variable = new VarAst("var1");

        Expression expression = SparqlAstToExpression.convert(variable);

        Variable converted = assertInstanceOf(Variable.class, expression);
        assertEquals("var1", converted.getLabel());
    }

    @Test
    @DisplayName("EXISTS filters fail explicitly until graph-pattern expression conversion exists")
    void existsFilterFailsWithUnsupportedFeatureException() {
        ExistsAst exists = new ExistsAst(new GroupGraphPatternAst(List.of()));

        UnsupportedQueryFeatureException error = assertThrows(
                UnsupportedQueryFeatureException.class,
                () -> SparqlAstToExpression.convert(exists));

        assertTrue(error.getMessage().contains("EXISTS filters"));
    }
}
