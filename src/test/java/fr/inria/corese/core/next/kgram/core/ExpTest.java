package fr.inria.corese.core.next.kgram.core;

import fr.inria.corese.core.next.query.kgram.api.core.*;
import fr.inria.corese.core.next.query.kgram.api.query.Producer;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Mappings;
import fr.inria.corese.core.next.query.kgram.core.Stack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for Exp class.
 * Tests KGRAM/SPARQL expressions including BGP, union, optional, filters, etc.
 *
 * @author Test Suite
 */
@DisplayName("Exp Tests")
class ExpTest {

    private Exp exp;
    private ExpType.Type mockType;
    private Node mockNode;
    private Edge mockEdge;
    private Filter mockFilter;

    @BeforeEach
    void setUp() {
        mockType = ExpType.Type.AND;
        mockNode = mock(Node.class);
        mockEdge = mock(Edge.class);
        mockFilter = mock(Filter.class);

        exp = Exp.create(mockType);
    }

    @Nested
    @DisplayName("Exp Creation Tests")
    class ExpCreationTests {

        @Test
        @DisplayName("Should create exp with type")
        void testCreateWithType() {
            Exp e = Exp.create(ExpType.Type.AND);
            assertNotNull(e, "Exp should not be null");
            assertEquals(ExpType.Type.AND, e.type(), "Type should match");
        }

        @Test
        @DisplayName("Should create exp with two sub-expressions")
        void testCreateWithTwoExps() {
            Exp e1 = Exp.create(ExpType.Type.AND);
            Exp e2 = Exp.create(ExpType.Type.FILTER);
            Exp e = Exp.create(ExpType.Type.JOIN, e1, e2);

            assertNotNull(e, "Exp should not be null");
            assertEquals(2, e.size(), "Should have 2 sub-expressions");
        }

        @Test
        @DisplayName("Should create exp with three sub-expressions")
        void testCreateWithThreeExps() {
            Exp e1 = Exp.create(ExpType.Type.AND);
            Exp e2 = Exp.create(ExpType.Type.FILTER);
            Exp e3 = Exp.create(ExpType.Type.EDGE);
            Exp e = Exp.create(ExpType.Type.JOIN, e1, e2, e3);

            assertNotNull(e, "Exp should not be null");
            assertEquals(3, e.size(), "Should have 3 sub-expressions");
        }

        @Test
        @DisplayName("Should create exp with one sub-expression")
        void testCreateWithOneExp() {
            Exp e1 = Exp.create(ExpType.Type.AND);
            Exp e = Exp.create(ExpType.Type.OPTIONAL, e1);

            assertNotNull(e, "Exp should not be null");
            assertEquals(1, e.size(), "Should have 1 sub-expression");
        }

        @Test
        @DisplayName("Should create exp with node")
        void testCreateWithNode() {
            Exp e = Exp.create(ExpType.Type.NODE, mockNode);
            assertNotNull(e, "Exp should not be null");
            assertEquals(mockNode, e.getNode(), "Node should match");
        }

        @Test
        @DisplayName("Should create exp with edge")
        void testCreateWithEdge() {
            Exp e = Exp.create(ExpType.Type.EDGE, mockEdge);
            assertNotNull(e, "Exp should not be null");
            assertEquals(mockEdge, e.getEdge(), "Edge should match");
        }

        @Test
        @DisplayName("Should create exp with filter")
        void testCreateWithFilter() {
            Exp e = Exp.create(ExpType.Type.FILTER, mockFilter);
            assertNotNull(e, "Exp should not be null");
            assertEquals(mockFilter, e.getFilter(), "Filter should match");
        }

        @Test
        @DisplayName("Should create values expression")
        void testCreateValues() {
            List<Node> nodes = new ArrayList<>();
            nodes.add(mockNode);
            Mappings mockMappings = mock(Mappings.class);

            Exp e = Exp.createValues(nodes, mockMappings);
            assertNotNull(e, "Values exp should not be null");
            assertTrue(e.isValues(), "Should be values type");
        }
    }

    @Nested
    @DisplayName("Type Checking Tests")
    class TypeCheckingTests {

        @Test
        @DisplayName("Should check if is BGP")
        void testIsBGP() {
            Exp e = Exp.create(ExpType.Type.BGP);
            assertTrue(e.isBGP(), "Should be BGP");
        }

        @Test
        @DisplayName("Should check if is filter")
        void testIsFilter() {
            Exp e = Exp.create(ExpType.Type.FILTER);
            assertTrue(e.isFilter(), "Should be filter");
        }

