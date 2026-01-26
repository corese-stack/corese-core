package fr.inria.corese.core.next.kgram.core;

import fr.inria.corese.core.next.kgram.api.core.ExpType;
import fr.inria.corese.core.next.kgram.api.core.Filter;
import fr.inria.corese.core.next.kgram.api.core.Node;
import fr.inria.corese.core.sparql.triple.parser.ASTExtension;
import fr.inria.corese.core.sparql.triple.parser.ASTQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Comprehensive unit tests for Query class.
 * Tests KGRAM query structure including select, from, where, order by,
 * group by, having, filters, and various query types.
 *
 */
@DisplayName("Query Tests")
class QueryTest {

    private Query query;
    private Node mockNode;
    private Filter mockFilter;

    @BeforeEach
    void setUp() {
        mockNode = mock(Node.class);
        mockFilter = mock(Filter.class);
        query = new Query();
    }

    @Nested
    @DisplayName("Query Creation Tests")
    class QueryCreationTests {

        @Test
        @DisplayName("Should create empty query")
        void testCreateEmptyQuery() {
            Query q = new Query();
            assertNotNull(q, "Query should not be null");
            assertEquals(ExpType.Type.QUERY, q.type(), "Type should be QUERY");
        }

        @Test
        @DisplayName("Should create query using factory method")
        void testCreateQueryFactory() {
            Exp exp = Exp.create(ExpType.Type.AND);
            Query q = Query.create(exp);
            assertNotNull(q, "Query should not be null");
        }

        @Test
        @DisplayName("Should create query with type")
        void testCreateQueryWithType() {
            Query q = Query.create(0);
            assertNotNull(q, "Query should not be null");
        }
    }

    @Nested
    @DisplayName("Limit and Offset Tests")
    class LimitOffsetTests {

        @Test
        @DisplayName("Should set and get limit")
        void testSetAndGetLimit() {
            query.setLimit(100);
            assertEquals(100, query.getLimit(), "Limit should be 100");
        }

        @Test
        @DisplayName("Should have default limit Integer.MAX_VALUE")
        void testDefaultLimit() {
            assertEquals(Integer.MAX_VALUE, query.getLimit(),
                    "Default limit should be Integer.MAX_VALUE");
        }

        @Test
        @DisplayName("Should set and get offset")
        void testSetAndGetOffset() {
            query.setOffset(50);
            assertEquals(50, query.getOffset(), "Offset should be 50");
        }

        @Test
        @DisplayName("Should have default offset 0")
        void testDefaultOffset() {
            assertEquals(0, query.getOffset(), "Default offset should be 0");
        }

        @Test
        @DisplayName("Should calculate limit with offset")
        void testGetLimitOffset() {
            query.setLimit(100);
            query.setOffset(50);
            int limitOffset = query.getLimitOffset();
            assertTrue(limitOffset >= 100, "Limit offset should be at least 100");
        }

        @Test
        @DisplayName("Should set and get slice")
        void testSetAndGetSlice() {
            query.setSlice(30);
            assertEquals(30, query.getSlice(), "Slice should be 30");
        }
    }

    @Nested
    @DisplayName("Distinct Tests")
    class DistinctTests {

        @Test
        @DisplayName("Should set and check distinct")
        void testSetAndIsDistinct() {
            query.setDistinct(true);
            assertTrue(query.isDistinct(), "Query should be distinct");
        }

        @Test
        @DisplayName("Should not be distinct by default")
        void testDefaultDistinct() {
            assertFalse(query.isDistinct(), "Query should not be distinct by default");
        }
    }

    @Nested
    @DisplayName("Select Tests")
    class SelectTests {

        @Test
        @DisplayName("Should add select node")
        void testAddSelect() {
            query.addSelect(mockNode);
            List<Exp> selectFun = query.getSelectFun();
            assertEquals(1, selectFun.size(), "Should have one select expression");
        }

        @Test
        @DisplayName("Should get select function list")
        void testGetSelectFun() {
            List<Exp> selectFun = query.getSelectFun();
            assertNotNull(selectFun, "Select function list should not be null");
        }

