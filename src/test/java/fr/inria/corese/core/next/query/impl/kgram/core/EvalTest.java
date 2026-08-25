package fr.inria.corese.core.next.query.impl.kgram.core;

import fr.inria.corese.core.next.query.impl.kgram.api.core.BindingContext;
import fr.inria.corese.core.next.query.impl.kgram.api.query.*;
import fr.inria.corese.core.next.query.impl.kgram.event.KgramEventDispatcher;
import fr.inria.corese.core.next.query.impl.kgram.event.ResultListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Eval class.
 * Tests KGRAM query evaluation engine, including query processing,
 * memory management, and SPARQL operations.
 */
@DisplayName("Eval Tests")
class EvalTest {

    private Eval eval;
    private Producer mockProducer;
    private Evaluator mockEvaluator;
    private Matcher mockMatcher;
    private Memory mockMemory;
    private ProcessVisitor mockVisitor;

    @BeforeEach
    void setUp() {
        mockProducer = mock(Producer.class);
        mockEvaluator = mock(Evaluator.class);
        mockMatcher = mock(Matcher.class);
        mockMemory = mock(Memory.class);
        mockVisitor = mock(ProcessVisitor.class);

        eval = new Eval(mockProducer, mockEvaluator, mockMatcher);
    }

    @Nested
    @DisplayName("Eval Creation Tests")
    class EvalCreationTests {

        @Test
        @DisplayName("Should create eval with producer, evaluator and matcher")
        void testCreateEval() {
            Eval e = new Eval(mockProducer, mockEvaluator, mockMatcher);
            assertNotNull(e, "Eval should not be null");
        }

        @Test
        @DisplayName("Should create eval using factory method")
        void testCreateEvalFactory() {
            Eval e = Eval.create(mockProducer, mockEvaluator, mockMatcher);
            assertNotNull(e, "Factory-created eval should not be null");
        }

        @Test
        @DisplayName("Should create empty eval")
        void testCreateEmptyEval() {
            Eval e = new Eval();
            assertNotNull(e, "Empty eval should not be null");
        }
    }

    @Nested
    @DisplayName("Producer Management Tests")
    class ProducerManagementTests {

        @Test
        @DisplayName("Should get producer")
        void testGetProducer() {
            Producer result = eval.getProducer();
            assertEquals(mockProducer, result, "Should return producer");
        }

        @Test
        @DisplayName("Should set producer")
        void testSetProducer() {
            Producer newProducer = mock(Producer.class);
            eval.setProducer(newProducer);
            assertEquals(newProducer, eval.getProducer(), "Producer should be updated");
        }

        @Test
        @DisplayName("Should set producer using set method")
        void testSetProducerAlternative() {
            Producer newProducer = mock(Producer.class);
            eval.set(newProducer);
            assertEquals(newProducer, eval.getProducer(), "Producer should be updated");
        }
    }

    @Nested
    @DisplayName("Evaluator Management Tests")
    class EvaluatorManagementTests {

        @Test
        @DisplayName("Should get evaluator")
        void testGetEvaluator() {
            Evaluator result = eval.getEvaluator();
            assertEquals(mockEvaluator, result, "Should return evaluator");
        }

        @Test
        @DisplayName("Should set evaluator using set method")
        void testSetEvaluator() {
            Evaluator newEvaluator = mock(Evaluator.class);
            eval.set(newEvaluator);
            assertEquals(newEvaluator, eval.getEvaluator(), "Evaluator should be updated");
        }
    }

    @Nested
    @DisplayName("Matcher Management Tests")
    class MatcherManagementTests {

        @Test
        @DisplayName("Should get matcher")
        void testGetMatcher() {
            Matcher result = eval.getMatcher();
            assertEquals(mockMatcher, result, "Should return matcher");
        }

        @Test
        @DisplayName("Should set matcher")
        void testSetMatcher() {
            Matcher newMatcher = mock(Matcher.class);
            eval.setMatcher(newMatcher);
            assertEquals(newMatcher, eval.getMatcher(), "Matcher should be updated");
        }

        @Test
        @DisplayName("Should set matcher using set method")
        void testSetMatcherAlternative() {
            Matcher newMatcher = mock(Matcher.class);
            eval.set(newMatcher);
            assertEquals(newMatcher, eval.getMatcher(), "Matcher should be updated");
        }
    }

    @Nested
    @DisplayName("Memory Management Tests")
    class MemoryManagementTests {

        @Test
        @DisplayName("Should set and get memory")
        void testSetAndGetMemory() {
            eval.setMemory(mockMemory);
            assertEquals(mockMemory, eval.getMemory(), "Memory should match");
        }

        @Test
        @DisplayName("Should get environment from memory")
        void testGetEnvironment() {
            eval.setMemory(mockMemory);
            Environment env = eval.getEnvironment();
            assertEquals(mockMemory, env, "Environment should be memory");
        }

