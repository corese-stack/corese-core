package fr.inria.corese.core.next.query.kgram.sparql.datatype;

import fr.inria.corese.core.next.query.kgram.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.CoreseDatatypeException;

/**
 * <p>
 * Title: Corese</p>
 * <p>
 * Description: A Semantic Search Engine</p>
 * <p>
 * Copyright: Copyright INRIA (c) 2007</p>
 * <p>
 * Company: INRIA</p>
 * <p>
 * Project: Acacia</p>
 * <br>
 * This is used for unknown literals that carry their own datatype
 * <br>
 * subclasses: 
 * CoreseExtension: list, map, xml, json
 * CoresePointer:   graph, triple, mappings, etc
 * Note: the subclasses could be merged
 *
 * @author Olivier Corby
 */
public class CoreseUndefLiteral extends CoreseStringLiteral {

    static final Datatype code = Datatype.UNDEF;
    static final CoreseUndefLiteral ERROR, UNBOUND;

    IDatatype datatype = null;

    static {
        ERROR = new CoreseUndefLiteral("Error", IDatatype.SYSTEM);
        UNBOUND = new CoreseUndefLiteral("Unbound", IDatatype.SYSTEM);
    }

    public CoreseUndefLiteral(String value) {
        super(value);
    }

    public CoreseUndefLiteral(String value, String dt) {
        super(value);
        setDatatype(dt);
    }

//  public CoreseUndefLiteral() {
//      super(FUTURE);
//  }
    @Override
    public void setDatatype(String uri) {
        datatype = getGenericDatatype(uri);
    }

    @Override
    public Datatype getCode() {
        return code;
    }

    @Override
    public IDatatype getDatatype() {
        return datatype;
    }

    @Override
    public boolean isUndefined() {
        return true;
    }
    
    @Override
    public boolean isGeneralized() {
        return true;
    }

    @Override
    public boolean isTrue() throws CoreseDatatypeException {
        throw new CoreseDatatypeException("isTrue not implemented");
    }

    @Override
    public boolean isTrueAble() {
        return false;
    }

    void check(IDatatype iod) throws CoreseDatatypeException {
        if ((getDatatype() == null || iod.getDatatype() == null) && getDatatype()!=iod.getDatatype()) {
            throw new CoreseDatatypeException("Datatypes incompatible");
        }
        if (! getDatatype().equals(iod.getDatatype())) {
            throw new CoreseDatatypeException("Datatypes different");
        }
    }

    @Override
    public boolean equalsWE(IDatatype iod) throws CoreseDatatypeException {
        switch (iod.getCode()) {
            case URI:
            case BLANK: 
            case TRIPLE:
                return false;
            // special case with literal !!!
            case LITERAL:
                return iod.equalsWE(this);

            case UNDEF:
                check(iod);
                break;

            default:
                throw new CoreseDatatypeException("Equality evaluation could not be done");
        }

        boolean b = getLabel().equals(iod.getLabel());
        if (!b) {
            throw new CoreseDatatypeException("Equality is false"); // WTF ??
        }
        return b;
    }
    
    @Override
    public int compare(IDatatype dt) throws CoreseDatatypeException{
        if (equalsWE(dt)) {
            return 0;
        }
        throw new CoreseDatatypeException("Comparison evaluation could not be done");
    }

    @Override
    public boolean less(IDatatype iod) throws CoreseDatatypeException {
        return result(iod, getLabel().compareTo(iod.getLabel()) < 0);
    }

    @Override
    public boolean lessOrEqual(IDatatype iod) throws CoreseDatatypeException {
        return result(iod, getLabel().compareTo(iod.getLabel()) <= 0);
    }

    @Override
    public boolean greater(IDatatype iod) throws CoreseDatatypeException {
        return result(iod, getLabel().compareTo(iod.getLabel()) > 0);
    }

    @Override
    public boolean greaterOrEqual(IDatatype iod) throws CoreseDatatypeException {
        return result(iod, getLabel().compareTo(iod.getLabel()) >= 0);
    }

    boolean result(IDatatype dt, boolean b) throws CoreseDatatypeException {
        if (isCompatible(dt)) {
            return b;
        }
        throw new CoreseDatatypeException("Result incompatible");
    }

    boolean isCompatible(IDatatype dt) {
        return dt.isGeneralized()&& getDatatypeURI().equals(dt.getDatatypeURI());
    }
}
