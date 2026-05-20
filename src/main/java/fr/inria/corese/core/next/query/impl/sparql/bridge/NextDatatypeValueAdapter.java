package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.kgram.api.core.DatatypeValue;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.CoreseDatatypeException;

/**
 * Adapts a runtime {@link IDatatype} to the Corese-next {@link DatatypeValue} API.
 */
public final class NextDatatypeValueAdapter implements DatatypeValue {

    private final IDatatype delegate;

    public NextDatatypeValueAdapter(IDatatype delegate) {
        this.delegate = delegate;
    }

    public static DatatypeValue ofNullable(IDatatype dt) {
        return dt == null ? null : new NextDatatypeValueAdapter(dt);
    }

    public IDatatype delegate() {
        return delegate;
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
        try {
            return delegate.isTrue();
        } catch (CoreseDatatypeException e) {
            return false;
        }
    }

    @Override
    public boolean equalsWE(DatatypeValue other) throws CoreseDatatypeException {
        IDatatype o = unwrap(other);
        return delegate.equalsWE(o);
    }

    @Override
    public int compare(DatatypeValue other) throws CoreseDatatypeException {
        IDatatype o = unwrap(other);
        return delegate.compare(o);
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

    private static IDatatype unwrap(DatatypeValue other) throws CoreseDatatypeException {
        if (other instanceof NextDatatypeValueAdapter a) {
            return a.delegate;
        }
        if (other instanceof IDatatype id) {
            return id;
        }
        throw new CoreseDatatypeException("Incompatible DatatypeValue: " + other);
    }
}
