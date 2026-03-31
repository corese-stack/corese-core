package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.AddAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.AndAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BinaryRegexAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BooleanNotAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BoundAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.DatatypeAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.DifferentAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.DivideAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EqualsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.FunctionCallAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.GreaterOrEqualThanAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.GreaterThanAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.IsBlankAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.IsIriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.IsLiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.LangAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.LangMatchesAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.LowerOrEqualThanAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.LowerThanAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.MultiplyAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.OrAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.SameTermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.SubtractAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.TrinaryRegexAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.UnaryMinusAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.UnaryPlusAst;

class SparqlParserFilterTest extends AbstractSparqlParserFeatureTest {

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

        SameTermAst sameTermAst = (SameTermAst) filterAst.operator();
        assertNotNull(sameTermAst.getLeftArgument());
        assertInstanceOf(VarAst.class, sameTermAst.getLeftArgument());
        assertNotNull(sameTermAst.getRightArgument());
        assertInstanceOf(IriAst.class, sameTermAst.getRightArgument());

        assertEquals("s", ((VarAst) sameTermAst.getLeftArgument()).name());
        assertEquals("<http://example.com>", ((IriAst) sameTermAst.getRightArgument()).raw());
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

        assertInstanceOf(VarAst.class, t.getString());
        assertEquals("s", ((VarAst) t.getString()).name());
        assertInstanceOf(LiteralAst.class, t.getPattern());
        assertEquals("\"test\"", ((LiteralAst) t.getPattern()).lexical());
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

        assertInstanceOf(VarAst.class, t.getString());
        assertInstanceOf(LiteralAst.class, t.getPattern());
        assertInstanceOf(LiteralAst.class, t.getFlags());

        assertEquals("s", ((VarAst) t.getString()).name());
        assertEquals("\"test\"", ((LiteralAst) t.getPattern()).lexical());
        assertEquals("\"i\"", ((LiteralAst) t.getFlags()).lexical());
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