        @Test
        @DisplayName("Should check if is optional")
        void testIsOptional() {
            Exp e = Exp.create(ExpType.Type.OPTIONAL);
            assertTrue(e.isOptional(), "Should be optional");
        }

        @Test
        @DisplayName("Should check if is union")
        void testIsUnion() {
            Exp e = Exp.create(ExpType.Type.UNION);
            assertTrue(e.isUnion(), "Should be union");
        }

        @Test
        @DisplayName("Should check if is minus")
        void testIsMinus() {
            Exp e = Exp.create(ExpType.Type.MINUS);
            assertTrue(e.isMinus(), "Should be minus");
        }

        @Test
        @DisplayName("Should check if is graph")
        void testIsGraph() {
            Exp e = Exp.create(ExpType.Type.GRAPH);
            assertTrue(e.isGraph(), "Should be graph");
        }

        @Test
        @DisplayName("Should check if is service")
        void testIsService() {
            Exp e = Exp.create(ExpType.Type.SERVICE);
            assertTrue(e.isService(), "Should be service");
        }

        @Test
        @DisplayName("Should check if is query")
        void testIsQuery() {
            Exp e = Exp.create(ExpType.Type.QUERY);
            assertTrue(e.isQuery(), "Should be query");
        }

        @Test
        @DisplayName("Should check if is join")
        void testIsJoin() {
            Exp e = Exp.create(ExpType.Type.JOIN);
            assertTrue(e.isJoin(), "Should be join");
        }

        @Test
        @DisplayName("Should check if is and")
        void testIsAnd() {
            Exp e = Exp.create(ExpType.Type.AND);
            assertTrue(e.isAnd(), "Should be and");
        }

        @Test
        @DisplayName("Should check if is node")
        void testIsNode() {
            Exp e = Exp.create(ExpType.Type.NODE);
            assertTrue(e.isNode(), "Should be node");
        }

        @Test
        @DisplayName("Should check if is edge")
        void testIsEdge() {
            Exp e = Exp.create(ExpType.Type.EDGE);
            assertTrue(e.isEdge(), "Should be edge");
        }

        @Test
        @DisplayName("Should check if is values")
        void testIsValues() {
            Exp e = Exp.create(ExpType.Type.VALUES);
            assertTrue(e.isValues(), "Should be values");
        }

        @Test
        @DisplayName("Should check if is bind")
        void testIsBind() {
            Exp e = Exp.create(ExpType.Type.BIND);
            assertTrue(e.isBind(), "Should be bind");
        }
    }

    @Nested
    @DisplayName("Node Management Tests")
    class NodeManagementTests {

        @Test
        @DisplayName("Should set and get node")
        void testSetAndGetNode() {
            exp.setNode(mockNode);
            assertEquals(mockNode, exp.getNode(), "Node should match");
        }

        @Test
        @DisplayName("Should set and get node list")
        void testSetAndGetNodeList() {
            List<Node> nodes = new ArrayList<>();
            nodes.add(mockNode);

            exp.setNodeList(nodes);
            assertEquals(nodes, exp.getNodeList(), "Node list should match");
        }

        @Test
        @DisplayName("Should check if has node list")
        void testHasNodeList() {
            List<Node> nodes = new ArrayList<>();
            nodes.add(mockNode);

            exp.setNodeList(nodes);
            assertTrue(exp.hasNodeList(), "Should have node list");
        }

        @Test
        @DisplayName("Should add node to list")
        void testAddNode() {
            exp.addNode(mockNode);
            assertNotNull(exp.getNodeList(), "Node list should not be null");
        }

        @Test
        @DisplayName("Should get graph node")
        void testGetGraphNode() {
            exp.setNode(mockNode);
            // Graph node can be null if not a graph type
            assertDoesNotThrow(() -> exp.getGraphNode(),
                    "Getting graph node should not throw");
        }
    }

    @Nested
    @DisplayName("Edge Management Tests")
    class EdgeManagementTests {

        @Test
        @DisplayName("Should set and get edge")
        void testSetAndGetEdge() {
            exp.setEdge(mockEdge);
            assertEquals(mockEdge, exp.getEdge(), "Edge should match");
        }

        @Test
        @DisplayName("Should add edge")
        void testAddEdge() {
            exp.add(mockEdge);
            assertDoesNotThrow(() -> exp.add(mockEdge),
                    "Adding edge should not throw");
        }
    }

    @Nested
    @DisplayName("Filter Management Tests")
    class FilterManagementTests {

        @Test
        @DisplayName("Should set and get filter")
        void testSetAndGetFilter() {
            exp.setFilter(mockFilter);
            assertEquals(mockFilter, exp.getFilter(), "Filter should match");
        }

