package fr.inria.corese.core.next.kgram.core;

import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.core.Eval;
import fr.inria.corese.core.next.query.kgram.core.Mapping;
import fr.inria.corese.core.next.query.kgram.core.Mappings;
import fr.inria.corese.core.next.query.kgram.core.Query;
import fr.inria.corese.core.sparql.api.IDatatype;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Mappings class.
 * Tests collection management, aggregates, sorting, and query operations.
 */
@DisplayName("Mappings Tests")
class MappingsTest {

    private Mappings mappings;
    private Query mockQuery;
    private Mapping mockMapping;
    private Node mockNode;

    @BeforeEach
    void setUp() {
        mockQuery = mock(Query.class);
        when(mockQuery.getSelect()).thenReturn(new ArrayList<>());
        when(mockQuery.getOrderBy()).thenReturn(new ArrayList<>());
        when(mockQuery.getGroupBy()).thenReturn(new ArrayList<>());
        when(mockQuery.isDistinct()).thenReturn(false);
        when(mockQuery.isListGroup()).thenReturn(false);
        when(mockQuery.getLimit()).thenReturn(Integer.MAX_VALUE);
        when(mockQuery.getOffset()).thenReturn(0);

        mappings = Mappings.create(mockQuery);
        mockMapping = mock(Mapping.class);
        mockNode = mock(Node.class);
    }

    @Nested
    @DisplayName("Mappings Creation Tests")
    class MappingsCreationTests {

        @Test
        @DisplayName("Should create empty mappings")
        void testCreateEmptyMappings() {
            Mappings m = new Mappings();
            assertNotNull(m, "Mappings should not be null");
            assertEquals(0, m.size(), "Empty mappings should have size 0");
        }

        @Test
        @DisplayName("Should create mappings with query")
        void testCreateMappingsWithQuery() {
            Mappings m = Mappings.create(mockQuery);
            assertNotNull(m, "Mappings should not be null");
            assertEquals(mockQuery, m.getQuery(), "Query should match");
        }

        @Test
        @DisplayName("Should create mappings with subEval flag")
        void testCreateMappingsWithSubEval() {
            Mappings m = Mappings.create(mockQuery, true);
            assertNotNull(m, "Mappings with subEval should not be null");
        }
    }

    @Nested
    @DisplayName("Add and Remove Tests")
    class AddRemoveTests {

        @Test
        @DisplayName("Should add mapping")
        void testAddMapping() {
            mappings.add(mockMapping);
            assertEquals(1, mappings.size(), "Should have one mapping");
        }

        @Test
        @DisplayName("Should add multiple mappings")
        void testAddMultipleMappings() {
            mappings.add(mockMapping);
            mappings.add(mock(Mapping.class));
            mappings.add(mock(Mapping.class));
            assertEquals(3, mappings.size(), "Should have 3 mappings");
        }

        @Test
        @DisplayName("Should add mappings collection")
        void testAddMappingsCollection() {
            Mappings other = Mappings.create(mockQuery);
            other.add(mockMapping);
            other.add(mock(Mapping.class));

            mappings.add(other);
            assertEquals(2, mappings.size(), "Should have 2 mappings");
        }

        @Test
        @DisplayName("Should get mapping at index")
        void testGetMappingAtIndex() {
            mappings.add(mockMapping);
            Mapping result = mappings.get(0);
            assertEquals(mockMapping, result, "Should return first mapping");
        }

        @Test
        @DisplayName("Should set mapping at index")
        void testSetMappingAtIndex() {
            mappings.add(mockMapping);
            Mapping newMapping = mock(Mapping.class);
            mappings.set(0, newMapping);
            assertEquals(newMapping, mappings.get(0), "Mapping should be updated");
        }

        @Test
        @DisplayName("Should clear all mappings")
        void testClear() {
            mappings.add(mockMapping);
            mappings.add(mock(Mapping.class));

            mappings.clear();
            assertEquals(0, mappings.size(), "Should be empty after clear");
            assertTrue(mappings.isEmpty(), "isEmpty should return true");
        }

        @Test
        @DisplayName("Should handle null mapping in submit")
        void testSubmitNull() {
            mappings.submit(null);
            assertEquals(0, mappings.size(), "Should not add null mapping");
        }
    }

    @Nested
    @DisplayName("Size and Empty Tests")
    class SizeEmptyTests {