    @Test
    void shouldParseAddsUnaryMinusEqualsFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s + - 2 = "test")
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

        AddAst addAst = (AddAst) t.getLeftArgument();

        assertInstanceOf(VarAst.class, addAst.getLeftArgument());
        assertEquals("s", ((VarAst) addAst.getLeftArgument()).name());
        assertInstanceOf(UnaryMinusAst.class, addAst.getRightArgument());
        UnaryMinusAst rightUnaryMinusAst = (UnaryMinusAst) addAst.getRightArgument();
        assertInstanceOf(LiteralAst.class, rightUnaryMinusAst.getArgument());
        assertEquals("2", ((LiteralAst) rightUnaryMinusAst.getArgument()).lexical());
    }

    @Test
    void shouldParseAddSubChainEqualsFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s * 2 + ?o - 5 = "test")
                }
                """);

        // Equal(Sub(Add(Mul(Var s ,Literal 2), Var o), Literal 5), Literal "test")

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(EqualsAst.class, filterAst.operator(), "Filter content should be an Equals operator");

        // Equal(Sub(...), Literal "test")
        EqualsAst equalsAst = (EqualsAst) filterAst.operator();
        assertInstanceOf(SubtractAst.class, equalsAst.getLeftArgument(), "Equals left argument should be a addtion operator");
        assertInstanceOf(LiteralAst.class, equalsAst.getRightArgument(), "Equals right argument should be a literal");
        assertEquals("\"test\"", ((LiteralAst) equalsAst.getRightArgument()).lexical());

        // Sub(...), Literal 5)
        SubtractAst subtractAst = (SubtractAst) equalsAst.getLeftArgument();
        assertInstanceOf(AddAst.class, subtractAst.getLeftArgument());
        assertInstanceOf(LiteralAst.class, subtractAst.getRightArgument());
        assertEquals("5", ((LiteralAst) subtractAst.getRightArgument()).lexical());

        // Add(Mul(...), Var o))
        AddAst addAst1 = (AddAst) subtractAst.getLeftArgument();
        assertInstanceOf(MultiplyAst.class, addAst1.getLeftArgument());
        MultiplyAst multiplyAst1 = (MultiplyAst) addAst1.getLeftArgument();
        assertInstanceOf(VarAst.class, addAst1.getRightArgument());
        assertEquals("o", ((VarAst) addAst1.getRightArgument()).name());

        // Mul(Var s ,Literal 2)
        assertInstanceOf(VarAst.class, multiplyAst1.getLeftArgument());
        assertEquals("s", ((VarAst) multiplyAst1.getLeftArgument()).name());
        assertInstanceOf(LiteralAst.class, multiplyAst1.getRightArgument());
        assertEquals("2", ((LiteralAst) multiplyAst1.getRightArgument()).lexical());

    }

    @Test
    void shouldParseUnaryPlusFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(+ ?s)
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(UnaryPlusAst.class, filterAst.operator(), "Filter content should be a unary plus operator");

        UnaryPlusAst t = (UnaryPlusAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getArgument());

        assertEquals("s", ((VarAst) t.getArgument()).name());
    }

    @Test
    void shouldParseUnaryMinusFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(- ?s)
                }
                """);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 pattern (BGP + FILTER)");

        PatternAst p2 = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, p2, "Last pattern should be a filter");

        FilterAst filterAst = (FilterAst) p2;
        assertInstanceOf(UnaryMinusAst.class, filterAst.operator(), "Filter content should be a unary minus operator");

        UnaryMinusAst t = (UnaryMinusAst) filterAst.operator();

        assertInstanceOf(VarAst.class, t.getArgument());
        assertEquals("s", ((VarAst) t.getArgument()).name());
    }

    @Test
    void shouldParseChainedOrFilterLeftAssociatively() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?a || ?b || ?c)
                }
                """);

        FilterAst filterAst = (FilterAst) ast.whereClause().patterns().getLast();
        assertInstanceOf(OrAst.class, filterAst.operator(), "Filter content should be an Or operator");

        OrAst root = (OrAst) filterAst.operator();
        assertInstanceOf(OrAst.class, root.getLeftArgument(), "Chained OR should be folded into a binary tree");
        assertInstanceOf(VarAst.class, root.getRightArgument());
        assertEquals("c", ((VarAst) root.getRightArgument()).name());

        OrAst left = (OrAst) root.getLeftArgument();
        assertInstanceOf(VarAst.class, left.getLeftArgument());
        assertInstanceOf(VarAst.class, left.getRightArgument());
        assertEquals("a", ((VarAst) left.getLeftArgument()).name());
        assertEquals("b", ((VarAst) left.getRightArgument()).name());
    }

    @Test
    void shouldParseChainedAndFilterLeftAssociatively() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?a && ?b && ?c)
                }
                """);

        FilterAst filterAst = (FilterAst) ast.whereClause().patterns().getLast();
        assertInstanceOf(AndAst.class, filterAst.operator(), "Filter content should be an And operator");

        AndAst root = (AndAst) filterAst.operator();
        assertInstanceOf(AndAst.class, root.getLeftArgument(), "Chained AND should be folded into a binary tree");
        assertInstanceOf(VarAst.class, root.getRightArgument());
        assertEquals("c", ((VarAst) root.getRightArgument()).name());

        AndAst left = (AndAst) root.getLeftArgument();
        assertInstanceOf(VarAst.class, left.getLeftArgument());
        assertInstanceOf(VarAst.class, left.getRightArgument());
        assertEquals("a", ((VarAst) left.getLeftArgument()).name());
        assertEquals("b", ((VarAst) left.getRightArgument()).name());
    }

    @Test
    void shouldParseSignedNegativeLiteralInAdditiveExpression() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s - -2 = "test")
                }
                """);

        FilterAst filterAst = (FilterAst) ast.whereClause().patterns().getLast();
        EqualsAst equalsAst = (EqualsAst) filterAst.operator();

        assertInstanceOf(SubtractAst.class, equalsAst.getLeftArgument());
        SubtractAst subtractAst = (SubtractAst) equalsAst.getLeftArgument();

        assertInstanceOf(VarAst.class, subtractAst.getLeftArgument());
        assertEquals("s", ((VarAst) subtractAst.getLeftArgument()).name());
        assertInstanceOf(LiteralAst.class, subtractAst.getRightArgument());
        assertEquals("-2", ((LiteralAst) subtractAst.getRightArgument()).lexical());
    }

    @Test
    void shouldParseSignedPositiveLiteralInAdditiveExpression() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s + +2 = "test")
                }
                """);

        FilterAst filterAst = (FilterAst) ast.whereClause().patterns().getLast();
        EqualsAst equalsAst = (EqualsAst) filterAst.operator();

        assertInstanceOf(AddAst.class, equalsAst.getLeftArgument());
        AddAst addAst = (AddAst) equalsAst.getLeftArgument();

        assertInstanceOf(VarAst.class, addAst.getLeftArgument());
        assertEquals("s", ((VarAst) addAst.getLeftArgument()).name());
        assertInstanceOf(LiteralAst.class, addAst.getRightArgument());
        assertEquals("+2", ((LiteralAst) addAst.getRightArgument()).lexical());
    }

    @Test
    void shouldParseMixedMultiplyThenDivideLeftAssociatively() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s * 2 / 3 = "test")
                }
                """);

        FilterAst filterAst = (FilterAst) ast.whereClause().patterns().getLast();
        EqualsAst equalsAst = (EqualsAst) filterAst.operator();

        assertInstanceOf(DivideAst.class, equalsAst.getLeftArgument());
        DivideAst divideAst = (DivideAst) equalsAst.getLeftArgument();
        assertInstanceOf(MultiplyAst.class, divideAst.getLeftArgument());
        assertInstanceOf(LiteralAst.class, divideAst.getRightArgument());
        assertEquals("3", ((LiteralAst) divideAst.getRightArgument()).lexical());

        MultiplyAst multiplyAst = (MultiplyAst) divideAst.getLeftArgument();
        assertInstanceOf(VarAst.class, multiplyAst.getLeftArgument());
        assertEquals("s", ((VarAst) multiplyAst.getLeftArgument()).name());
        assertInstanceOf(LiteralAst.class, multiplyAst.getRightArgument());
        assertEquals("2", ((LiteralAst) multiplyAst.getRightArgument()).lexical());
    }

    @Test
    void shouldParseMixedDivideThenMultiplyLeftAssociatively() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(?s / 2 * 3 = "test")
                }
                """);

        FilterAst filterAst = (FilterAst) ast.whereClause().patterns().getLast();
        EqualsAst equalsAst = (EqualsAst) filterAst.operator();

        assertInstanceOf(MultiplyAst.class, equalsAst.getLeftArgument());
        MultiplyAst multiplyAst = (MultiplyAst) equalsAst.getLeftArgument();
        assertInstanceOf(DivideAst.class, multiplyAst.getLeftArgument());
        assertInstanceOf(LiteralAst.class, multiplyAst.getRightArgument());
        assertEquals("3", ((LiteralAst) multiplyAst.getRightArgument()).lexical());

        DivideAst divideAst = (DivideAst) multiplyAst.getLeftArgument();
        assertInstanceOf(VarAst.class, divideAst.getLeftArgument());
        assertEquals("s", ((VarAst) divideAst.getLeftArgument()).name());
        assertInstanceOf(LiteralAst.class, divideAst.getRightArgument());
        assertEquals("2", ((LiteralAst) divideAst.getRightArgument()).lexical());
    }

    @Test
    void shouldParseExistsFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER EXISTS { ?s ex:email ?e }
                }
                """);

        assertNotNull(ast);
        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size());

        PatternAst last = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, last);

        FilterAst filterAst = (FilterAst) last;
        assertInstanceOf(ExistsAst.class, filterAst.operator());

        ExistsAst existsAst = (ExistsAst) filterAst.operator();
        assertNotNull(existsAst.pattern());
        assertFalse(existsAst.pattern().patterns().isEmpty());
    }

    @Test
    void shouldParseNotExistsFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER NOT EXISTS { ?s ex:email ?e }
                }
                """);

        assertNotNull(ast);
        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(2, where.patterns().size());

        PatternAst last = where.patterns().getLast();
        assertInstanceOf(FilterAst.class, last);

        FilterAst filterAst = (FilterAst) last;
        assertInstanceOf(NotExistsAst.class, filterAst.operator());

        NotExistsAst notExistsAst = (NotExistsAst) filterAst.operator();
        assertNotNull(notExistsAst.pattern());
        assertFalse(notExistsAst.pattern().patterns().isEmpty());
    }

    @Test
    void shouldParseExistsWithMultipleTriples() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER EXISTS {
                    ?s ex:email ?e .
                    ?s ex:name ?n
                  }
                }
                """);

        assertNotNull(ast);
        FilterAst filterAst = (FilterAst) ast.whereClause().patterns().getLast();
        assertInstanceOf(ExistsAst.class, filterAst.operator());

        ExistsAst existsAst = (ExistsAst) filterAst.operator();
        assertNotNull(existsAst.pattern());
    }

    @Test
    void shouldParseNotExistsEmpty() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER NOT EXISTS { }
                }
                """);

        assertNotNull(ast);
        FilterAst filterAst = (FilterAst) ast.whereClause().patterns().getLast();
        assertInstanceOf(NotExistsAst.class, filterAst.operator());

        NotExistsAst notExistsAst = (NotExistsAst) filterAst.operator();
        assertNotNull(notExistsAst.pattern());
        assertTrue(notExistsAst.pattern().patterns().isEmpty());
    }
}