        @Test
        @DisplayName("Should add filter")
        void testAddFilter() {
            exp.addFilter(mockFilter);
            List<Filter> filters = exp.getFilters();
            assertNotNull(filters, "Filters should not be null");
            assertTrue(filters.contains(mockFilter), "Should contain added filter");
        }

    }

    @Nested
    @DisplayName("Sub-Expression Management Tests")
    class SubExpressionTests {

        @Test
        @DisplayName("Should add sub-expression")
        void testAddExp() {
            Exp subExp = Exp.create(ExpType.Type.FILTER);
            exp.add(subExp);
            assertEquals(1, exp.size(), "Should have one sub-expression");
        }

        @Test
        @DisplayName("Should get sub-expression at index")
        void testGetAtIndex() {
            Exp subExp = Exp.create(ExpType.Type.FILTER);
            exp.add(subExp);

            Exp result = exp.get(0);
            assertEquals(subExp, result, "Sub-expression should match");
        }

        @Test
        @DisplayName("Should set sub-expression at index")
        void testSetAtIndex() {
            Exp subExp1 = Exp.create(ExpType.Type.FILTER);
            Exp subExp2 = Exp.create(ExpType.Type.EDGE);

            exp.add(subExp1);
            exp.set(0, subExp2);

            assertEquals(subExp2, exp.get(0), "Sub-expression should be updated");
        }

        @Test
        @DisplayName("Should get expression list")
        void testGetExpList() {
            Exp subExp = Exp.create(ExpType.Type.FILTER);
            exp.add(subExp);

            List<Exp> list = exp.getExpList();
            assertNotNull(list, "Expression list should not be null");
            assertTrue(list.contains(subExp), "List should contain added expression");
        }

        @Test
        @DisplayName("Should get size")
        void testSize() {
            assertEquals(0, exp.size(), "Initial size should be 0");

            exp.add(Exp.create(ExpType.Type.FILTER));
            exp.add(Exp.create(ExpType.Type.EDGE));

            assertEquals(2, exp.size(), "Size should be 2");
        }
    }

    @Nested
    @DisplayName("Index Management Tests")
    class IndexManagementTests {

        @Test
        @DisplayName("Should set and get index")
        void testSetAndGetIndex() {
            exp.setIndex(5);
            assertEquals(5, exp.getIndex(), "Index should be 5");
        }

        @Test
        @DisplayName("Should have default index -1")
        void testDefaultIndex() {
            Exp e = Exp.create(ExpType.Type.AND);
            assertEquals(-1, e.getIndex(), "Default index should be -1");
        }
    }

    @Nested
    @DisplayName("Status and Flag Tests")
    class StatusFlagTests {

        @Test
        @DisplayName("Should set and check fail flag")
        void testSetAndIsFail() {
            exp.setFail(true);
            assertTrue(exp.isFail(), "Fail should be true");
        }

        @Test
        @DisplayName("Should set and check aggregate flag")
        void testSetAndIsAggregate() {
            exp.setAggregate(true);
            assertTrue(exp.isAggregate(), "Aggregate should be true");
        }

        @Test
        @DisplayName("Should set and check silent flag")
        void testSetAndIsSilent() {
            exp.setSilent(true);
            assertTrue(exp.isSilent(), "Silent should be true");
        }

        @Test
        @DisplayName("Should set and check functional flag")
        void testSetAndIsFunctional() {
            exp.setFunctional(true);
            assertTrue(exp.isFunctional(), "Functional should be true");
        }

        @Test
        @DisplayName("Should set and check system flag")
        void testSetAndIsSystem() {
            exp.setSystem(true);
            assertTrue(exp.isSystem(), "System should be true");
        }

        @Test
        @DisplayName("Should set and check generated flag")
        void testSetAndIsGenerated() {
            exp.setGenerated(true);
            assertTrue(exp.isGenerated(), "Generated should be true");
        }

        @Test
        @DisplayName("Should set and check BGPAble flag")
        void testSetAndIsBGPAble() {
            exp.setBGPAble(true);
            assertTrue(exp.isBGPAble(), "BGPAble should be true");
        }
    }

    @Nested
    @DisplayName("Min/Max Tests")
    class MinMaxTests {

        @Test
        @DisplayName("Should set and get min")
        void testSetAndGetMin() {
            exp.setMin(5);
            assertEquals(5, exp.getMin(), "Min should be 5");
        }

        @Test
        @DisplayName("Should set and get max")
        void testSetAndGetMax() {
            exp.setMax(10);
            assertEquals(10, exp.getMax(), "Max should be 10");
        }

