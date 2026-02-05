package fr.inria.corese.core.next.query.kgram.adapter;

import fr.inria.corese.core.next.query.kgram.api.core.DatatypeValue;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.CoreseDatatypeException;

/**
 * Adapter to use an IDatatype via the DatatypeValue interface.
 */
public record DatatypeAdapter(IDatatype delegate) implements DatatypeValue {

    /**
     * Constructs an adapter for the given IDatatype delegate.
     *
     * @param delegate the IDatatype instance to wrap
     * @throws IllegalArgumentException if delegate is null
     */
    public DatatypeAdapter {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate cannot be null");
        }
    }


    /**
     * Unwraps a DatatypeValue to get the underlying IDatatype.
     *
     * @param value the value to unwrap
     * @return the IDatatype, or null if value is null or not an adapter
     */
    public static IDatatype unwrap(DatatypeValue value) {
        if (value instanceof DatatypeAdapter(IDatatype delegate1)) {
            return delegate1;
        }
        return null;
    }

    @Override
    public String getLabel() {
        return delegate.getLabel();
    }

    @Override
    public Object getValue() {
        return delegate.getValue();
    }

    @Override
    public String getDatatypeURI() {
        return delegate.getDatatypeURI();
    }

    @Override
    public boolean isTrue() {
        // VERSION 1: Use isTrueTest() if that's the method name
        return delegate.isTrueTest();
    }

    @Override
    public boolean equalsWE(DatatypeValue other) throws CoreseDatatypeException {
        if (other instanceof DatatypeAdapter(IDatatype delegate1)) {
            return delegate.equalsWE(delegate1);
        }
        return false;
    }

    @Override
    public int compare(DatatypeValue other) throws CoreseDatatypeException {
        if (other instanceof DatatypeAdapter(IDatatype delegate1)) {
            return delegate.compare(delegate1);
        }
        throw new IllegalArgumentException("Cannot compare with non-IDatatype value");
    }

    @Override
    public int intValue() {
        return delegate.intValue();
    }

    @Override
    public double doubleValue() {
        return delegate.doubleValue();
    }

    @Override
    public boolean isNumber() {
        return delegate.isNumber();
    }

    @Override
    public boolean isLiteral() {
        return delegate.isLiteral();
    }

    @Override
    public boolean isURI() {
        return delegate.isURI();
    }

    @Override
    public boolean isBlank() {
        return delegate.isBlank();
    }

}