        @Test
        @DisplayName("Should return size 0 for empty mappings")
        void testEmptySize() {
            assertEquals(0, mappings.size(), "Empty mappings should have size 0");
            assertTrue(mappings.isEmpty(), "isEmpty should return true");
        }

        @Test
        @DisplayName("Should return correct size after adding")
        void testSizeAfterAdding() {
            mappings.add(mockMapping);
            mappings.add(mock(Mapping.class));

            assertEquals(2, mappings.size(), "Size should be 2");
            assertFalse(mappings.isEmpty(), "isEmpty should return false");
        }
    }

    @Nested
    @DisplayName("Iterator Tests")
    class IteratorTests {

        @Test
        @DisplayName("Should provide iterator")
        void testIterator() {
            mappings.add(mockMapping);
            Iterator<Mapping> iterator = mappings.iterator();

            assertNotNull(iterator, "Iterator should not be null");
            assertTrue(iterator.hasNext(), "Iterator should have next");
        }

        @Test
        @DisplayName("Should iterate through all mappings")
        void testIterateAll() {
            mappings.add(mockMapping);
            mappings.add(mock(Mapping.class));
            mappings.add(mock(Mapping.class));

            int count = 0;
            for (Mapping ignored : mappings) {
                count++;
            }
            assertEquals(3, count, "Should iterate through 3 mappings");
        }

        @Test
        @DisplayName("Empty iterator should not have next")
        void testEmptyIterator() {
            Iterator<Mapping> iterator = mappings.iterator();
            assertFalse(iterator.hasNext(), "Empty iterator should not have next");
        }
    }

    @Nested
    @DisplayName("Query Management Tests")
    class QueryManagementTests {

        @Test
        @DisplayName("Should get query")
        void testGetQuery() {
            Query result = mappings.getQuery();
            assertEquals(mockQuery, result, "Should return query");
        }

        @Test
        @DisplayName("Should set query")
        void testSetQuery() {
            Query newQuery = mock(Query.class);
            mappings.setQuery(newQuery);
            assertEquals(newQuery, mappings.getQuery(), "Query should be updated");
        }
    }

    @Nested
    @DisplayName("Select Node Tests")
    class SelectNodeTests {

        @Test
        @DisplayName("Should get select nodes")
        void testGetSelect() {
            List<Node> select = mappings.getSelect();
            assertNotNull(select, "Select should not be null");
        }


        @Test
        @DisplayName("Should set single select node")
        void testSetSingleNode() {
            mappings.setSelect(mockNode);

            List<Node> select = mappings.getSelect();
            assertNotNull(select, "Select should not be null");
            assertEquals(1, select.size(), "Should have one node");
        }
    }

    @Nested
    @DisplayName("Distinct Tests")
    class DistinctTests {

        @Test
        @DisplayName("Should check if distinct")
        void testIsDistinct() {
            assertFalse(mappings.isDistinct(), "Should not be distinct by default");
        }

        @Test
        @DisplayName("Should create distinct mappings")
        void testDistinct() {
            when(mockQuery.getSelectFun()).thenReturn(new ArrayList<>());
            mappings.add(mockMapping);

            Mappings distinct = mappings.distinct();
            assertNotNull(distinct, "Distinct mappings should not be null");
        }
    }

    @Nested
    @DisplayName("Node Value Tests")
    class NodeValueTests {

        @Test
        @DisplayName("Should get node value")
        void testGetNode() {
            when(mockNode.getLabel()).thenReturn("?x");
            when(mockMapping.getNode("?x")).thenReturn(mockNode);
            mappings.add(mockMapping);

            Node result = mappings.getNode("?x");
            assertEquals(mockNode, result, "Should return node");
        }

        @Test
        @DisplayName("Should return null for empty mappings")
        void testGetNodeEmpty() {
            Node result = mappings.getNode("?x");
            assertNull(result, "Should return null for empty mappings");
        }

        @Test
        @DisplayName("Should get value by name")
        void testGetValue() {
            IDatatype mockValue = mock(IDatatype.class);
            when(mockNode.getDatatypeValue()).thenReturn(mockValue);
            when(mockMapping.getNode("?x")).thenReturn(mockNode);
            mappings.add(mockMapping);

            IDatatype result = mappings.getValue("?x");
            assertEquals(mockValue, result, "Should return value");
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("toString should not be null")
        void testToStringNotNull() {
            String str = mappings.toString();
            assertNotNull(str, "toString should not return null");
        }

        @Test
        @DisplayName("Empty mappings toString should be empty")
        void testToStringEmpty() {
            String str = mappings.toString();
            assertTrue(str.isBlank(),
                    "Empty mappings should produce empty string");
        }

    }

