package fr.inria.corese.core.next.kgram.adapter;

import fr.inria.corese.core.next.kgram.api.core.Node;
import fr.inria.corese.core.next.kgram.core.Mappings;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the BindingAdapter class.
 * Tests the adapter pattern implementation that converts a Binding
 * into a BindingContext.
 */
@DisplayName("BindingAdapter Tests")
class BindingAdapterTest {

    private Binding mockBinding;
    private BindingAdapter adapter;
    private fr.inria.corese.core.kgram.api.core.Expr mockExpr;
    private Node mockNode;
    private IDatatype mockDatatype;

    @BeforeEach
    void setUp() {
        mockBinding = mock(Binding.class);
        mockExpr = mock(fr.inria.corese.core.kgram.api.core.Expr.class);
        mockNode = mock(Node.class);
        mockDatatype = mock(IDatatype.class);

        adapter = new BindingAdapter(mockBinding);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create adapter with valid binding")
        void testConstructorWithValidBinding() {
            BindingAdapter ba = new BindingAdapter(mockBinding);
            assertNotNull(ba, "Adapter should not be null");
            assertEquals(mockBinding, ba.delegate(), "Delegate should match");
        }

        @Test
        @DisplayName("Should throw exception with null binding")
        void testConstructorWithNullBinding() {
            assertThrows(IllegalArgumentException.class,
                    () -> new BindingAdapter(null),
                    "Should throw IllegalArgumentException for null delegate");
        }
    }

    @Nested
    @DisplayName("Delegate Tests")
    class DelegateTests {

        @Test
        @DisplayName("Should return delegate")
        void testDelegate() {
            Binding result = adapter.delegate();
            assertSame(mockBinding, result, "Should return same delegate instance");
        }
    }


    @Nested
    @DisplayName("setValue Tests")
    class SetValueTests {

        @Test
        @DisplayName("Should set value by variable name")
        void testSetValue() {
            String varName = "x";
            when(mockExpr.getLabel()).thenReturn(varName);
            when(mockBinding.getVariables()).thenReturn(createExprList(mockExpr));
            when(mockNode.getDatatypeValue()).thenReturn(mockDatatype);

            adapter.setValue(varName, mockNode);
            verify(mockBinding).set(mockExpr, mockDatatype);
        }

        @Test
        @DisplayName("Should set null value")
        void testSetNullValue() {
            String varName = "x";
            when(mockExpr.getLabel()).thenReturn(varName);
            when(mockBinding.getVariables()).thenReturn(createExprList(mockExpr));

            adapter.setValue(varName, null);
            verify(mockBinding).set(mockExpr, null);
        }

        @Test
        @DisplayName("Should do nothing for unknown variable")
        void testSetValueUnknownVariable() {
            when(mockBinding.getVariables()).thenReturn(new ArrayList<>());

            adapter.setValue("unknown", mockNode);
            verify(mockBinding, never()).set(any(), any());
        }
    }

    @Nested
    @DisplayName("isDefined Tests")
    class IsDefinedTests {

        @Test
        @DisplayName("Should return true for defined variable")
        void testIsDefinedTrue() {
            String varName = "x";
            when(mockExpr.getLabel()).thenReturn(varName);
            when(mockBinding.getVariables()).thenReturn(createExprList(mockExpr));
            when(mockBinding.isBound(varName)).thenReturn(true);

            boolean result = adapter.isDefined(varName);
            assertTrue(result, "Should return true for defined variable");
        }


        @Test
        @DisplayName("Should return false for unknown variable")
        void testIsDefinedUnknown() {
            when(mockBinding.getVariables()).thenReturn(new ArrayList<>());

            boolean result = adapter.isDefined("unknown");
            assertFalse(result, "Should return false for unknown variable");
        }
    }

    @Nested
    @DisplayName("copy Tests")
    class CopyTests {

        @Test
        @DisplayName("Should copy from another BindingAdapter")
        void testCopyFromBindingAdapter() {
            Binding otherBinding = mock(Binding.class);
            BindingAdapter other = new BindingAdapter(otherBinding);

            adapter.copy(other);
            verify(mockBinding).share(otherBinding);
        }

        @Test
        @DisplayName("Should handle null other")
        void testCopyNull() {
            assertDoesNotThrow(() -> adapter.copy(null),
                    "Should not throw when copying from null");
        }
    }


    @Nested
    @DisplayName("visit Tests")
    class VisitTests {

        @Test
        @DisplayName("Should visit with next kgram types")
        void testVisitNextKgramTypes() {
            fr.inria.corese.core.next.kgram.core.Exp mockExp =
                    mock(fr.inria.corese.core.next.kgram.core.Exp.class);
            Mappings mockMappings1 = mock(Mappings.class);
            Mappings mockMappings2 = mock(Mappings.class);

            assertDoesNotThrow(() -> adapter.visit(mockExp, mockNode, mockMappings1, mockMappings2),
                    "Should not throw when visiting with next kgram types");
        }

        @Test
        @DisplayName("Should visit with legacy kgram types")
        void testVisitLegacyKgramTypes() {
            fr.inria.corese.core.kgram.core.Exp mockExp =
                    mock(fr.inria.corese.core.kgram.core.Exp.class);
            fr.inria.corese.core.kgram.api.core.Node mockLegacyNode =
                    mock(fr.inria.corese.core.kgram.api.core.Node.class);
            fr.inria.corese.core.kgram.core.Mappings mockMappings1 =
                    mock(fr.inria.corese.core.kgram.core.Mappings.class);
            fr.inria.corese.core.kgram.core.Mappings mockMappings2 =
                    mock(fr.inria.corese.core.kgram.core.Mappings.class);

            assertDoesNotThrow(() -> adapter.visit(mockExp, mockLegacyNode, mockMappings1, mockMappings2),
                    "Should not throw when visiting with legacy kgram types");
        }


        @Test
        @DisplayName("Should handle visit with null parameters")
        void testVisitWithNullParameters() {
            fr.inria.corese.core.next.kgram.core.Exp mockExp =
                    mock(fr.inria.corese.core.next.kgram.core.Exp.class);

            assertDoesNotThrow(() -> adapter.visit(mockExp, null, null, null),
                    "Should not throw when visiting with null parameters");
        }
    }

    @Nested
    @DisplayName("equals Tests")
    class EqualsTests {

        @Test
        @DisplayName("Should be equal to itself")
        void testEqualsSelf() {
            assertEquals(adapter, adapter, "Should be equal to itself");
        }

        @Test
        @DisplayName("Should not be equal to null")
        void testNotEqualsNull() {
            assertNotEquals(null, adapter, "Should not be equal to null");
        }


    }

    @Nested
    @DisplayName("Record Tests")
    class RecordTests {

        @Test
        @DisplayName("Should have consistent hashCode")
        void testHashCode() {
            int hash1 = adapter.hashCode();
            int hash2 = adapter.hashCode();
            assertEquals(hash1, hash2, "HashCode should be consistent");
        }

        @Test
        @DisplayName("Should have proper toString")
        void testToString() {
            String str = adapter.toString();
            assertNotNull(str, "toString should not return null");
            assertTrue(str.contains("BindingAdapter"),
                    "toString should contain class name");
        }
    }

    // Helper method to create a list of Expr
    private List<fr.inria.corese.core.kgram.api.core.Expr> createExprList(
            fr.inria.corese.core.kgram.api.core.Expr... exprs) {
        List<fr.inria.corese.core.kgram.api.core.Expr> list = new ArrayList<>();
        Collections.addAll(list, exprs);
        return list;
    }
}