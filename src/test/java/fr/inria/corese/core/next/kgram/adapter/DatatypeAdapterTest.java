package fr.inria.corese.core.next.kgram.adapter;

import fr.inria.corese.core.next.query.kgram.adapter.DatatypeAdapter;
import fr.inria.corese.core.next.query.kgram.api.core.DatatypeValue;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.CoreseDatatypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("DatatypeAdapter Tests")
class DatatypeAdapterTest {

    private IDatatype mockDatatype;
    private DatatypeAdapter adapter;


    @BeforeEach
    void setUp() {
        mockDatatype = mock(IDatatype.class);
        adapter = new DatatypeAdapter(mockDatatype);
    }


    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create adapter with valid datatype")
        void testConstructorWithValidDatatype() {
            DatatypeAdapter da = new DatatypeAdapter(mockDatatype);
            assertNotNull(da, "Adapter should not be null");
            assertEquals(mockDatatype, da.delegate(), "Delegate should match");
        }

        @Test
        @DisplayName("Should throw exception with null datatype")
        void testConstructorWithNullDatatype() {
            assertThrows(IllegalArgumentException.class,
                    () -> new DatatypeAdapter(null),
                    "Should throw IllegalArgumentException for null delegate");
        }
    }

    @Nested
    @DisplayName("Delegate Tests")
    class DelegateTests {

        @Test
        @DisplayName("Should return delegate")
        void testDelegate() {
            IDatatype result = adapter.delegate();
            assertSame(mockDatatype, result, "Should return same delegate instance");
        }
    }

    @Nested
    @DisplayName("unwrap Tests")
    class UnwrapTests {

        @Test
        @DisplayName("Should unwrap DatatypeAdapter")
        void testUnwrapAdapter() {
            IDatatype result = DatatypeAdapter.unwrap(adapter);
            assertSame(mockDatatype, result, "Should unwrap to delegate");
        }

        @Test
        @DisplayName("Should return null for null value")
        void testUnwrapNull() {
            assertNull(null, "Should return null for null value");
        }

        @Test
        @DisplayName("Should return null for non-adapter value")
        void testUnwrapNonAdapter() {
            DatatypeValue nonAdapter = mock(DatatypeValue.class);
            IDatatype result = DatatypeAdapter.unwrap(nonAdapter);
            assertNull(result, "Should return null for non-adapter value");
        }
    }

    @Nested
    @DisplayName("getLabel Tests")
    class GetLabelTests {

        @Test
        @DisplayName("Should get label")
        void testGetLabel() {
            String expectedLabel = "test-label";
            when(mockDatatype.getLabel()).thenReturn(expectedLabel);

            String result = adapter.getLabel();
            assertEquals(expectedLabel, result, "Label should match");
            verify(mockDatatype).getLabel();
        }

        @Test
        @DisplayName("Should return null label")
        void testGetNullLabel() {
            when(mockDatatype.getLabel()).thenReturn(null);

            String result = adapter.getLabel();
            assertNull(result, "Should return null label");
        }
    }


    @Nested
    @DisplayName("getDatatypeURI Tests")
    class GetDatatypeURITests {

        @Test
        @DisplayName("Should get datatype URI")
        void testGetDatatypeURI() {
            String expectedURI = "http://www.w3.org/2001/XMLSchema#string";
            when(mockDatatype.getDatatypeURI()).thenReturn(expectedURI);

            String result = adapter.getDatatypeURI();
            assertEquals(expectedURI, result, "Datatype URI should match");
            verify(mockDatatype).getDatatypeURI();
        }

        @Test
        @DisplayName("Should return null datatype URI")
        void testGetNullDatatypeURI() {
            when(mockDatatype.getDatatypeURI()).thenReturn(null);

            String result = adapter.getDatatypeURI();
            assertNull(result, "Should return null datatype URI");
        }
    }

    @Nested
    @DisplayName("isTrue Tests")
    class IsTrueTests {

        @Test
        @DisplayName("Should return true")
        void testIsTrueReturnsTrue() {
            when(mockDatatype.isTrueTest()).thenReturn(true);

            boolean result = adapter.isTrue();
            assertTrue(result, "Should return true");
            verify(mockDatatype).isTrueTest();
        }

        @Test
        @DisplayName("Should return false")
        void testIsTrueReturnsFalse() {
            when(mockDatatype.isTrueTest()).thenReturn(false);

            boolean result = adapter.isTrue();
            assertFalse(result, "Should return false");
            verify(mockDatatype).isTrueTest();
        }
    }

    @Nested
    @DisplayName("equalsWE Tests")
    class EqualsWETests {

        @Test
        @DisplayName("Should compare equal datatypes")
        void testEqualsWETrue() throws CoreseDatatypeException {
            IDatatype otherDatatype = mock(IDatatype.class);
            DatatypeAdapter other = new DatatypeAdapter(otherDatatype);
            when(mockDatatype.equalsWE(otherDatatype)).thenReturn(true);

            boolean result = adapter.equalsWE(other);
            assertTrue(result, "Should be equal");
            verify(mockDatatype).equalsWE(otherDatatype);
        }

        @Test
        @DisplayName("Should compare unequal datatypes")
        void testEqualsWEFalse() throws CoreseDatatypeException {
            IDatatype otherDatatype = mock(IDatatype.class);
            DatatypeAdapter other = new DatatypeAdapter(otherDatatype);
            when(mockDatatype.equalsWE(otherDatatype)).thenReturn(false);

            boolean result = adapter.equalsWE(other);
            assertFalse(result, "Should not be equal");
            verify(mockDatatype).equalsWE(otherDatatype);
        }

        @Test
        @DisplayName("Should return false for non-adapter value")
        void testEqualsWENonAdapter() throws CoreseDatatypeException {
            DatatypeValue other = mock(DatatypeValue.class);

            boolean result = adapter.equalsWE(other);
            assertFalse(result, "Should return false for non-adapter value");
        }

        @Test
        @DisplayName("Should propagate CoreseDatatypeException")
        void testEqualsWEException() throws CoreseDatatypeException {
            IDatatype otherDatatype = mock(IDatatype.class);
            DatatypeAdapter other = new DatatypeAdapter(otherDatatype);
            CoreseDatatypeException exception = new CoreseDatatypeException("Test error");
            when(mockDatatype.equalsWE(otherDatatype)).thenThrow(exception);

            assertThrows(CoreseDatatypeException.class,
                    () -> adapter.equalsWE(other),
                    "Should propagate CoreseDatatypeException");
        }
    }

    @Nested
    @DisplayName("compare Tests")
    class CompareTests {

        @Test
        @DisplayName("Should compare less than")
        void testCompareLessThan() throws CoreseDatatypeException {
            IDatatype otherDatatype = mock(IDatatype.class);
            DatatypeAdapter other = new DatatypeAdapter(otherDatatype);
            when(mockDatatype.compare(otherDatatype)).thenReturn(-1);

            int result = adapter.compare(other);
            assertEquals(-1, result, "Should return -1 for less than");
            verify(mockDatatype).compare(otherDatatype);
        }

        @Test
        @DisplayName("Should compare equal")
        void testCompareEqual() throws CoreseDatatypeException {
            IDatatype otherDatatype = mock(IDatatype.class);
            DatatypeAdapter other = new DatatypeAdapter(otherDatatype);
            when(mockDatatype.compare(otherDatatype)).thenReturn(0);

            int result = adapter.compare(other);
            assertEquals(0, result, "Should return 0 for equal");
            verify(mockDatatype).compare(otherDatatype);
        }

        @Test
        @DisplayName("Should compare greater than")
        void testCompareGreaterThan() throws CoreseDatatypeException {
            IDatatype otherDatatype = mock(IDatatype.class);
            DatatypeAdapter other = new DatatypeAdapter(otherDatatype);
            when(mockDatatype.compare(otherDatatype)).thenReturn(1);

            int result = adapter.compare(other);
            assertEquals(1, result, "Should return 1 for greater than");
            verify(mockDatatype).compare(otherDatatype);
        }

        @Test
        @DisplayName("Should throw exception for non-adapter value")
        void testCompareNonAdapter() {
            DatatypeValue other = mock(DatatypeValue.class);

            assertThrows(IllegalArgumentException.class,
                    () -> adapter.compare(other),
                    "Should throw IllegalArgumentException for non-adapter value");
        }

        @Test
        @DisplayName("Should propagate CoreseDatatypeException")
        void testCompareException() throws CoreseDatatypeException {
            IDatatype otherDatatype = mock(IDatatype.class);
            DatatypeAdapter other = new DatatypeAdapter(otherDatatype);
            CoreseDatatypeException exception = new CoreseDatatypeException("Test error");
            when(mockDatatype.compare(otherDatatype)).thenThrow(exception);

            assertThrows(CoreseDatatypeException.class,
                    () -> adapter.compare(other),
                    "Should propagate CoreseDatatypeException");
        }
    }

    @Nested
    @DisplayName("intValue Tests")
    class IntValueTests {

        @Test
        @DisplayName("Should get int value")
        void testIntValue() {
            int expectedValue = 42;
            when(mockDatatype.intValue()).thenReturn(expectedValue);

            int result = adapter.intValue();
            assertEquals(expectedValue, result, "Int value should match");
            verify(mockDatatype).intValue();
        }

        @Test
        @DisplayName("Should get zero int value")
        void testIntValueZero() {
            when(mockDatatype.intValue()).thenReturn(0);

            int result = adapter.intValue();
            assertEquals(0, result, "Int value should be zero");
        }

        @Test
        @DisplayName("Should get negative int value")
        void testIntValueNegative() {
            when(mockDatatype.intValue()).thenReturn(-10);

            int result = adapter.intValue();
            assertEquals(-10, result, "Int value should be negative");
        }
    }

    @Nested
    @DisplayName("doubleValue Tests")
    class DoubleValueTests {

        @Test
        @DisplayName("Should get double value")
        void testDoubleValue() {
            double expectedValue = 3.14;
            when(mockDatatype.doubleValue()).thenReturn(expectedValue);

            double result = adapter.doubleValue();
            assertEquals(expectedValue, result, "Double value should match");
            verify(mockDatatype).doubleValue();
        }

        @Test
        @DisplayName("Should get zero double value")
        void testDoubleValueZero() {
            when(mockDatatype.doubleValue()).thenReturn(0.0);

            double result = adapter.doubleValue();
            assertEquals(0.0, result, "Double value should be zero");
        }

        @Test
        @DisplayName("Should get negative double value")
        void testDoubleValueNegative() {
            when(mockDatatype.doubleValue()).thenReturn(-5.5);

            double result = adapter.doubleValue();
            assertEquals(-5.5, result, "Double value should be negative");
        }
    }


}