        @Test
        @DisplayName("Should get binding from environment")
        void testGetBinding() {
            BindingContext mockBind = mock(BindingContext.class);
            when(mockMemory.getBind()).thenReturn(mockBind);
            eval.setMemory(mockMemory);

            BindingContext result = eval.getBinding();
            assertEquals(mockBind, result, "Should return binding context");
        }

        @Test
        @DisplayName("Should get bind using getBind method")
        void testGetBind() {
            BindingContext mockBind = mock(BindingContext.class);
            when(mockMemory.getBind()).thenReturn(mockBind);
            eval.setMemory(mockMemory);

            BindingContext result = eval.getBind();
            assertEquals(mockBind, result, "Should return binding context");
        }
    }

    @Nested
    @DisplayName("Provider Management Tests")
    class ProviderManagementTests {

        @Test
        @DisplayName("Should set and get provider")
        void testSetAndGetProvider() {
            Provider mockProvider = mock(Provider.class);
            eval.set(mockProvider);
            assertEquals(mockProvider, eval.getProvider(), "Provider should match");
        }
    }

    @Nested
    @DisplayName("Visitor Management Tests")
    class VisitorManagementTests {

        @Test
        @DisplayName("Should set and get visitor")
        void testSetAndGetVisitor() {
            eval.setVisitor(mockVisitor);
            assertEquals(mockVisitor, eval.getVisitor(), "Visitor should match");
        }

        @Test
        @DisplayName("Should have default visitor after creation")
        void testDefaultVisitor() {
            Eval e = new Eval(mockProducer, mockEvaluator, mockMatcher);
            assertNotNull(e.getVisitor(), "Should have default visitor");
        }
    }

    @Nested
    @DisplayName("Event Manager Tests")
    class EventManagerTests {

        @Test
        @DisplayName("Should set and get event manager")
        void testSetAndGetEventManager() {
            KgramEventDispatcher mockManager = mock(KgramEventDispatcher.class);
            eval.setEventManager(mockManager);
            assertEquals(mockManager, eval.getEventManager(),
                    "Event manager should match");
        }

        @Test
        @DisplayName("Should create event manager")
        void testCreateEventManager() {
            assertDoesNotThrow(() -> eval.createManager(),
                    "Creating event manager should not throw");
            assertNotNull(eval.getEventManager(),
                    "Event manager should be created");
        }
    }

    @Nested
    @DisplayName("Result Listener Tests")
    class ResultListenerTests {

        @Test
        @DisplayName("Should set and get result listener")
        void testSetAndGetResultListener() {
            ResultListener mockListener = mock(ResultListener.class);
            eval.setListener(mockListener);
            assertEquals(mockListener, eval.getListener(),
                    "Listener should match");
        }

        @Test
        @DisplayName("Should get null listener by default")
        void testGetListenerDefault() {
            ResultListener listener = eval.getListener();
            assertNull(listener, "Default listener should be null");
        }
    }

    @Nested
    @DisplayName("SPARQL Engine Tests")
    class SPARQLEngineTests {

        @Test
        @DisplayName("Should set and get SPARQL engine")
        void testSetAndGetSPARQLEngine() {
            SPARQLEngine mockEngine = mock(SPARQLEngine.class);
            eval.setSPARQLEngine(mockEngine);
            assertEquals(mockEngine, eval.getSPARQLEngine(),
                    "SPARQL engine should match");
        }
    }


    @Nested
    @DisplayName("Stop Flag Tests")
    class StopFlagTests {

        @Test
        @DisplayName("Should set stop flag")
        void testSetStop() {
            eval.setStop(true);
            assertTrue(eval.isStop(), "Stop should be true");
        }

        @Test
        @DisplayName("Should not be stopped by default")
        void testDefaultStop() {
            assertFalse(eval.isStop(), "Stop should be false by default");
        }

        @Test
        @DisplayName("Should finish and stop")
        void testFinish() {
            eval.finish();
            assertTrue(eval.isStop(), "Should be stopped after finish");
        }
    }

    @Nested
    @DisplayName("Limit Tests")
    class LimitTests {

        @Test
        @DisplayName("Should set limit")
        void testSetLimit() {
            assertDoesNotThrow(() -> eval.setLimit(100),
                    "Setting limit should not throw");
        }
    }

    @Nested
    @DisplayName("Copy Tests")
    class CopyTests {

        @Test
        @DisplayName("Should copy eval with memory and producer")
        void testCopy() {
            Memory mem = mock(Memory.class);
            Producer prod = mock(Producer.class);

            Eval copy = eval.copy(mem, prod);
            assertNotNull(copy, "Copy should not be null");
            assertNotSame(eval, copy, "Copy should be different instance");
        }

