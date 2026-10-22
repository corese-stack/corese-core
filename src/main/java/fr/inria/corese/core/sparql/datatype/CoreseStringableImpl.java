package fr.inria.corese.core.sparql.datatype;

import fr.inria.corese.core.sparql.api.IDatatype;
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
 * An implementation of all the datatype that representation is a string: URI,
 * literals, strings for example. These classes have the same implemented
 * functions.<br>
 * It subsumes URI, Literal, xsd:string<br>
 * We can compare URI with URI, string and literal/XMLLiteral with string and
 * literal/XMLLiteral (they can be &lt;= modulo lang)<br> e.g. : titi &lt;=
 * toto@en<br> This class factorize util functions such as contains and plus
 * <br>
 *
 * @author Olivier Corby & Olivier Savoie
 */
public abstract class CoreseStringableImpl extends CoreseDatatype {

    static IDatatype.Datatype code = IDatatype.Datatype.STRINGABLE;
    public static int count = 0;
    String value = "";

    private transient volatile IDatatype cachedDateTime;
    private transient volatile IDatatype cachedDate;
    private static final CoreseUndefLiteral CAST_FAILURE = new CoreseUndefLiteral("CAST_FAILURE");

    protected CoreseStringableImpl() {
    }

    protected CoreseStringableImpl(String str) {
        setValue(str);
    }

    @Override
    public void setValue(String str) {
        this.value = str;
        this.cachedDateTime = null;
        this.cachedDate = null;
    }

    @Override
    public String getLabel() {
        return this.value;
    }

    /**
     * Cast a literal to a boolean may be allowed: when the value can be cast to
     * a float, double, decimal or integer, if this value is 0, then return
     * false, else return true
     */
    @Override
    public IDatatype cast(String target) {
        if (RDF.xsddateTime.equals(target)) {
            return castDateTime();
        }
        if (RDF.xsddate.equals(target)) {
            return castDate();
        }
        if (target.equals(RDF.xsdboolean)) {
            return castBoolean(target);
        }
        return super.cast(target);
    }

    private IDatatype castDateTime() {
        IDatatype cached = cachedDateTime;
        if (cached != null) {
            return (cached == CAST_FAILURE) ? null : cached;
        }
        IDatatype res = super.cast(RDF.xsddateTime);
        cachedDateTime = (res != null) ? res : CAST_FAILURE;
        return res;
    }

    private IDatatype castDate() {
        IDatatype cached = cachedDate;
        if (cached != null) {
            return (cached == CAST_FAILURE) ? null : cached;
        }
        IDatatype res = super.cast(RDF.xsddate);
        cachedDate = (res != null) ? res : CAST_FAILURE;
        return res;
    }

    private IDatatype castBoolean(String target) {
        try {
            float f = Float.parseFloat(getLabel());
            if (f == 0) {
                return CoreseBoolean.FALSE;
            } else if (f == 1) {
                return CoreseBoolean.TRUE;
            }
            return null;
        } catch (NumberFormatException e) {
            return super.cast(target);
        }
    }

    @Override
    public IDatatype.Datatype getCode() {
        return code;
    }

    @Override
    public String getLowerCaseLabel() {
        return getLabel().toLowerCase();
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isTrue() throws CoreseDatatypeException {
        return booleanValue();
    }

    @Override
    public boolean booleanValue() {
        return !getLabel().isEmpty();
    }

    @Override
    public boolean isTrueAble() {
        return true;
    }

    @Override
    public boolean contains(IDatatype iod) {
        return getLowerCaseLabel().contains(iod.getLowerCaseLabel());
    }

    @Override
    public boolean startsWith(IDatatype iod) {
        return getLabel().startsWith(iod.getLabel());

    }


    @Override
    public String getNormalizedLabel() {
        return getLabel();
    }


}