        @Test
        @DisplayName("Should have default min -1")
        void testDefaultMin() {
            Exp e = Exp.create(ExpType.Type.AND);
            assertEquals(-1, e.getMin(), "Default min should be -1");
        }

        @Test
        @DisplayName("Should have default max -1")
        void testDefaultMax() {
            Exp e = Exp.create(ExpType.Type.AND);
            assertEquals(-1, e.getMax(), "Default max should be -1");
        }
    }

    @Nested
    @DisplayName("Level Tests")
    class LevelTests {

        @Test
        @DisplayName("Should set and get level")
        void testSetAndGetLevel() {
            exp.setLevel(3);
            assertEquals(3, exp.getLevel(), "Level should be 3");
        }

        @Test
        @DisplayName("Should have default level -1")
        void testDefaultLevel() {
            Exp e = Exp.create(ExpType.Type.AND);
            assertEquals(-1, e.getLevel(), "Default level should be -1");
        }
    }

    @Nested
    @DisplayName("Number Tests")
    class NumberTests {

        @Test
        @DisplayName("Should set and get number")
        void testSetAndGetNumber() {
            exp.setNumber(42);
            assertEquals(42, exp.getNumber(), "Number should be 42");
        }

        @Test
        @DisplayName("Should set and get num")
        void testSetAndGetNum() {
            exp.setNum(7);
            assertEquals(7, exp.getNum(), "Num should be 7");
        }
    }

    @Nested
    @DisplayName("Object Management Tests")
    class ObjectManagementTests {

        @Test
        @DisplayName("Should set and get object")
        void testSetAndGetObject() {
            Object obj = new Object();
            exp.setObject(obj);
            assertEquals(obj, exp.getObject(), "Object should match");
        }
    }

    @Nested
    @DisplayName("Producer Tests")
    class ProducerTests {

        @Test
        @DisplayName("Should set and get producer")
        void testSetAndGetProducer() {
            Producer mockProducer = mock(Producer.class);
            exp.setProducer(mockProducer);
            assertEquals(mockProducer, exp.getProducer(), "Producer should match");
        }
    }

    @Nested
    @DisplayName("Regex Tests")
    class RegexTests {

        @Test
        @DisplayName("Should set and get regex")
        void testSetAndGetRegex() {
            Regex mockRegex = mock(Regex.class);
            exp.setRegex(mockRegex);
            assertEquals(mockRegex, exp.getRegex(), "Regex should match");
        }
    }

    @Nested
    @DisplayName("Mappings Tests")
    class MappingsTests {

        @Test
        @DisplayName("Should set and get mappings")
        void testSetAndGetMappings() {
            Mappings mockMappings = mock(Mappings.class);
            exp.setMappings(mockMappings);
            assertEquals(mockMappings, exp.getMappings(), "Mappings should match");
        }

        @Test
        @DisplayName("Should check if has mappings")
        void testIsMappings() {
            exp.setMappings(true);
            assertTrue(exp.isMappings(), "Should have mappings");
        }


    }

    @Nested
    @DisplayName("Stack Tests")
    class StackTests {

        @Test
        @DisplayName("Should set and get stack")
        void testSetAndGetStack() {
            Stack mockStack = mock(Stack.class);
            exp.setStack(mockStack);
            assertEquals(mockStack, exp.getStack(), "Stack should match");
        }
    }

    @Nested
    @DisplayName("Path Expression Tests")
    class PathExpressionTests {

        @Test
        @DisplayName("Should set and get path expression")
        void testSetAndGetPath() {
            Exp pathExp = Exp.create(ExpType.Type.PATH);
            exp.setPath(pathExp);
            assertEquals(pathExp, exp.getPath(), "Path expression should match");
        }
    }

    @Nested
    @DisplayName("Bind Expression Tests")
    class BindExpressionTests {

        @Test
        @DisplayName("Should set and get bind expression")
        void testSetAndGetBind() {
            Exp bindExp = Exp.create(ExpType.Type.BIND);
            exp.setBind(bindExp);
            assertEquals(bindExp, exp.getBind(), "Bind expression should match");
        }
    }

    @Nested
    @DisplayName("Values Expression Tests")
    class ValuesExpressionTests {

        @Test
        @DisplayName("Should set and get values expression")
        void testSetAndGetValues() {
            Exp valuesExp = Exp.create(ExpType.Type.VALUES);
            exp.setValues(valuesExp);
            assertEquals(valuesExp, exp.getValues(), "Values expression should match");
        }
    }

    @Nested
    @DisplayName("Postpone Tests")
    class PostponeTests {

