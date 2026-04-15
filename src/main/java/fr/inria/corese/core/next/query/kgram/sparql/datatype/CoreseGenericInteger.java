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
        switch (datatype){
            case fr.inria.corese.core.next.query.kgram.sparql.datatype.RDF.xsdbyte:
                return intValue() <= 127 && intValue() >= -128;
                
            case fr.inria.corese.core.next.query.kgram.sparql.datatype.RDF.xsdshort:
                return intValue() <= 32767 && intValue() >= -32768;
                
             case fr.inria.corese.core.next.query.kgram.sparql.datatype.RDF.xsdint:
                return intValue() <= 2147483647 && intValue() >= -2147483648;    
                
            case fr.inria.corese.core.next.query.kgram.sparql.datatype.RDF.xsdpositiveInteger:
                return intValue() > 0; 
                
            case fr.inria.corese.core.next.query.kgram.sparql.datatype.RDF.xsdnegativeInteger:
                return intValue() < 0; 
                
            case fr.inria.corese.core.next.query.kgram.sparql.datatype.RDF.xsdnonNegativeInteger:
                return intValue() >= 0; 
                
            case fr.inria.corese.core.next.query.kgram.sparql.datatype.RDF.xsdnonPositiveInteger:
                 return intValue() <= 0; 
                
            case fr.inria.corese.core.next.query.kgram.sparql.datatype.RDF.xsdunsignedByte:
                return validate(XSD.xsdByte.getIRI().stringValue()) && intValue() >= 0;
                
            case fr.inria.corese.core.next.query.kgram.sparql.datatype.RDF.xsdunsignedInt:
                return validate(XSD.xsdInt.getIRI().stringValue()) && intValue() >= 0;
                
            case fr.inria.corese.core.next.query.kgram.sparql.datatype.RDF.xsdunsignedLong:
                return validate(XSD.xsdLong.getIRI().stringValue()) && intValue() >= 0;
                
            case fr.inria.corese.core.next.query.kgram.sparql.datatype.RDF.xsdunsignedShort:
                return validate(XSD.xsdShort.getIRI().stringValue()) && intValue() >= 0;
                
        }
        return true;
    }
    
    
}