    @Nested
    @DisplayName("Union Tests")
    class UnionTests {

        @Test
        @DisplayName("Should perform union")
        void testUnion() {
            Mappings other = Mappings.create(mockQuery);
            other.add(mock(Mapping.class));

            mappings.add(mockMapping);
            Mappings result = mappings.union(other);

            assertNotNull(result, "Union result should not be null");
            assertEquals(2, result.size(), "Union should have 2 mappings");
        }

        @Test
        @DisplayName("Union with empty mappings")
        void testUnionEmpty() {
            Mappings other = Mappings.create(mockQuery);
            Mappings result = mappings.union(other);

            assertNotNull(result, "Union with empty should not be null");
            assertEquals(0, result.size(), "Union should be empty");
        }
    }

    @Nested
    @DisplayName("Join Tests")
    class JoinTests {

        @Test
        @DisplayName("Should perform join")
        void testJoin() {
            Mapping m1 = mock(Mapping.class);
            Mapping m2 = mock(Mapping.class);
            Mapping joined = mock(Mapping.class);

            when(m1.join(m2)).thenReturn(joined);

            Mappings other = Mappings.create(mockQuery);
            mappings.add(m1);
            other.add(m2);

            Mappings result = mappings.join(other);
            assertNotNull(result, "Join result should not be null");
        }

        @Test
        @DisplayName("Should perform and operation")
        void testAnd() {
            Mappings other = Mappings.create(mockQuery);
            Mappings result = mappings.and(other);
            assertNotNull(result, "And result should not be null");
        }

        @Test
        @DisplayName("Should join variable with value")
        void testJoinNodeValue() {
            when(mockNode.getLabel()).thenReturn("?x");
            Node value = mock(Node.class);
            when(mockMapping.getNodeValue(mockNode)).thenReturn(null);

            mappings.add(mockMapping);
            mappings.join(mockNode, value);

            verify(mockMapping).addNode(mockNode, value);
        }
    }

    @Nested
    @DisplayName("Minus Tests")
    class MinusTests {

        @Test
        @DisplayName("Should perform minus operation")
        void testMinus() {
            Mapping m1 = mock(Mapping.class);
            Mapping m2 = mock(Mapping.class);

            when(m1.compatible(m2)).thenReturn(false);

            Mappings other = Mappings.create(mockQuery);
            mappings.add(m1);
            other.add(m2);

            Mappings result = mappings.minus(other);
            assertNotNull(result, "Minus result should not be null");
            assertEquals(1, result.size(), "Should keep incompatible mapping");
        }

        @Test
        @DisplayName("Minus should remove compatible mappings")
        void testMinusRemoveCompatible() {
            Mapping m1 = mock(Mapping.class);
            Mapping m2 = mock(Mapping.class);

            when(m1.compatible(m2)).thenReturn(true);

            Mappings other = Mappings.create(mockQuery);
            mappings.add(m1);
            other.add(m2);

            Mappings result = mappings.minus(other);
            assertEquals(0, result.size(), "Should remove compatible mapping");
        }
    }

    @Nested
    @DisplayName("Optional Tests")
    class OptionalTests {

        @Test
        @DisplayName("Should perform optional operation")
        void testOptional() {
            Mapping m1 = mock(Mapping.class);
            Mapping m2 = mock(Mapping.class);
            Mapping joined = mock(Mapping.class);

            when(m1.join(m2)).thenReturn(joined);

            Mappings other = Mappings.create(mockQuery);
            mappings.add(m1);
            other.add(m2);

            Mappings result = mappings.optional(other);
            assertNotNull(result, "Optional result should not be null");
        }

        @Test
        @DisplayName("Optional should keep left mapping if no join")
        void testOptionalNoJoin() {
            Mapping m1 = mock(Mapping.class);
            Mapping m2 = mock(Mapping.class);

            when(m1.join(m2)).thenReturn(null);

            Mappings other = Mappings.create(mockQuery);
            mappings.add(m1);
            other.add(m2);

            Mappings result = mappings.optional(other);
            assertEquals(1, result.size(), "Should keep left mapping");
        }
    }

