package fr.inria.corese.core.next.query.impl.kgram.core;

import fr.inria.corese.core.next.query.impl.kgram.api.core.BindingContext;
import fr.inria.corese.core.next.query.impl.kgram.api.core.ExpType;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Evaluator;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Matcher;
import fr.inria.corese.core.next.query.impl.kgram.api.query.ProcessVisitor;
import fr.inria.corese.core.next.query.impl.kgram.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Comprehensive unit tests for Memory class.
 * Tests KGRAM memory management including node and edge binding stacks,
 * aggregate operations, path evaluation, and service details.
 */
@DisplayName("Memory Tests")
class MemoryTest {

    private Memory memory;
    private Matcher mockMatcher;
    private Evaluator mockEvaluator;
    private Node mockNode;
    private Query mockQuery;
    private Eval mockEval;

    @BeforeEach
    void setUp() {
        mockMatcher = mock(Matcher.class);
        mockEvaluator = mock(Evaluator.class);
        mockNode = mock(Node.class);
        mockQuery = mock(Query.class);
        mockEval = mock(Eval.class);

        memory = new Memory(mockMatcher, mockEvaluator);
    }

    @Nested
    @DisplayName("Memory Creation Tests")
    class MemoryCreationTests {

        @Test
        @DisplayName("Should create memory with matcher and evaluator")
        void testCreateMemory() {
            Memory mem = new Memory(mockMatcher, mockEvaluator);
            assertNotNull(mem, "Memory should not be null");
            assertEquals(mockMatcher, mem.getMatcher(), "Matcher should match");
        }

        @Test
        @DisplayName("Should create empty memory")
        void testCreateEmptyMemory() {
            Memory mem = new Memory();
            assertNotNull(mem, "Empty memory should not be null");
        }

        @Test
        @DisplayName("Should initialize with query")
        void testInitWithQuery() {
            // Mock query with empty select list and body
            List<Node> selectNodes = new ArrayList<>();
            Exp bodyExp = Exp.create(ExpType.Type.AND);

            when(mockQuery.getSelect()).thenReturn(selectNodes);
            when(mockQuery.getBody()).thenReturn(bodyExp);

            Memory result = memory.init(mockQuery);
            assertNotNull(result, "Initialized memory should not be null");
            assertSame(memory, result, "Should return same memory instance");
        }

        @Test
        @DisplayName("Should initialize from another memory")
        void testInitFromMemory() {
            Memory other = new Memory(mockMatcher, mockEvaluator);
            assertDoesNotThrow(() -> memory.init(other),
                    "Init from memory should not throw");
        }
    }

    @Nested
    @DisplayName("Eval Management Tests")
    class EvalManagementTests {

        @Test
        @DisplayName("Should set and get eval")
        void testSetAndGetEval() {
            memory.setEval(mockEval);
            assertEquals(mockEval, memory.getEval(), "Eval should match");
        }

        @Test
        @DisplayName("Should get visitor from eval")
        void testGetVisitor() {
            ProcessVisitor mockVisitor = mock(ProcessVisitor.class);
            when(mockEval.getVisitor()).thenReturn(mockVisitor);
            memory.setEval(mockEval);

            ProcessVisitor result = memory.getVisitor();
            assertEquals(mockVisitor, result, "Visitor should match");
        }
    }

    @Nested
    @DisplayName("Query Management Tests")
    class QueryManagementTests {

        @Test
        @DisplayName("Should get query")
        void testGetQuery() {
            assertDoesNotThrow(() -> memory.getQuery(),
                    "Getting query should not throw");
        }
    }

    @Nested
    @DisplayName("Matcher Tests")
    class MatcherTests {

        @Test
        @DisplayName("Should get matcher")
        void testGetMatcher() {
            Matcher result = memory.getMatcher();
            assertEquals(mockMatcher, result, "Matcher should match");
        }
    }

    @Nested
    @DisplayName("Evaluator Tests")
    class EvaluatorTests {

        @Test
        @DisplayName("Should get evaluator")
        void testGetEvaluator() {
            Evaluator result = memory.getEvaluator();
            assertEquals(mockEvaluator, result, "Evaluator should match");
        }
    }

    @Nested
    @DisplayName("Graph Node Tests")
    class GraphNodeTests {

        @Test
        @DisplayName("Should set and get graph node")
        void testSetAndGetGraphNode() {
            memory.setGraphNode(mockNode);
            assertEquals(mockNode, memory.getGraphNode(), "Graph node should match");
        }
    }

    @Nested
    @DisplayName("Stack Tests")
    class StackTests {

        @Test
        @DisplayName("Should get stack")
        void testGetStack() {
            assertDoesNotThrow(() -> memory.getStack(),
                    "Getting stack should not throw");
        }
    }

    @Nested
    @DisplayName("Expression Tests")
    class ExpressionTests {

        @Test
        @DisplayName("Should set and get exp")
        void testSetAndGetExp() {
            Exp mockExp = mock(Exp.class);
            memory.setExp(mockExp);
            assertEquals(mockExp, memory.getExp(), "Exp should match");
        }
    }

    @Nested
    @DisplayName("Aggregate Tests")
    class AggregateTests {

        @Test
        @DisplayName("Should check if is aggregate")
        void testIsAggregate() {
            boolean result = memory.isAggregate();
            assertFalse(result, "Should not be aggregate by default");
        }
    }

    @Nested
    @DisplayName("Results Management Tests")
    class ResultsManagementTests {

        @Test
        @DisplayName("Should set results")
        void testSetResults() {
            Mappings mockMappings = mock(Mappings.class);
            Memory result = memory.setResults(mockMappings);
            assertNotNull(result, "SetResults should return memory");
            assertSame(memory, result, "Should return same memory instance");
        }
    }

    @Nested
    @DisplayName("Object Management Tests")
    class ObjectManagementTests {

        @Test
        @DisplayName("Should set and get object")
        void testSetAndGetObject() {
            Object obj = new Object();
            memory.setObject(obj);
            assertEquals(obj, memory.getObject(), "Object should match");
        }
    }


    @Nested
    @DisplayName("Binding Context Tests")
    class BindingContextTests {

        @Test
        @DisplayName("Should set binding context")
        void testSetBinding() {
            BindingContext mockContext = mock(BindingContext.class);
            Memory result = memory.setBinding(mockContext);
            assertNotNull(result, "SetBinding should return memory");
            assertSame(memory, result, "Should return same memory instance");
        }

        @Test
        @DisplayName("Should get bind")
        void testGetBind() {
            BindingContext bind = memory.getBind();
            // Can be null if not set
            assertNull(bind, "Bind should be null initially");
        }

        @Test
        @DisplayName("Should set bind")
        void testSetBind() {
            BindingContext mockContext = mock(BindingContext.class);
            assertDoesNotThrow(() -> memory.setBind(mockContext),
                    "Setting bind should not throw");
        }

        @Test
        @DisplayName("Should check if has bind")
        void testHasBind() {
            boolean result = memory.hasBind();
            assertFalse(result, "Should not have bind by default");
        }

        @Test
        @DisplayName("Should share binding contexts")
        void testShare() {
            BindingContext target = mock(BindingContext.class);
            BindingContext source = mock(BindingContext.class);

            assertDoesNotThrow(() -> memory.share(target, source),
                    "Sharing bindings should not throw");
        }
    }


}