        @Test
        @DisplayName("Should copy eval with extern flag")
        void testCopyWithExtern() {
            Memory mem = mock(Memory.class);
            Producer prod = mock(Producer.class);

            Eval copy = eval.copy(mem, prod, true);
            assertNotNull(copy, "Copy with extern should not be null");
        }
    }

    @Nested
    @DisplayName("Plugin Tests")
    class PluginTests {

        @Test
        @DisplayName("Should add plugin")
        void testAddPlugin() {
            Plugin mockPlugin = mock(Plugin.class);
            assertDoesNotThrow(() -> eval.add(mockPlugin),
                    "Adding plugin should not throw");
        }
    }

    @Nested
    @DisplayName("Static Configuration Tests")
    class StaticConfigurationTests {

        @Test
        @DisplayName("Should get and set push edge mappings")
        void testPushEdgeMappings() {
            boolean original = Eval.isPushEdgeMappings();

            Eval.setPushEdgeMappings(false);
            assertFalse(Eval.isPushEdgeMappings(),
                    "PushEdgeMappings should be false");

            // Reset to original
            Eval.setPushEdgeMappings(original);
        }

        @Test
        @DisplayName("Should get and set parameter graph mappings")
        void testParameterGraphMappings() {
            boolean original = Eval.isParameterGraphMappings();

            Eval.setParameterGraphMappings(false);
            assertFalse(Eval.isParameterGraphMappings(),
                    "ParameterGraphMappings should be false");

            // Reset to original
            Eval.setParameterGraphMappings(original);
        }

        @Test
        @DisplayName("Should get and set parameter union mappings")
        void testParameterUnionMappings() {
            boolean original = Eval.isParameterUnionMappings();

            Eval.setParameterUnionMappings(false);
            assertFalse(Eval.isParameterUnionMappings(),
                    "ParameterUnionMappings should be false");

            // Reset to original
            Eval.setParameterUnionMappings(original);
        }

        @Test
        @DisplayName("Should set all new mappings versions")
        void testSetNewMappingsVersion() {
            // Save originals
            boolean origPush = Eval.isPushEdgeMappings();
            boolean origGraph = Eval.isParameterGraphMappings();
            boolean origUnion = Eval.isParameterUnionMappings();

            Eval.setNewMappingsVersion(false);
            assertFalse(Eval.isPushEdgeMappings(), "All flags should be false");
            assertFalse(Eval.isParameterGraphMappings(), "All flags should be false");
            assertFalse(Eval.isParameterUnionMappings(), "All flags should be false");

            // Reset to originals
            Eval.setPushEdgeMappings(origPush);
            Eval.setParameterGraphMappings(origGraph);
            Eval.setParameterUnionMappings(origUnion);
        }
    }

    @Nested
    @DisplayName("Initial Results Tests")
    class InitialResultsTests {

        @Test
        @DisplayName("Should set initial mappings")
        void testSetMappings() {
            Mappings mockMappings = mock(Mappings.class);
            assertDoesNotThrow(() -> eval.setMappings(mockMappings),
                    "Setting mappings should not throw");
        }

        @Test
        @DisplayName("Should get results")
        void testGetResults() {
            // Can be null if not initialized
            assertDoesNotThrow(() -> eval.getResults(),
                    "Getting results should not throw");
        }
    }

    @Nested
    @DisplayName("Stack Tests")
    class StackTests {

        @Test
        @DisplayName("Should get stack")
        void testGetStack() {
            // Can be null if not set
            assertDoesNotThrow(() -> eval.getStack(),
                    "Getting stack should not throw");
        }


    }

    @Nested
    @DisplayName("Count Tests")
    class CountTests {

        @Test
        @DisplayName("Should get count")
        void testGetCount() {
            int count = eval.getCount();
            assertTrue(count >= 0, "Count should be non-negative");
        }
    }

    @Nested
    @DisplayName("Join Mappings Flag Tests")
    class JoinMappingsFlagTests {

        @Test
        @DisplayName("Should check join mappings flag")
        void testIsJoinMappings() {
            boolean flag = eval.isJoinMappings();
            assertTrue(flag, "JoinMappings should be true by default");
        }
    }

    @Nested
    @DisplayName("Static Count Tests")
    class StaticCountTests {

        @Test
        @DisplayName("Should access static count")
        void testStaticCount() {
            int count = Eval.count;
            assertTrue(count >= 0, "Static count should be non-negative");
        }

        @Test
        @DisplayName("Should access display result max")
        void testDisplayResultMax() {
            int max = Eval.DISPLAY_RESULT_MAX;
            assertTrue(max > 0, "Display result max should be positive");
        }
    }

    @Nested
    @DisplayName("Clean Tests")
    class CleanTests {

        @Test
        @DisplayName("Should clean eval state")
        void testClean() {
            assertDoesNotThrow(() -> eval.clean(),
                    "Clean should not throw exception");
        }
    }
}