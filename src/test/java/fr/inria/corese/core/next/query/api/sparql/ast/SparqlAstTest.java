package fr.inria.corese.core.next.query.api.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAstTestSupport.simplePredicateTerm;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SPARQL AST types in {@code fr.inria.corese.core.next.query.sparql.ast}:
 * term types (VarAst, IriAst, LiteralAst), TriplePatternAst, BgpAst, GroupGraphPatternAst, QueryAst.
 *
 */
class SparqlAstTest {

    // ---------- VarAst ----------

    @Nested
    @DisplayName("VarAst")
    class VarAstTest {

        @Test
        @DisplayName("creates with valid name")
        void validName() {
            VarAst v = new VarAst("s");
            assertEquals("s", v.name());
        }

        @Test
        @DisplayName("creates with name containing digits and underscore")
        void nameWithDigitsAndUnderscore() {
            VarAst v = new VarAst("var_123");
            assertEquals("var_123", v.name());
        }

        @Test
        @DisplayName("throws when name is null")
        void nullName() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new VarAst(null));
            assertTrue(e.getMessage().contains("null") || e.getMessage().contains("blank"));
        }

        @Test
        @DisplayName("throws when name is blank")
        void blankName() {
            assertThrows(IllegalArgumentException.class, () -> new VarAst(""));
            assertThrows(IllegalArgumentException.class, () -> new VarAst("   "));
            assertThrows(IllegalArgumentException.class, () -> new VarAst("\t\n"));
        }

        @Test
        @DisplayName("equals and hashCode for same name")
        void equality() {
            VarAst a = new VarAst("x");
            VarAst b = new VarAst("x");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("not equal for different names")
        void inequality() {
            assertNotEquals(new VarAst("s"), new VarAst("p"));
        }

        @Test
        @DisplayName("implements TermAst")
        void implementsTermAst() {
            assertInstanceOf(TermAst.class, new VarAst("s"));
        }
    }

    // ---------- IriAst ----------

    @Nested
    @DisplayName("IriAst")
    class IriAstTest {

        @Test
        @DisplayName("creates with valid raw IRI")
        void validRaw() {
            IriAst i = new IriAst("<http://example.org/>");
            assertEquals("<http://example.org/>", i.raw());
        }

        @Test
        @DisplayName("creates with empty string")
        void emptyStringAllowed() {
            IriAst i = new IriAst("");
            assertEquals("", i.raw());
        }

        @Test
        @DisplayName("creates with QName")
        void qname() {
            IriAst i = new IriAst("foaf:Person");
            assertEquals("foaf:Person", i.raw());
        }

        @Test
        @DisplayName("throws when raw is null")
        void nullRaw() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new IriAst(null));
            assertTrue(e.getMessage().contains("null"));
        }

        @Test
        @DisplayName("equals and hashCode for same raw")
        void equality() {
            IriAst a = new IriAst("a");
            IriAst b = new IriAst("a");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("implements TermAst")
        void implementsTermAst() {
            assertInstanceOf(TermAst.class, new IriAst("x"));
        }
    }

    // ---------- LiteralAst ----------

    @Nested
    @DisplayName("LiteralAst")
    class LiteralAstTest {

        @Test
        @DisplayName("creates with lexical only")
        void lexicalOnly() {
            LiteralAst l = new LiteralAst("hello", null, null);
            assertEquals("hello", l.lexical());
            assertNull(l.lang());
            assertNull(l.datatype());
        }

        @Test
        @DisplayName("creates with language tag")
        void withLang() {
            LiteralAst l = new LiteralAst("\"salut\"", "fr", null);
            assertEquals("\"salut\"", l.lexical());
            assertEquals("fr", l.lang());
            assertNull(l.datatype());
        }

        @Test
        @DisplayName("creates with datatype")
        void withDatatype() {
            LiteralAst l = new LiteralAst("\"42\"", null, "xsd:integer");
            assertEquals("\"42\"", l.lexical());
            assertNull(l.lang());
            assertEquals("xsd:integer", l.datatype());
        }

        @Test
        @DisplayName("creates with lang and datatype (grammar usually has one)")
        void withLangAndDatatype() {
            LiteralAst l = new LiteralAst("x", "en", "xsd:string");
            assertEquals("x", l.lexical());
            assertEquals("en", l.lang());
            assertEquals("xsd:string", l.datatype());
        }

        @Test
        @DisplayName("throws when lexical is null")
        void nullLexical() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> new LiteralAst(null, null, null));
            assertTrue(e.getMessage().contains("null"));
        }

        @Test
        @DisplayName("equals and hashCode for same components")
        void equality() {
            LiteralAst a = new LiteralAst("x", "fr", null);
            LiteralAst b = new LiteralAst("x", "fr", null);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("not equal when lang or datatype differs")
        void inequality() {
            LiteralAst a = new LiteralAst("x", null, null);
            assertNotEquals(new LiteralAst("x", "fr", null), a);
            assertNotEquals(new LiteralAst("x", null, "xsd:string"), a);
        }

        @Test
        @DisplayName("implements TermAst")
        void implementsTermAst() {
            assertInstanceOf(TermAst.class, new LiteralAst("x", null, null));
        }
    }

    // ---------- TriplePatternAst ----------

    @Nested
    @DisplayName("TriplePatternAst")
    class TriplePatternAstTest {

        private final VarAst s = new VarAst("s");
        private final VarAst p = new VarAst("p");
        private final VarAst o = new VarAst("o");

        @Test
        @DisplayName("creates with three terms")
        void valid() {
            TriplePatternAst t = TriplePatternAst.of(s, p, o);
            assertSame(s, t.subject());
            assertSame(p, simplePredicateTerm(t));
            assertSame(o, t.object());
        }

        @Test
        @DisplayName("creates with mixed term types")
        void mixedTerms() {
            IriAst pred = new IriAst("a");
            LiteralAst obj = new LiteralAst("lit", null, null);
            TriplePatternAst t = TriplePatternAst.of(s, pred, obj);
            assertInstanceOf(VarAst.class, t.subject());
            assertInstanceOf(IriAst.class, simplePredicateTerm(t));
            assertInstanceOf(LiteralAst.class, t.object());
        }

        @Test
        @DisplayName("throws when subject is null")
        void nullSubject() {
            assertThrows(IllegalArgumentException.class, () -> TriplePatternAst.of(null, p, o));
        }

        @Test
        @DisplayName("throws when predicate is null")
        void nullPredicate() {
            assertThrows(IllegalArgumentException.class, () -> TriplePatternAst.of(s, null, o));
        }

        @Test
        @DisplayName("throws when object is null")
        void nullObject() {
            assertThrows(IllegalArgumentException.class, () -> TriplePatternAst.of(s, p, null));
        }

        @Test
        @DisplayName("equals and hashCode for same triple")
        void equality() {
            TriplePatternAst a = TriplePatternAst.of(s, p, o);
            TriplePatternAst b = TriplePatternAst.of(new VarAst("s"), new VarAst("p"), new VarAst("o"));
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("not equal when component differs")
        void inequality() {
            TriplePatternAst t = TriplePatternAst.of(s, p, o);
            assertNotEquals(t, TriplePatternAst.of(new VarAst("x"), p, o));
        }
    }

    // ---------- BgpAst ----------

    @Nested
    @DisplayName("BgpAst")
    class BgpAstTest {

        private final TriplePatternAst triple = TriplePatternAst.of(
                new VarAst("s"), new VarAst("p"), new VarAst("o"));

        @Test
        @DisplayName("creates with empty list")
        void emptyList() {
            BgpAst bgp = new BgpAst(List.of());
            assertNotNull(bgp.triples());
            assertTrue(bgp.triples().isEmpty());
        }

        @Test
        @DisplayName("creates with null -> empty list")
        void nullBecomesEmpty() {
            BgpAst bgp = new BgpAst(null);
            assertNotNull(bgp.triples());
            assertTrue(bgp.triples().isEmpty());
        }

        @Test
        @DisplayName("creates with triples and returns defensive copy")
        void defensiveCopy() {
            List<TriplePatternAst> mutable = new ArrayList<>(List.of(triple));
            BgpAst bgp = new BgpAst(mutable);
            mutable.clear();
            assertEquals(1, bgp.triples().size());
            assertEquals(triple, bgp.triples().get(0));
        }

        @Test
        @DisplayName("returned list is unmodifiable")
        void unmodifiable() {
            BgpAst bgp = new BgpAst(List.of(triple));
            assertThrows(UnsupportedOperationException.class, () -> bgp.triples().add(triple));
        }

        @Test
        @DisplayName("equals and hashCode for same triples")
        void equality() {
            BgpAst a = new BgpAst(List.of(triple));
            BgpAst b = new BgpAst(List.of(triple));
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("implements PatternAst")
        void implementsPatternAst() {
            assertInstanceOf(PatternAst.class, new BgpAst(List.of()));
        }
    }

    // ---------- GroupGraphPatternAst ----------

    @Nested
    @DisplayName("GroupGraphPatternAst")
    class GroupGraphPatternAstTest {

        private final BgpAst bgp = new BgpAst(List.of(
                TriplePatternAst.of(new VarAst("s"), new VarAst("p"), new VarAst("o"))));

        @Test
        @DisplayName("creates with empty list")
        void emptyList() {
            GroupGraphPatternAst g = new GroupGraphPatternAst(List.of());
            assertNotNull(g.patterns());
            assertTrue(g.patterns().isEmpty());
        }

        @Test
        @DisplayName("creates with null -> empty list")
        void nullBecomesEmpty() {
            GroupGraphPatternAst g = new GroupGraphPatternAst(null);
            assertNotNull(g.patterns());
            assertTrue(g.patterns().isEmpty());
        }

        @Test
        @DisplayName("creates with patterns and returns defensive copy")
        void defensiveCopy() {
            List<PatternAst> mutable = new ArrayList<>(List.of(bgp));
            GroupGraphPatternAst g = new GroupGraphPatternAst(mutable);
            mutable.clear();
            assertEquals(1, g.patterns().size());
            assertSame(bgp, g.patterns().getFirst());
        }

        @Test
        @DisplayName("returned list is unmodifiable")
        void unmodifiable() {
            GroupGraphPatternAst g = new GroupGraphPatternAst(List.of(bgp));
            assertThrows(UnsupportedOperationException.class, () -> g.patterns().add(bgp));
        }

        @Test
        @DisplayName("equals and hashCode for same patterns")
        void equality() {
            GroupGraphPatternAst a = new GroupGraphPatternAst(List.of(bgp));
            GroupGraphPatternAst b = new GroupGraphPatternAst(List.of(bgp));
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }

    // ---------- QueryAst / SelectQueryAst ----------

    @Nested
    @DisplayName("QueryAst / SelectQueryAst")
    class QuerySelectAstTest {

        @Test
        @DisplayName("SelectQueryAst stores and returns whereClause via whereClause()")
        void whereClauseAccessor() {
            GroupGraphPatternAst where = new GroupGraphPatternAst(List.of(
                    new BgpAst(List.of(TriplePatternAst.of(
                            new VarAst("s"), new VarAst("p"), new VarAst("o"))))));
            SparqlQueryAst q = new SelectQueryAst(where);
            assertSame(where, q.whereClause());
        }

        @Test
        @DisplayName("SelectQueryAst record equality when same whereClause")
        void selectQueryAstEquality() {
            GroupGraphPatternAst where = new GroupGraphPatternAst(List.of());
            SelectQueryAst a = new SelectQueryAst(where);
            SelectQueryAst b = new SelectQueryAst(where);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("SelectQueryAst with null whereClause uses empty group")
        void nullWhereClauseDefaultsToEmpty() {
            SelectQueryAst q = new SelectQueryAst(null);
            assertNotNull(q.whereClause());
            assertTrue(q.whereClause().patterns().isEmpty());
        }

        @Test
        @DisplayName("SelectQueryAst implements QueryAst")
        void implementsQueryAst() {
            assertInstanceOf(QueryAst.class, new SelectQueryAst(new GroupGraphPatternAst(List.of())));
        }
    }

    // ---------- QueryAst / AskQueryAst ----------

    @Nested
    @DisplayName("QueryAst / AskQueryAst")
    class QueryAskAstTest {

        @Test
        @DisplayName("AskQueryAst stores and returns whereClause via whereClause()")
        void whereClauseAccessor() {
            GroupGraphPatternAst where = new GroupGraphPatternAst(List.of(
                    new BgpAst(List.of(TriplePatternAst.of(
                            new VarAst("s"), new VarAst("p"), new VarAst("o"))))));
            SparqlQueryAst q = new AskQueryAst(DatasetClauseAst.none(), where);
            assertSame(where, q.whereClause());
        }

        @Test
        @DisplayName("AskQueryAst record equality when same whereClause")
        void askQueryAstEquality() {
            GroupGraphPatternAst where = new GroupGraphPatternAst(List.of());
            AskQueryAst a = new AskQueryAst(DatasetClauseAst.none(), where);
            AskQueryAst b = new AskQueryAst(DatasetClauseAst.none(), where);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("AskQueryAst with null whereClause uses empty group")
        void nullWhereClauseDefaultsToEmpty() {
            AskQueryAst q = new AskQueryAst(DatasetClauseAst.none(), null);
            assertNotNull(q.whereClause());
            assertTrue(q.whereClause().patterns().isEmpty());
        }

        @Test
        @DisplayName("AskQueryAst implements QueryAst")
        void implementsQueryAst() {
            assertInstanceOf(QueryAst.class, new AskQueryAst(DatasetClauseAst.none(), new GroupGraphPatternAst(List.of())));
        }
    }

    // ---------- TermAst / PatternAst typing ----------

    @Nested
    @DisplayName("AST type hierarchy")
    class TypeHierarchyTest {

        @Test
        @DisplayName("VarAst, IriAst, LiteralAst are TermAst")
        void termImplementations() {
            assertInstanceOf(TermAst.class, new VarAst("x"));
            assertInstanceOf(TermAst.class, new IriAst("y"));
            assertInstanceOf(TermAst.class, new LiteralAst("z", null, null));
        }

        @Test
        @DisplayName("BgpAst is PatternAst")
        void patternImplementation() {
            assertInstanceOf(PatternAst.class, new BgpAst(List.of()));
        }
    }

    // ---------- Record toString (coverage / debugging) ----------

    @Nested
    @DisplayName("toString")
    class ToStringTest {

        @Test
        @DisplayName("VarAst toString contains name")
        void varAst() {
            assertTrue(new VarAst("s").toString().contains("s"));
        }

        @Test
        @DisplayName("TriplePatternAst toString is non-empty")
        void triplePatternAst() {
            TriplePatternAst t = TriplePatternAst.of(
                    new VarAst("s"), new VarAst("p"), new VarAst("o"));
            assertNotNull(t.toString());
            assertFalse(t.toString().isEmpty());
        }

        @Test
        @DisplayName("SelectQueryAst whereClause() used in composition")
        void queryAstWithNestedStructures() {
            BgpAst bgp = new BgpAst(List.of(
                    TriplePatternAst.of(new VarAst("s"), new IriAst("a"), new VarAst("o"))));
            GroupGraphPatternAst where = new GroupGraphPatternAst(List.of(bgp));
            SparqlQueryAst q = new SelectQueryAst(where);
            assertEquals(1, q.whereClause().patterns().size());
            assertEquals(1, ((BgpAst) q.whereClause().patterns().get(0)).triples().size());
        }
    }
}
