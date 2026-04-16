package fr.inria.corese.core.next.query.kgram.sparql.datatype;

import fr.inria.corese.core.next.query.kgram.sparql.api.IDatatype;
import fr.inria.corese.core.next.data.impl.common.vocabulary.XSD;

/**
 * int short ...
 *
 * @author corby
 * @deprecated @TODO replace by fr.inria.corese.core.next.data class
 */
public class CoreseGenericInteger extends CoreseInteger {

    IDatatype datatype;
    
    CoreseGenericInteger(){}

    public CoreseGenericInteger(String label, String uri) {
        super(label);
        setDatatype(uri);
    }

    public CoreseGenericInteger(String label) {
        super(label);
        // by safety:
        datatype = super.getDatatype();
    }

    public CoreseGenericInteger(int n, String uri) {
        super(n);
        setDatatype(uri);
    }
    
    public CoreseGenericInteger(long n) {
        super(n);
        setDatatype(XSD.xsdLong.getIRI().stringValue());
    } 
    
    // for computing, without label
    public static CoreseGenericInteger create(long n) {
        CoreseGenericInteger i = new CoreseGenericInteger();
        i.setValue(n);
        i.setDatatype(XSD.xsdLong.getIRI().stringValue());
        return i;
    }

    @Override
    public void setDatatype(String uri) {
        datatype = getGenericDatatype(uri);
    }

    @Override
    public IDatatype getDatatype() {
        return datatype;
    }
    
    @Override
    public boolean isXSDInteger() { 
        return false;
    }

    @Override
    public IDatatype typeCheck() {
        if (validate(getDatatypeURI())){
            return this;
        }
        else {
            return DatatypeMap.createUndef(getLabel(), getDatatypeURI());
        }
    }
    
    boolean validate(String datatype){
        final String xsdByte = XSD.xsdByte.getIRI().stringValue();
        final String xsdShort = XSD.xsdShort.getIRI().stringValue();
        final String xsdInt = XSD.xsdInt.getIRI().stringValue();
        final String xsdPositiveInteger = XSD.xsdPositiveInteger.getIRI().stringValue();
        final String xsdNegativeInteger = XSD.xsdNegativeInteger.getIRI().stringValue();
        final String xsdNonNegativeInteger = XSD.xsdNonNegativeInteger.getIRI().stringValue();
        final String xsdNonPositiveInteger = XSD.xsdNonPositiveInteger.getIRI().stringValue();
        final String xsdUnsignedByte = XSD.xsdUnsignedByte.getIRI().stringValue();
        final String xsdUnsignedInt = XSD.xsdUnsignedInt.getIRI().stringValue();
        final String xsdUnsignedLong = XSD.xsdUnsignedLong.getIRI().stringValue();
        final String xsdUnsignedShort = XSD.xsdUnsignedShort.getIRI().stringValue();
        return switch (datatype) {
            case xsdByte -> intValue() <= 127 && intValue() >= -128;
            case xsdShort -> intValue() <= 32767 && intValue() >= -32768;
            case xsdInt -> intValue() <= 2147483647 && intValue() >= -2147483648;
            case xsdPositiveInteger -> intValue() > 0;
            case xsdNegativeInteger -> intValue() < 0;
            case xsdNonNegativeInteger -> intValue() >= 0;
            case xsdNonPositiveInteger -> intValue() <= 0;
            case xsdUnsignedByte -> validate(XSD.xsdByte.getIRI().stringValue()) && intValue() >= 0;
            case xsdUnsignedInt -> validate(XSD.xsdInt.getIRI().stringValue()) && intValue() >= 0;
            case xsdUnsignedLong -> validate(XSD.xsdLong.getIRI().stringValue()) && intValue() >= 0;
            case xsdUnsignedShort -> validate(XSD.xsdShort.getIRI().stringValue()) && intValue() >= 0;
            default -> true;
        };
    }
    
    
}
