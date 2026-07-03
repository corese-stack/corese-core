package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.parser.AbstractSparqlParserFeatureTest;
import fr.inria.corese.core.next.query.impl.parser.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CoreseAstQueryBuilderConstructTest extends AbstractSparqlParserFeatureTest {

    private final CoreseAstQueryBuilder builder = new CoreseAstQueryBuilder();

    private static GroupGraphPatternAst whereSPO() {
        return new GroupGraphPatternAst(List.of(new BgpAst(List.of(new TriplePatternAst(new VarAst("x"), new VarAst("p"), new VarAst("o"))))));
    }

    private static ConstructTemplateAst template(TriplePatternAst... triples) {
        return new ConstructTemplateAst(List.of(triples));
    }

    @Test
    @DisplayName("CONSTRUCT { ?x ?p ?o } WHERE { ?x ?p ?o }: flagged CONSTRUCT, template is a BGP with one edge")
    void templateCompiledAsBgp() {
        ConstructQueryAst construct = new ConstructQueryAst(template(new TriplePatternAst(new VarAst("x"), new VarAst("p"), new VarAst("o"))), whereSPO());

        Query query = builder.toNextQuery(construct);

        assertTrue(query.isConstruct(), "query flagged as CONSTRUCT");
        assertFalse(query.isSelect(), "CONSTRUCT should not also report itself as SELECT");
        assertTrue(query.getBody().isAnd(), "WHERE compiled into the body");
        Exp constructExp = query.getConstruct();
        assertNotNull(constructExp, "template stored via setConstruct(Exp)");
        assertTrue(constructExp.isBGP(), "template is a BGP");
        assertEquals(1, constructExp.size(), "one template triple -> one edge");
    }

    @Test
    @DisplayName("Parser -> ConstructQueryAst -> Query keeps the CONSTRUCT form")
    void buildsQueryFromParsedConstruct() {
        SparqlParser parser = newParserDefault();
        ConstructQueryAst construct = assertInstanceOf(ConstructQueryAst.class, parser.parse("CONSTRUCT { ?x ?p ?o } WHERE { ?x ?p ?o }"));

        Query query = builder.toNextQuery(construct);

        assertTrue(query.isConstruct());
        assertFalse(query.isSelect());
        assertTrue(query.getBody().get(0).isBGP());
        assertNotNull(query.getConstruct());
        assertEquals(1, query.getConstruct().size());
    }

    @Test
    @DisplayName("Two template triples produce two edges")
    void templateWithTwoTriples() {
        ConstructQueryAst construct = new ConstructQueryAst(template(new TriplePatternAst(new VarAst("x"), new IriAst("http://example.org/p"), new VarAst("o")), new TriplePatternAst(new VarAst("x"), new IriAst("http://example.org/q"), new VarAst("o"))), whereSPO());

        Query query = builder.toNextQuery(construct);

        assertEquals(2, query.getConstruct().size());
    }

    @Test
    @DisplayName("Template variable bound by WHERE reuses the runtime body node")
    void templateVariableResolvesToBodyNode() {
        ConstructQueryAst construct = new ConstructQueryAst(template(new TriplePatternAst(new VarAst("x"), new VarAst("p"), new VarAst("o"))), whereSPO());

        Query query = builder.toNextQuery(construct);

        Edge edge = query.getConstruct().first().getEdge();
        assertSame(query.getExtNode("x"), edge.getNode(0), "template ?x is the body node");
        assertSame(query.getExtNode("o"), edge.getNode(1), "template ?o is the body node");
    }

    @Test
    @DisplayName("Template-only variable (not in WHERE) stays fresh and does not throw")
    void templateOnlyVariableStaysFresh() {
        ConstructQueryAst construct = new ConstructQueryAst(template(new TriplePatternAst(new VarAst("x"), new VarAst("p"), new VarAst("newVar"))), whereSPO());

        Query query = assertDoesNotThrow(() -> builder.toNextQuery(construct));

        assertNull(query.getExtNode("newVar"), "template-only variable is not injected into the body");
        Node object = query.getConstruct().first().getEdge().getNode(1);
        assertTrue(object.isVariable(), "template-only variable is still a variable node");
    }

    @Test
    @DisplayName("Template IRIs become fixed (non-variable) nodes")
    void templateWithFixedResources() {
        ConstructQueryAst construct = new ConstructQueryAst(template(new TriplePatternAst(new IriAst("http://example.org/s"), new IriAst("http://example.org/p"), new VarAst("o"))), whereSPO());

        Query query = builder.toNextQuery(construct);

        Edge edge = query.getConstruct().first().getEdge();
        assertFalse(edge.getNode(0).isVariable(), "fixed subject IRI");
        assertFalse(edge.getProperty().isVariable(), "fixed predicate IRI");
        assertTrue(edge.getNode(1).isVariable(), "object still bound to body ?o");
    }

    @Test
    @DisplayName("FROM / FROM NAMED, LIMIT and OFFSET are applied")
    void appliesDatasetLimitOffset() {
        DatasetClauseAst dataset = new DatasetClauseAst(Set.of(new IriAst("http://example.org/g")), Set.of(new IriAst("http://example.org/n")));
        SolutionModifierAst mod = SolutionModifierAst.withoutGroupBy(false, false, List.of(), 5L, 2L);
        ConstructQueryAst construct = new ConstructQueryAst(template(new TriplePatternAst(new VarAst("x"), new VarAst("p"), new VarAst("o"))), dataset, whereSPO(), mod);

        Query query = builder.toNextQuery(construct);

        assertEquals(List.of("http://example.org/g"), query.getFrom().stream().map(Node::getLabel).toList(), "FROM applied");
        assertEquals(List.of("http://example.org/n"), query.getNamed().stream().map(Node::getLabel).toList(), "FROM NAMED applied");
        assertEquals(5, query.getLimit(), "LIMIT applied");
        assertEquals(2, query.getOffset(), "OFFSET applied");
    }

    @Test
    @DisplayName("ORDER BY ?x is applied")
    void appliesOrderBy() {
        SolutionModifierAst mod = SolutionModifierAst.withoutGroupBy(false, false, List.of(new OrderConditionAst(ASTConstants.OrderDirection.ASC, new VarAst("x"))), null, null);
        ConstructQueryAst construct = new ConstructQueryAst(template(new TriplePatternAst(new VarAst("x"), new VarAst("p"), new VarAst("o"))), DatasetClauseAst.none(), whereSPO(), mod);

        Query query = builder.toNextQuery(construct);

        assertFalse(query.getOrderBy().isEmpty(), "ORDER BY applied");
    }

    @Test
    @DisplayName("Inline VALUES is not supported yet -> UnsupportedOperationException")
    void rejectsValuesClause() {
        ValuesAst values = new ValuesAst(List.of(new ValueMappingAst(Map.of(new VarAst("x"), new IriAst("http://example.org/v")))));
        ConstructQueryAst construct = new ConstructQueryAst(template(new TriplePatternAst(new VarAst("x"), new VarAst("p"), new VarAst("o"))), DatasetClauseAst.none(), whereSPO(), null, null, values);

        assertThrows(UnsupportedOperationException.class, () -> builder.toNextQuery(construct));
    }

    @Test
    @DisplayName("toNextQuery((ConstructQueryAst) null) throws NullPointerException")
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> builder.toNextQuery((ConstructQueryAst) null));
    }
}