        @Test
        @DisplayName("Should set and check postpone flag")
        void testSetAndIsPostpone() {
            exp.setPostpone(true);
            assertTrue(exp.isPostpone(), "Postpone should be true");
        }

        @Test
        @DisplayName("Should set and get postpone expression")
        void testSetAndGetPostpone() {
            Exp postponeExp = Exp.create(ExpType.Type.FILTER);
            exp.setPostpone(postponeExp);
            assertEquals(postponeExp, exp.getPostpone(), "Postpone exp should match");
        }
    }

    @Nested
    @DisplayName("InScope Filter Tests")
    class InScopeFilterTests {

        @Test
        @DisplayName("Should set and get inscope filters")
        void testSetAndGetInscopeFilter() {
            List<Exp> filters = new ArrayList<>();
            filters.add(Exp.create(ExpType.Type.FILTER));

            exp.setInscopeFilter(filters);
            assertEquals(filters, exp.getInscopeFilter(), "InScope filters should match");
        }

        @Test
        @DisplayName("Should get or create inscope filters")
        void testGetCreateInscopeFilter() {
            List<Exp> filters = exp.getCreateInscopeFilter();
            assertNotNull(filters, "InScope filters should not be null");
        }
    }

    @Nested
    @DisplayName("Type Management Tests")
    class TypeManagementTests {

        @Test
        @DisplayName("Should get type")
        void testGetType() {
            assertEquals(mockType, exp.type(), "Type should match");
        }

        @Test
        @DisplayName("Should set type")
        void testSetType() {
            exp.setType(ExpType.Type.UNION);
            assertEquals(ExpType.Type.UNION, exp.type(), "Type should be updated");
        }
    }

    @Nested
    @DisplayName("Iterator Tests")
    class IteratorTests {

        @Test
        @DisplayName("Should iterate through sub-expressions")
        void testIterator() {
            exp.add(Exp.create(ExpType.Type.FILTER));
            exp.add(Exp.create(ExpType.Type.EDGE));

            int count = 0;
            for (Exp ignored : exp) {
                count++;
            }
            assertEquals(2, count, "Should iterate through 2 expressions");
        }

        @Test
        @DisplayName("Empty exp should have no iterations")
        void testEmptyIterator() {
            int count = 0;
            for (Exp ignored : exp) {
                count++;
            }
            assertEquals(0, count, "Empty exp should not iterate");
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("toString should not be null")
        void testToStringNotNull() {
            String str = exp.toString();
            assertNotNull(str, "toString should not return null");
        }

        @Test
        @DisplayName("Should get datatype label")
        void testGetDatatypeLabel() {
            String label = exp.getDatatypeLabel();
            assertNotNull(label, "Datatype label should not be null");
        }
    }

    @Nested
    @DisplayName("Statement Tests")
    class StatementTests {

        @Test
        @DisplayName("Should check if is statement")
        void testIsStatement() {
            // Result depends on type
            assertDoesNotThrow(() -> exp.isStatement(),
                    "isStatement should not throw");
        }

        @Test
        @DisplayName("Should get statement")
        void testGetStatement() {
            assertDoesNotThrow(() -> exp.getStatement(),
                    "getStatement should not throw");
        }
    }

    @Nested
    @DisplayName("Cache Tests")
    class CacheTests {

        @Test
        @DisplayName("Should set and get cache node")
        void testSetAndGetCacheNode() {
            exp.setCacheNode(mockNode);
            assertEquals(mockNode, exp.getCacheNode(), "Cache node should match");
        }
    }

    @Nested
    @DisplayName("InScope Node List Tests")
    class InScopeNodeListTests {

        @Test
        @DisplayName("Should set and get inscope node list")
        void testSetAndGetInScopeNodeList() {
            List<Node> nodes = new ArrayList<>();
            nodes.add(mockNode);

            exp.setInScopeNodeList(nodes);
            assertEquals(nodes, exp.getInScopeNodeList(), "InScope node list should match");
        }
    }

    @Nested
    @DisplayName("Constants Tests")
    class ConstantsTests {

        @Test
        @DisplayName("Should have correct subject constant")
        void testSubjectConstant() {
            assertEquals(0, Exp.SUBJECT, "SUBJECT should be 0");
        }

        @Test
        @DisplayName("Should have correct object constant")
        void testObjectConstant() {
            assertEquals(1, Exp.OBJECT, "OBJECT should be 1");
        }

        @Test
        @DisplayName("Should have correct predicate constant")
        void testPredicateConstant() {
            assertEquals(2, Exp.PREDICATE, "PREDICATE should be 2");
        }
    }
}