        @Test
        @DisplayName("Should set select function list")
        void testSetSelectFun() {
            List<Exp> list = new ArrayList<>();
            list.add(Exp.create(ExpType.Type.NODE, mockNode));
            query.setSelectFun(list);
            assertEquals(1, query.getSelectFun().size(),
                    "Select list should have one element");
        }

        @Test
        @DisplayName("Should get select node list")
        void testGetSelect() {
            List<Node> select = query.getSelect();
            assertNotNull(select, "Select list should not be null");
        }

        @Test
        @DisplayName("Should set select node list")
        void testSetSelect() {
            List<Node> list = new ArrayList<>();
            list.add(mockNode);
            query.setSelect(list);
            assertEquals(list, query.getSelect(), "Select list should match");
        }

        @Test
        @DisplayName("Should check if is select expression")
        void testIsSelectExpression() {
            boolean result = query.isSelectExpression();
            assertFalse(result, "Should not be select expression by default");
        }

        @Test
        @DisplayName("Should check if is select query")
        void testIsSelect() {
            boolean result = query.isSelect();
            assertTrue(result, "Should be select query by default");
        }
    }

    @Nested
    @DisplayName("From and Named Tests")
    class FromNamedTests {

        @Test
        @DisplayName("Should get from list")
        void testGetFrom() {
            List<Node> from = query.getFrom();
            assertNotNull(from, "From list should not be null");
        }

        @Test
        @DisplayName("Should set from list")
        void testSetFrom() {
            List<Node> list = new ArrayList<>();
            list.add(mockNode);
            query.setFrom(list);
            assertEquals(list, query.getFrom(), "From list should match");
        }

        @Test
        @DisplayName("Should get named list")
        void testGetNamed() {
            List<Node> named = query.getNamed();
            assertNotNull(named, "Named list should not be null");
        }

        @Test
        @DisplayName("Should set named list")
        void testSetNamed() {
            List<Node> list = new ArrayList<>();
            list.add(mockNode);
            query.setNamed(list);
            assertEquals(list, query.getNamed(), "Named list should match");
        }
    }

    @Nested
    @DisplayName("Order By Tests")
    class OrderByTests {

        @Test
        @DisplayName("Should add order by node")
        void testAddOrderBy() {
            query.addOrderBy(mockNode);
            List<Exp> orderBy = query.getOrderBy();
            assertEquals(1, orderBy.size(), "Should have one order by expression");
        }

        @Test
        @DisplayName("Should get order by list")
        void testGetOrderBy() {
            List<Exp> orderBy = query.getOrderBy();
            assertNotNull(orderBy, "Order by list should not be null");
        }

        @Test
        @DisplayName("Should set order by list")
        void testSetOrderBy() {
            List<Exp> list = new ArrayList<>();
            list.add(Exp.create(ExpType.Type.NODE, mockNode));
            query.setOrderBy(list);
            assertEquals(1, query.getOrderBy().size(),
                    "Order by list should have one element");
        }

        @Test
        @DisplayName("Should check if has order by")
        void testIsOrderBy() {
            assertFalse(query.isOrderBy(), "Should not have order by initially");
            query.addOrderBy(mockNode);
            assertTrue(query.isOrderBy(), "Should have order by after adding");
        }

        @Test
        @DisplayName("Should chain order by")
        void testOrderByChaining() {
            Query result = query.orderBy(mockNode);
            assertSame(query, result, "Should return same query for chaining");
        }
    }

    @Nested
    @DisplayName("Group By Tests")
    class GroupByTests {

        @Test
        @DisplayName("Should add group by node")
        void testAddGroupBy() {
            query.addGroupBy(mockNode);
            List<Exp> groupBy = query.getGroupBy();
            assertEquals(1, groupBy.size(), "Should have one group by expression");
        }

        @Test
        @DisplayName("Should get group by list")
        void testGetGroupBy() {
            List<Exp> groupBy = query.getGroupBy();
            assertNotNull(groupBy, "Group by list should not be null");
        }

        @Test
        @DisplayName("Should set group by list")
        void testSetGroupBy() {
            List<Exp> list = new ArrayList<>();
            list.add(Exp.create(ExpType.Type.NODE, mockNode));
            query.setGroupBy(list);
            assertEquals(1, query.getGroupBy().size(),
                    "Group by list should have one element");
        }