    @Nested
    @DisplayName("Limit Tests")
    class LimitTests {

        @Test
        @DisplayName("Should apply limit")
        void testLimit() {
            mappings.add(mockMapping);
            mappings.add(mock(Mapping.class));
            mappings.add(mock(Mapping.class));

            mappings.limit(2);
            assertEquals(2, mappings.size(), "Should limit to 2 mappings");
        }

        @Test
        @DisplayName("Limit larger than size should not change")
        void testLimitLarger() {
            mappings.add(mockMapping);
            mappings.limit(10);
            assertEquals(1, mappings.size(), "Size should remain 1");
        }

        @Test
        @DisplayName("Limit 0 should clear all")
        void testLimitZero() {
            mappings.add(mockMapping);
            mappings.limit(0);
            assertEquals(0, mappings.size(), "Should be empty");
        }
    }

    @Nested
    @DisplayName("Project Tests")
    class ProjectTests {

        @Test
        @DisplayName("Should project mappings")
        void testProject() {
            when(mockQuery.getSelect()).thenReturn(new ArrayList<>());
            mappings.add(mockMapping);

            Mappings result = mappings.project();
            assertNotNull(result, "Project result should not be null");
        }

        @Test
        @DisplayName("Should project on single node")
        void testProjectNode() {
            Mapping projected = mock(Mapping.class);
            when(mockMapping.project(mockNode)).thenReturn(projected);

            mappings.add(mockMapping);
            Mappings result = mappings.project(mockNode);

            assertNotNull(result, "Project result should not be null");
            assertEquals(1, result.size(), "Should have one projected mapping");
        }
    }

    @Nested
    @DisplayName("Aggregate Tests")
    class AggregateTests {

        @Test
        @DisplayName("Should aggregate node values")
        void testAggregate() {
            Node val1 = mock(Node.class);
            Node val2 = mock(Node.class);

            when(mockNode.getLabel()).thenReturn("?x");
            when(mockMapping.getNodeValue(mockNode)).thenReturn(val1);

            Mapping m2 = mock(Mapping.class);
            when(m2.getNodeValue(mockNode)).thenReturn(val2);

            mappings.add(mockMapping);
            mappings.add(m2);

            List<Node> result = mappings.aggregate(mockNode);
            assertNotNull(result, "Aggregate result should not be null");
        }
    }

    @Nested
    @DisplayName("Fake Mappings Tests")
    class FakeMappingsTests {

        @Test
        @DisplayName("Should set and check fake flag")
        void testSetFake() {
            assertFalse(mappings.isFake(), "Should not be fake by default");

            mappings.setFake(true);
            assertTrue(mappings.isFake(), "Should be fake after setting");
        }
    }


    @Nested
    @DisplayName("Insert/Delete Tests")
    class InsertDeleteTests {

        @Test
        @DisplayName("Should set and get insert edges")
        void testSetGetInsert() {
            List<Edge> insert = new ArrayList<>();
            mappings.setInsert(insert);
            assertEquals(insert, mappings.getInsert(), "Insert should match");
        }

        @Test
        @DisplayName("Should set and get delete edges")
        void testSetGetDelete() {
            List<Edge> delete = new ArrayList<>();
            mappings.setDelete(delete);
            assertEquals(delete, mappings.getDelete(), "Delete should match");
        }
    }

    @Nested
    @DisplayName("Link Tests")
    class LinkTests {

        @Test
        @DisplayName("Should add and get links")
        void testAddLink() {
            mappings.addLink("http://example.org");

            List<String> links = mappings.getLinkList();
            assertNotNull(links, "Links should not be null");
            assertEquals(1, links.size(), "Should have one link");
        }

        @Test
        @DisplayName("Should get first link")
        void testGetLink() {
            mappings.addLink("http://example.org/1");
            mappings.addLink("http://example.org/2");

            String link = mappings.getLink();
            assertEquals("http://example.org/1", link, "Should return first link");
        }

        @Test
        @DisplayName("Should return null when no links")
        void testGetLinkEmpty() {
            String link = mappings.getLink();
            assertNull(link, "Should return null when no links");
        }
    }

    @Nested
    @DisplayName("Error Tests")
    class ErrorTests {

