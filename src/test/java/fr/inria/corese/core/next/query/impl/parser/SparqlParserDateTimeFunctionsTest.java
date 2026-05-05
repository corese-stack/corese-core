package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.BindAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.DayAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.HoursAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.MinutesAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.MonthAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.NowAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.NumericExpressionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.SecondsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.SimpleLiteralExpressionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.TimezoneAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.TzAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.XsdDateTimeExpressionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.XsdDayTimeDurationExpressionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.YearAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : Functions on dates and time")
class SparqlParserDateTimeFunctionsTest extends AbstractSparqlParserFeatureTest {

    // ── NOW() ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BIND(NOW() AS ?now)")
    void shouldParseNowInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(NOW() AS ?now)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        assertInstanceOf(NowAst.class, bind.expression());
        assertInstanceOf(XsdDateTimeExpressionAst.class, bind.expression());
        assertEquals("now", bind.variable().name());
    }

    @Test
    @DisplayName("ORDER BY NOW() — no variable, should be valid")
    void shouldParseOrderByNow() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?s WHERE {
                  ?s ?p ?o .
                } ORDER BY NOW()
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        assertInstanceOf(NowAst.class, select.solutionModifier().orderBy().getFirst().expression());
    }

    // ── YEAR() ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BIND(YEAR(?date) AS ?y)")
    void shouldParseYearInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s <http://example.org/date> ?date .
                  BIND(YEAR(?date) AS ?y)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        YearAst year = assertInstanceOf(YearAst.class, bind.expression());
        assertInstanceOf(NumericExpressionAst.class, year);
        assertEquals("date", assertInstanceOf(VarAst.class, year.getArgument()).name());
    }

    @Test
    @DisplayName("ORDER BY YEAR(?date)")
    void shouldParseOrderByYear() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?date WHERE {
                  ?s <http://example.org/date> ?date .
                } ORDER BY YEAR(?date)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        assertInstanceOf(YearAst.class, select.solutionModifier().orderBy().getFirst().expression());
    }

    @Test
    @DisplayName("ORDER BY YEAR(?missing) — should reject variable not visible in WHERE")
    void shouldRejectOrderByYearWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?date WHERE {
                  ?s <http://example.org/date> ?date .
                } ORDER BY YEAR(?missing)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }

    // ── MONTH() ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BIND(MONTH(?date) AS ?m)")
    void shouldParseMonthInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s <http://example.org/date> ?date .
                  BIND(MONTH(?date) AS ?m)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        MonthAst month = assertInstanceOf(MonthAst.class, bind.expression());
        assertInstanceOf(NumericExpressionAst.class, month);
        assertEquals("date", assertInstanceOf(VarAst.class, month.getArgument()).name());
    }

    // ── DAY() ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BIND(DAY(?date) AS ?d)")
    void shouldParseDayInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s <http://example.org/date> ?date .
                  BIND(DAY(?date) AS ?d)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        DayAst day = assertInstanceOf(DayAst.class, bind.expression());
        assertInstanceOf(NumericExpressionAst.class, day);
        assertEquals("date", assertInstanceOf(VarAst.class, day.getArgument()).name());
    }

    // ── HOURS() ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BIND(HOURS(?dt) AS ?h)")
    void shouldParseHoursInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s <http://example.org/dt> ?dt .
                  BIND(HOURS(?dt) AS ?h)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        HoursAst hours = assertInstanceOf(HoursAst.class, bind.expression());
        assertInstanceOf(NumericExpressionAst.class, hours);
        assertEquals("dt", assertInstanceOf(VarAst.class, hours.getArgument()).name());
    }

    // ── MINUTES() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BIND(MINUTES(?dt) AS ?min)")
    void shouldParseMinutesInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s <http://example.org/dt> ?dt .
                  BIND(MINUTES(?dt) AS ?min)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        MinutesAst minutes = assertInstanceOf(MinutesAst.class, bind.expression());
        assertInstanceOf(NumericExpressionAst.class, minutes);
        assertEquals("dt", assertInstanceOf(VarAst.class, minutes.getArgument()).name());
    }

    // ── SECONDS() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BIND(SECONDS(?dt) AS ?sec)")
    void shouldParseSecondsInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s <http://example.org/dt> ?dt .
                  BIND(SECONDS(?dt) AS ?sec)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        SecondsAst seconds = assertInstanceOf(SecondsAst.class, bind.expression());
        assertInstanceOf(NumericExpressionAst.class, seconds);
        assertEquals("dt", assertInstanceOf(VarAst.class, seconds.getArgument()).name());
    }

    // ── TIMEZONE() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("BIND(TIMEZONE(?dt) AS ?tz)")
    void shouldParseTimezoneInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s <http://example.org/dt> ?dt .
                  BIND(TIMEZONE(?dt) AS ?tz)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        TimezoneAst timezone = assertInstanceOf(TimezoneAst.class, bind.expression());
        assertInstanceOf(XsdDayTimeDurationExpressionAst.class, timezone);
        assertEquals("dt", assertInstanceOf(VarAst.class, timezone.getArgument()).name());
    }

    // ── TZ() ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BIND(TZ(?dt) AS ?tzStr)")
    void shouldParseTzInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s <http://example.org/dt> ?dt .
                  BIND(TZ(?dt) AS ?tzStr)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        TzAst tz = assertInstanceOf(TzAst.class, bind.expression());
        assertInstanceOf(SimpleLiteralExpressionAst.class, tz);
        assertEquals("dt", assertInstanceOf(VarAst.class, tz.getArgument()).name());
    }

    @Test
    @DisplayName("ORDER BY TZ(?dt)")
    void shouldParseOrderByTz() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?dt WHERE {
                  ?s <http://example.org/dt> ?dt .
                } ORDER BY TZ(?dt)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        assertInstanceOf(TzAst.class, select.solutionModifier().orderBy().getFirst().expression());
    }

    @Test
    @DisplayName("ORDER BY TZ(?missing) — should reject variable not visible in WHERE")
    void shouldRejectOrderByTzWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?dt WHERE {
                  ?s <http://example.org/dt> ?dt .
                } ORDER BY TZ(?missing)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