        @Test
        @DisplayName("Should check if has group by")
        void testIsGroupBy() {
            assertFalse(query.isGroupBy(), "Should not have group by initially");
            query.addGroupBy(mockNode);
            assertTrue(query.isGroupBy(), "Should have group by after adding");
        }

        @Test
        @DisplayName("Should check has group by or connect")
        void testHasGroupBy() {
            assertFalse(query.hasGroupBy(), "Should not have group by or connect");
            query.addGroupBy(mockNode);
            assertTrue(query.hasGroupBy(), "Should have group by");
        }

        @Test
        @DisplayName("Should chain group by")
        void testGroupByChaining() {
            Query result = query.groupBy(mockNode);
            assertSame(query, result, "Should return same query for chaining");
        }
    }

    @Nested
    @DisplayName("Having Tests")
    class HavingTests {

        @Test
        @DisplayName("Should set and get having")
        void testSetAndGetHaving() {
            Exp havingExp = Exp.create(ExpType.Type.FILTER, mockFilter);
            query.setHaving(havingExp);
            assertEquals(havingExp, query.getHaving(), "Having should match");
        }
    }

    @Nested
    @DisplayName("Construct Tests")
    class ConstructTests {

        @Test
        @DisplayName("Should set and check if is construct")
        void testSetAndIsConstruct() {
            query.setConstruct(true);
            assertTrue(query.isConstruct(), "Query should be construct");
        }

        @Test
        @DisplayName("Should set and get construct expression")
        void testSetAndGetConstruct() {
            Exp constructExp = Exp.create(ExpType.Type.AND);
            query.setConstruct(constructExp);
            assertEquals(constructExp, query.getConstruct(), "Construct should match");
        }