        @Test
        @DisplayName("Should set and check error flag")
        void testSetError() {
            assertFalse(mappings.isError(), "Should not have error by default");

            mappings.setError(true);
            assertTrue(mappings.isError(), "Should have error after setting");
        }
    }

    @Nested
    @DisplayName("Provenance Tests")
    class ProvenanceTests {

        @Test
        @DisplayName("Should set and get provenance")
        void testSetGetProvenance() {
            Object prov = new Object();
            mappings.setProvenance(prov);
            assertEquals(prov, mappings.getProvenance(), "Provenance should match");
        }
    }

    @Nested
    @DisplayName("Complete Tests")
    class CompleteTests {

        @Test
        @DisplayName("Should complete with eval")
        void testComplete() {
            Eval mockEval = mock(Eval.class);
            mappings.add(mockMapping);

            assertDoesNotThrow(() -> mappings.complete(mockEval),
                    "Complete should not throw exception");
        }
    }

    @Nested
    @DisplayName("Min/Max Tests")
    class MinMaxTests {

        @Test
        @DisplayName("Should find min node")
        void testMin() {
            Node val1 = mock(Node.class);
            Node val2 = mock(Node.class);

            when(val1.compare(val2)).thenReturn(-1);
            when(mockMapping.getNode(mockNode)).thenReturn(val1);

            Mapping m2 = mock(Mapping.class);
            when(m2.getNode(mockNode)).thenReturn(val2);

            mappings.add(mockMapping);
            mappings.add(m2);

            Node result = mappings.min(mockNode);
            assertEquals(val1, result, "Should return minimum node");
        }

        @Test
        @DisplayName("Should find max node")
        void testMax() {
            Node val1 = mock(Node.class);
            Node val2 = mock(Node.class);

            when(val1.compare(val2)).thenReturn(1);
            when(mockMapping.getNode(mockNode)).thenReturn(val1);

            Mapping m2 = mock(Mapping.class);
            when(m2.getNode(mockNode)).thenReturn(val2);

            mappings.add(mockMapping);
            mappings.add(m2);

            Node result = mappings.max(mockNode);
            assertEquals(val1, result, "Should return maximum node");
        }

        @Test
        @DisplayName("Min on empty should return null")
        void testMinEmpty() {
            Node result = mappings.min(mockNode);
            assertNull(result, "Min on empty should return null");
        }
    }


    @Nested
    @DisplayName("Display Tests")
    class DisplayTests {

        @Test
        @DisplayName("Should set and get display limit")
        void testSetGetDisplay() {
            mappings.setDisplay(100);
            assertEquals(100, mappings.getDisplay(), "Display should be 100");
        }
    }

    @Nested
    @DisplayName("Length Tests")
    class LengthTests {

        @Test
        @DisplayName("Should set and get length")
        void testSetGetLength() {
            mappings.setLength(500);
            assertEquals(500, mappings.getLength(), "Length should be 500");
        }
    }

    @Nested
    @DisplayName("Datatype Label Tests")
    class DatatypeLabelTests {

        @Test
        @DisplayName("Should return datatype label with size")
        void testGetDatatypeLabel() {
            mappings.add(mockMapping);
            String label = mappings.getDatatypeLabel();

            assertNotNull(label, "Label should not be null");
            assertTrue(label.contains("Mappings"), "Label should contain 'Mappings'");
            assertTrue(label.contains("size=1"), "Label should contain size");
        }
    }


    @Nested
    @DisplayName("Comparator Tests")
    class ComparatorTests {

        @Test
        @DisplayName("Should compare mappings")
        void testCompare() {
            Mapping m1 = mock(Mapping.class);
            Mapping m2 = mock(Mapping.class);

            when(m1.getOrderBy()).thenReturn(new Node[0]);
            when(m2.getOrderBy()).thenReturn(new Node[0]);

            int result = mappings.compare(m1, m2);
            assertEquals(0, result, "Empty mappings should be equal");
        }
    }

    @Nested
    @DisplayName("Loop Interface Tests")
    class LoopInterfaceTests {

        @Test
        @DisplayName("Should return iterable for loop")
        void testGetLoop() {
            Iterable<?> loop = mappings.getLoop();
            assertNotNull(loop, "Loop should not be null");
            assertEquals(mappings, loop, "Loop should return self");
        }
    }
}