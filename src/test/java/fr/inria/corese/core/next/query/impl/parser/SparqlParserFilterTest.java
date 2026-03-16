package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class SparqlParserFilterTest extends AbstractSparqlParserFeatureTest {

    private static final Logger logger = LoggerFactory.getLogger(SparqlParserFilterTest.class);

    @Test
    void shouldParseTrueFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(true)
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(LiteralAst.class, filterAst.operator(), "Filter content should be an literal");

        LiteralAst t = (LiteralAst) filterAst.operator();

        assertEquals("true", t.lexical());
    }

    @Test
    void shouldParseAndFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s && true)
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        logger.info("{}", ast);
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(AndAst.class, filterAst.operator(), "Filter content should be an And operator");

        AndAst t = (AndAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getLeftArgument());
        assertInstanceOf(LiteralAst.class, t.getRightArgument());

        assertEquals("s", ((VarAst) t.getLeftArgument()).name());
        assertEquals("true", ((LiteralAst) t.getRightArgument()).lexical());
    }

    @Test
    void shouldParseOrFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?s WHERE {
                  ?s ?p ?o .
                  FILTER(?s || true)
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(OrAst.class, filterAst.operator(), "Filter content should be an Or operator");

        OrAst t = (OrAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getLeftArgument());
        assertInstanceOf(LiteralAst.class, t.getRightArgument());

        assertEquals("s", ((VarAst) t.getLeftArgument()).name());
        assertEquals("true", ((LiteralAst) t.getRightArgument()).lexical());
    }

    @Test
    void shouldParseNotFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(! ?s)
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(BooleanNotAst.class, filterAst.operator(), "Filter content should be an Not operator");

        BooleanNotAst t = (BooleanNotAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getArgument());

        assertEquals("s", ((VarAst) t.getArgument()).name());
    }

    @Test
    void shouldParseBoundFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(Bound( ?s))
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(BoundAst.class, filterAst.operator(), "Filter content should be an BOUND function");

        BoundAst t = (BoundAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getArgument());

        assertEquals("s", ((VarAst) t.getArgument()).name());
    }

    @Test
    void shouldParseIsIriFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(isIri( ?s))
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(IsIriAst.class, filterAst.operator(), "Filter content should be an IsIRI function");

        IsIriAst t = (IsIriAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getArgument());

        assertEquals("s", ((VarAst) t.getArgument()).name());
    }

    @Test
    void shouldParseIsUriFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(isUri( ?s))
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(IsIriAst.class, filterAst.operator(), "Filter content should be an IsIRI function");

        IsIriAst t = (IsIriAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getArgument());

        assertEquals("s", ((VarAst) t.getArgument()).name());
    }

    @Test
    void shouldParseIsBlankFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(isBlank( ?s))
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(IsBlankAst.class, filterAst.operator(), "Filter content should be an IsIRI function");

        IsBlankAst t = (IsBlankAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getArgument());

        assertEquals("s", ((VarAst) t.getArgument()).name());
    }

    @Test
    void shouldParseIsLiteralFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(isLiteral( ?s))
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(IsLiteralAst.class, filterAst.operator(), "Filter content should be an IsLiteral function");

        IsLiteralAst t = (IsLiteralAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getArgument());

        assertEquals("s", ((VarAst) t.getArgument()).name());
    }

    @Test
    void shouldParseStrEqualsFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(str(?s) = "test")
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(EqualsAst.class, filterAst.operator(), "Filter content should be an Equals operator");

        EqualsAst t = (EqualsAst) filterAst.operator();

        assertInstanceOf(StrAst.class, t.getLeftArgument(), "Equals left argument should be a Str() function");
        assertInstanceOf(LiteralAst.class, t.getRightArgument(), "Equals right argument should be a literal");

        StrAst strAst = (StrAst) t.getLeftArgument();

        assertInstanceOf(VarAst.class, strAst.getArgument());
        assertEquals("s", ((VarAst) strAst.getArgument()).name());
    }

    @Test
    void shouldParseLangEqualsFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(lang(?s) = "test")
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(EqualsAst.class, filterAst.operator(), "Filter content should be an Equals operator");

        EqualsAst t = (EqualsAst) filterAst.operator();

        assertInstanceOf(LangAst.class, t.getLeftArgument(), "Equals left argument should be a Lang() function");
        assertInstanceOf(LiteralAst.class, t.getRightArgument(), "Equals right argument should be a literal");

        LangAst strAst = (LangAst) t.getLeftArgument();

        assertInstanceOf(VarAst.class, strAst.getArgument());
        assertEquals("s", ((VarAst) strAst.getArgument()).name());
    }

    @Test
    void shouldParseDatatypeEqualsFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(datatype(?s) = "test")
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(EqualsAst.class, filterAst.operator(), "Filter content should be an Equals operator");

        EqualsAst t = (EqualsAst) filterAst.operator();

        assertInstanceOf(DatatypeAst.class, t.getLeftArgument(), "Equals left argument should be a Datatype() function");
        assertInstanceOf(LiteralAst.class, t.getRightArgument(), "Equals right argument should be a literal");

        DatatypeAst strAst = (DatatypeAst) t.getLeftArgument();

        assertInstanceOf(VarAst.class, strAst.getArgument());
        assertEquals("s", ((VarAst) strAst.getArgument()).name());
    }

    @Test
    void shouldParseEqualsFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s = <http://example.com>)
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(EqualsAst.class, filterAst.operator(), "Filter content should be an equals (=) operator");

        EqualsAst t = (EqualsAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getLeftArgument());
        assertInstanceOf(IriAst.class, t.getRightArgument());

        assertEquals("s", ((VarAst) t.getLeftArgument()).name());
        assertEquals("<http://example.com>", ((IriAst) t.getRightArgument()).raw());
    }

    @Test
    void shouldParseDifferentFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s != <http://example.com>)
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(DifferentAst.class, filterAst.operator(), "Filter content should be an different (!=) operator");

        DifferentAst t = (DifferentAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getLeftArgument());
        assertInstanceOf(IriAst.class, t.getRightArgument());

        assertEquals("s", ((VarAst) t.getLeftArgument()).name());
        assertEquals("<http://example.com>", ((IriAst) t.getRightArgument()).raw());
    }

    @Test
    void shouldParseLowerFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s < <http://example.com>)
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(LowerThanAst.class, filterAst.operator(), "Filter content should be an lower than (<) operator");

        LowerThanAst t = (LowerThanAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getLeftArgument());
        assertInstanceOf(IriAst.class, t.getRightArgument());

        assertEquals("s", ((VarAst) t.getLeftArgument()).name());
        assertEquals("<http://example.com>", ((IriAst) t.getRightArgument()).raw());
    }

    @Test
    void shouldParseLowerOrEqualFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s <= <http://example.com>)
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(LowerOrEqualThanAst.class, filterAst.operator(), "Filter content should be an lower or equal than (<=) operator");

        LowerOrEqualThanAst t = (LowerOrEqualThanAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getLeftArgument());
        assertInstanceOf(IriAst.class, t.getRightArgument());

        assertEquals("s", ((VarAst) t.getLeftArgument()).name());
        assertEquals("<http://example.com>", ((IriAst) t.getRightArgument()).raw());
    }

    @Test
    void shouldParseGreaterFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s > <http://example.com>)
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(GreaterThanAst.class, filterAst.operator(), "Filter content should be an greater than (>) operator");

        GreaterThanAst t = (GreaterThanAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getLeftArgument());
        assertInstanceOf(IriAst.class, t.getRightArgument());

        assertEquals("s", ((VarAst) t.getLeftArgument()).name());
        assertEquals("<http://example.com>", ((IriAst) t.getRightArgument()).raw());
    }

    @Test
    void shouldParseGreaterOrEqualFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s >= <http://example.com>)
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(GreaterOrEqualThanAst.class, filterAst.operator(), "Filter content should be an greater or equal than (>=) operator");

        GreaterOrEqualThanAst t = (GreaterOrEqualThanAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getLeftArgument());
        assertInstanceOf(IriAst.class, t.getRightArgument());

        assertEquals("s", ((VarAst) t.getLeftArgument()).name());
        assertEquals("<http://example.com>", ((IriAst) t.getRightArgument()).raw());
    }

    @Test
    void shouldParseDivideEqualsFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s / 2 = "test")
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(EqualsAst.class, filterAst.operator(), "Filter content should be an Equals operator");

        EqualsAst t = (EqualsAst) filterAst.operator();

        assertInstanceOf(DivideAst.class, t.getLeftArgument(), "Equals left argument should be a divide operator");
        assertInstanceOf(LiteralAst.class, t.getRightArgument(), "Equals right argument should be a literal");

        DivideAst divAst = (DivideAst) t.getLeftArgument();

        assertInstanceOf(VarAst.class, divAst.getLeftArgument());
        assertEquals("s", ((VarAst) divAst.getLeftArgument()).name());
        assertInstanceOf(LiteralAst.class, divAst.getRightArgument());
        assertEquals("2", ((LiteralAst) divAst.getRightArgument()).lexical());
    }

    @Test
    void shouldParseTimesEqualsFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s * 2 = "test")
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(EqualsAst.class, filterAst.operator(), "Filter content should be an Equals operator");

        EqualsAst t = (EqualsAst) filterAst.operator();

        assertInstanceOf(MultiplyAst.class, t.getLeftArgument(), "Equals left argument should be a multiply operator");
        assertInstanceOf(LiteralAst.class, t.getRightArgument(), "Equals right argument should be a literal");

        MultiplyAst strAst = (MultiplyAst) t.getLeftArgument();

        assertInstanceOf(VarAst.class, strAst.getLeftArgument());
        assertEquals("s", ((VarAst) strAst.getLeftArgument()).name());
        assertInstanceOf(LiteralAst.class, strAst.getRightArgument());
        assertEquals("2", ((LiteralAst) strAst.getRightArgument()).lexical());
    }

    @Test
    void shouldParseAddsEqualsFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s + 2 = "test")
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(EqualsAst.class, filterAst.operator(), "Filter content should be an Equals operator");

        EqualsAst t = (EqualsAst) filterAst.operator();

        assertInstanceOf(AddAst.class, t.getLeftArgument(), "Equals left argument should be a addition operator");
        assertInstanceOf(LiteralAst.class, t.getRightArgument(), "Equals right argument should be a literal");

        AddAst strAst = (AddAst) t.getLeftArgument();

        assertInstanceOf(VarAst.class, strAst.getLeftArgument());
        assertEquals("s", ((VarAst) strAst.getLeftArgument()).name());
        assertInstanceOf(LiteralAst.class, strAst.getRightArgument());
        assertEquals("2", ((LiteralAst) strAst.getRightArgument()).lexical());
    }

    @Test
    void shouldParseSubstractEqualsFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s - 2 = "test")
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(EqualsAst.class, filterAst.operator(), "Filter content should be an Equals operator");

        EqualsAst t = (EqualsAst) filterAst.operator();

        assertInstanceOf(SubtractAst.class, t.getLeftArgument(), "Equals left argument should be a substract (-) operator");
        assertInstanceOf(LiteralAst.class, t.getRightArgument(), "Equals right argument should be a literal");

        SubtractAst strAst = (SubtractAst) t.getLeftArgument();

        assertInstanceOf(VarAst.class, strAst.getLeftArgument());
        assertEquals("s", ((VarAst) strAst.getLeftArgument()).name());
        assertInstanceOf(LiteralAst.class, strAst.getRightArgument());
        assertEquals("2", ((LiteralAst) strAst.getRightArgument()).lexical());
    }

    @Test
    void shouldParseSameTermFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(sameTerm(?s, <http://example.com>))
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(SameTermAst.class, filterAst.operator(), "Filter content should be a sameTerm operator");

        SameTermAst t = (SameTermAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getLeftArgument());
        assertInstanceOf(IriAst.class, t.getRightArgument());

        assertEquals("s", ((VarAst) t.getLeftArgument()).name());
        assertEquals("<http://example.com>", ((IriAst) t.getRightArgument()).raw());
    }

    @Test
    void shouldParseLangMatchesFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(langMatches(?s, "test"))
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(LangMatchesAst.class, filterAst.operator(), "Filter content should be an langMatches (=) operator");

        LangMatchesAst t = (LangMatchesAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getLeftArgument());
        assertInstanceOf(LiteralAst.class, t.getRightArgument());

        assertEquals("s", ((VarAst) t.getLeftArgument()).name());
        assertEquals("\"test\"", ((LiteralAst) t.getRightArgument()).lexical());
    }

    @Test
    void shouldParseBinaryRegexFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(REGEX(?s, "test"))
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(BinaryRegexAst.class, filterAst.operator(), "Filter content should be an langMatches (=) operator");

        BinaryRegexAst t = (BinaryRegexAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getLeftArgument());
        assertInstanceOf(LiteralAst.class, t.getRightArgument());

        assertEquals("s", ((VarAst) t.getLeftArgument()).name());
        assertEquals("\"test\"", ((LiteralAst) t.getRightArgument()).lexical());
    }

    @Test
    void shouldParseTrinaryRegexFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(REGEX(?s, "test", "i"))
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(TrinaryRegexAst.class, filterAst.operator(), "Filter content should be an langMatches (=) operator");

        TrinaryRegexAst t = (TrinaryRegexAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.string());
        assertInstanceOf(LiteralAst.class, t.pattern());
        assertInstanceOf(LiteralAst.class, t.flags());

        assertEquals("s", ((VarAst) t.string()).name());
        assertEquals("\"test\"", ((LiteralAst) t.pattern()).lexical());
        assertEquals("\"i\"", ((LiteralAst) t.flags()).lexical());
    }

    @Test
    void shouldParseFunCallFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(<http://example.org/function>(?s, "test"))
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(FunctionCallAst.class, filterAst.operator(), "Filter content should be afunction call operator");

        FunctionCallAst t = (FunctionCallAst) filterAst.operator();

        assertInstanceOf(IriAst.class, t.functionName());
        assertInstanceOf(List.class, t.arguments());
        assertEquals(2, t.arguments().size());
        assertInstanceOf(VarAst.class, t.arguments().getFirst());
        assertInstanceOf(LiteralAst.class, t.arguments().getLast());

        assertEquals("<http://example.org/function>", ((IriAst) t.functionName()).raw());
        assertEquals("s", ((VarAst) t.arguments().getFirst()).name());
        assertEquals("\"test\"", ((LiteralAst) t.arguments().getLast()).lexical());
    }
}