        @Test
        @DisplayName("Should get insert expression")
        void testGetInsert() {
            Exp constructExp = Exp.create(ExpType.Type.AND);
            query.setConstruct(constructExp);
            assertEquals(constructExp, query.getInsert(),
                    "Insert should return construct");
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should set and check if is delete")
        void testSetAndIsDelete() {
            query.setDelete(true);
            assertTrue(query.isDelete(), "Query should be delete");
        }

        @Test
        @DisplayName("Should set and get delete expression")
        void testSetAndGetDelete() {
            Exp deleteExp = Exp.create(ExpType.Type.AND);
            query.setDelete(deleteExp);
            assertEquals(deleteExp, query.getDelete(), "Delete should match");
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should set and check if is update")
        void testSetAndIsUpdate() {
            query.setUpdate(true);
            assertTrue(query.isUpdate(), "Query should be update");
        }

        @Test
        @DisplayName("Should set and check if is insert")
        void testSetAndIsInsert() {
            query.setInsert(true);
            assertTrue(query.isInsert(), "Query should be insert");
        }
    }

    @Nested
    @DisplayName("Aggregate Tests")
    class AggregateTests {

        @Test
        @DisplayName("Should set and check if is aggregate")
        void testSetAndIsAggregate() {
            query.setAggregate(true);
            assertTrue(query.isAggregate(), "Query should be aggregate");
        }

        @Test
        @DisplayName("Should set aggregate from select functions")
        void testSetAggregate() {
            assertDoesNotThrow(() -> query.setAggregate(),
                    "Setting aggregate should not throw");
        }
    }

    @Nested
    @DisplayName("Body Tests")
    class BodyTests {

        @Test
        @DisplayName("Should get body")
        void testGetBody() {
            assertDoesNotThrow(() -> query.getBody(),
                    "Getting body should not throw");
        }

        @Test
        @DisplayName("Should set body")
        void testSetBody() {
            Exp exp = Exp.create(ExpType.Type.AND);
            query.setBody(exp);
            assertEquals(exp, query.getBody(), "Body should match");
        }
    }

    @Nested
    @DisplayName("Node Management Tests")
    class NodeManagementTests {

        @Test
        @DisplayName("Should get pattern nodes")
        void testGetPatternNodes() {
            List<Node> nodes = query.getPatternNodes();
            assertNotNull(nodes, "Pattern nodes should not be null");
        }

        @Test
        @DisplayName("Should get query nodes")
        void testGetQueryNodes() {
            List<Node> nodes = query.getQueryNodes();
            assertNotNull(nodes, "Query nodes should not be null");
        }

        @Test
        @DisplayName("Should get pattern select nodes")
        void testGetPatternSelectNodes() {
            List<Node> nodes = query.getPatternSelectNodes();
            assertNotNull(nodes, "Pattern select nodes should not be null");
        }

        @Test
        @DisplayName("Should get binding nodes")
        void testGetBindingNodes() {
            List<Node> nodes = query.getBindingNodes();
            assertNotNull(nodes, "Binding nodes should not be null");
        }

        @Test
        @DisplayName("Should count nodes")
        void testNbNodes() {
            int count = query.nbNodes();
            assertTrue(count >= 0, "Node count should be non-negative");
        }

        @Test
        @DisplayName("Should count edges")
        void testNbEdges() {
            int count = query.nbEdges();
            assertTrue(count >= 0, "Edge count should be non-negative");
        }
    }

    @Nested
    @DisplayName("Graph Node Tests")
    class GraphNodeTests {

        @Test
        @DisplayName("Should set and get graph node")
        void testSetAndGetGraphNode() {
            query.setGraphNode(mockNode);
            assertEquals(mockNode, query.getGraphNode(), "Graph node should match");
        }

        @Test
        @DisplayName("Should get path node")
        void testGetPathNode() {
            assertDoesNotThrow(() -> query.getPathNode(),
                    "Getting path node should not throw");
        }
    }

    @Nested
    @DisplayName("Query Flags Tests")
    class QueryFlagsTests {

        @Test
        @DisplayName("Should set and check relax flag")
        void testSetAndIsRelax() {
            query.setRelax(true);
            assertTrue(query.isRelax(), "Query should be relax");
        }

        @Test
        @DisplayName("Should set and check optimize flag")
        void testSetAndIsOptimize() {
            query.setOptimize(true);
            assertTrue(query.isOptimize(), "Query should be optimize");
        }

        @Test
        @DisplayName("Should set and check test flag")
        void testSetAndIsTest() {
            query.setTest(true);
            assertTrue(query.isTest(), "Query should be test");
        }

        @Test
        @DisplayName("Should set and check new flag")
        void testSetAndIsNew() {
            query.setNew(false);
            assertFalse(query.isNew(), "Query should not be new");
        }

        @Test
        @DisplayName("Should set and check correct flag")
        void testSetAndIsCorrect() {
            query.setCorrect(false);
            assertFalse(query.isCorrect(), "Query should not be correct");
        }

        @Test
        @DisplayName("Should check if is connect")
        void testIsConnect() {
            boolean result = query.isConnect();
            assertFalse(result, "Query should not be connect by default");
        }

        @Test
        @DisplayName("Should set and check map flag")
        void testSetAndIsMap() {
            query.setMap(false);
            assertFalse(query.isMap(), "Query should not be map");
        }

        @Test
        @DisplayName("Should set and check rule flag")
        void testSetAndIsRule() {
            query.setRule(true);
            assertTrue(query.isRule(), "Query should be rule");
        }

        @Test
        @DisplayName("Should check if is record edge")
        void testIsRecordEdge() {
            boolean result = query.isRecordEdge();
            assertFalse(result, "Should not record edge by default");
        }

        @Test
        @DisplayName("Should check if is detail")
        void testIsDetail() {
            boolean result = query.isDetail();
            assertTrue(result, "Should be detail by default");
        }

        @Test
        @DisplayName("Should set and check synchronized flag")
        void testSetAndIsSynchronized() {
            query.setSynchronized(true);
            assertTrue(query.isSynchronized(), "Query should be synchronized");
        }

        @Test
        @DisplayName("Should set and check algebra flag")
        void testSetAndIsAlgebra() {
            query.setAlgebra(true);
            assertTrue(query.isAlgebra(), "Query should be algebra");
        }
    }

    @Nested
    @DisplayName("Service Tests")
    class ServiceTests {

        @Test
        @DisplayName("Should set and check if is service")
        void testSetAndIsService() {
            query.setService(true);
            assertTrue(query.isService(), "Query should be service");
        }

        @Test
        @DisplayName("Should set and get service URL")
        void testSetAndGetService() {
            query.setService("http://example.org/sparql");
            assertEquals("http://example.org/sparql", query.getService(),
                    "Service URL should match");
        }
    }

    @Nested
    @DisplayName("Template Tests")
    class TemplateTests {

        @Test
        @DisplayName("Should set and check if is template")
        void testSetAndIsTemplate() {
            query.setTemplate(true);
            assertTrue(query.isTemplate(), "Query should be template");
        }

        @Test
        @DisplayName("Should check if is transformation template")
        void testIsTransformationTemplate() {
            boolean result = query.isTransformationTemplate();
            assertFalse(result, "Should not be transformation template by default");
        }
    }

    @Nested
    @DisplayName("AST Tests")
    class ASTTests {

        @Test
        @DisplayName("Should set and get AST")
        void testSetAndGetAST() {
            ASTQuery mockAST = mock(ASTQuery.class);
            query.setAST(mockAST);
            assertEquals(mockAST, query.getAST(), "AST should match");
        }

        @Test
        @DisplayName("Should get global AST")
        void testGetGlobalAST() {
            assertDoesNotThrow(() -> query.getGlobalAST(),
                    "Getting global AST should not throw");
        }
    }

    @Nested
    @DisplayName("Global Query Tests")
    class GlobalQueryTests {

        @Test
        @DisplayName("Should get global query")
        void testGetGlobalQuery() {
            Query global = query.getGlobalQuery();
            assertSame(query, global, "Should return self when not subquery");
        }

        @Test
        @DisplayName("Should set outer query")
        void testSetOuterQuery() {
            Query outer = new Query();
            query.setOuterQuery(outer);
            assertEquals(outer, query.getOuterQuery(), "Outer query should match");
        }

        @Test
        @DisplayName("Should get outer query")
        void testGetOuterQuery() {
            Query outer = query.getOuterQuery();
            assertSame(query, outer, "Should return self when no outer query");
        }
    }

    @Nested
    @DisplayName("Extension Tests")
    class ExtensionTests {

        @Test
        @DisplayName("Should set and check if is extension")
        void testSetAndIsExtension() {
            query.setExtension(true);
            assertTrue(query.isExtension(), "Query should be extension");
        }

        @Test
        @DisplayName("Should set and get extension")
        void testSetAndGetExtension() {
            ASTExtension mockExt = mock(ASTExtension.class);
            query.setExtension(mockExt);
            assertEquals(mockExt, query.getExtension(), "Extension should match");
        }

        @Test
        @DisplayName("Should get actual extension")
        void testGetActualExtension() {
            assertDoesNotThrow(() -> query.getActualExtension(),
                    "Getting actual extension should not throw");
        }
    }

    @Nested
    @DisplayName("Parallel and Lock Tests")
    class ParallelLockTests {

        @Test
        @DisplayName("Should set and check parallel flag")
        void testSetAndIsParallel() {
            query.setParallel(false);
            assertFalse(query.isParallel(), "Query should not be parallel");
        }

        @Test
        @DisplayName("Should set and check lock flag")
        void testSetAndIsLock() {
            query.setLock(false);
            assertFalse(query.isLock(), "Query should not be lock");
        }
    }

    @Nested
    @DisplayName("Federate and Validate Tests")
    class FederateValidateTests {

        @Test
        @DisplayName("Should set and check federate flag")
        void testSetAndIsFederate() {
            query.setFederate(true);
            assertTrue(query.isFederate(), "Query should be federate");
        }

        @Test
        @DisplayName("Should set and check validate flag")
        void testSetAndIsValidate() {
            query.setValidate(true);
            assertTrue(query.isValidate(), "Query should be validate");
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("toString should not be null")
        void testToString() {
            String str = query.toString();
            assertNotNull(str, "toString should not return null");
        }

        @Test
        @DisplayName("Should get datatype label")
        void testGetDatatypeLabel() {
            String label = query.getDatatypeLabel();
            assertEquals("[Query]", label, "Datatype label should be [Query]");
        }
    }
}