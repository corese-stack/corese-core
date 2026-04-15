package fr.inria.corese.core.next.query.kgram.sparql.datatype;

import fr.inria.corese.core.next.data.impl.common.vocabulary.XSD;
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

    static Datatype code = Datatype.STRINGABLE;
    public static int count = 0;
    String value = "";

    public CoreseStringableImpl() {
    }

    public CoreseStringableImpl(String str) {
        setValue(str);
    }

    @Override
    public void setValue(String str) {
        this.value = str;
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
        if (target.equals(XSD.xsdBoolean.getIRI().stringValue())) {
            try {
                Float f =  Float.parseFloat(getLabel());
                if (f == 0) {
                    return CoreseBoolean.FALSE;
                } else if (f == 1) {
                    return CoreseBoolean.TRUE;
                } else {
                    return null;
                }
            } catch (NumberFormatException e) {
                return super.cast(target);
            }
        } else {
            return super.cast(target);
        }
    }

    @Override
    public Datatype getCode() {
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
        return getLabel().length() > 0;
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

    //optimization
    public boolean contains(String label) {
        return getLowerCaseLabel().contains(label.toLowerCase());
    }

    public boolean startsWith(String label) {
        return getLabel().startsWith(label);
    }

    @Override
    public String getNormalizedLabel() {
        return getLabel();
    }

    public static String getNormalizedLabel(String label) {
        return label;
    }

    public boolean equals(String siod) {
        return getLabel().equals(siod);
    }

    int intCompare(CoreseStringableImpl icod) {
        return getLabel().compareTo(icod.getLabel());
    }

}
