package fr.inria.corese.core.next.query.kgram.sparql.datatype;

import fr.inria.corese.core.next.data.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.query.kgram.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.CoreseDatatypeException;

/**
 * <p>Title: Corese</p>
 * <p>Description: A Semantic Search Engine</p>
 * <p>Copyright: Copyright INRIA (c) 2007</p>
 * <p>Company: INRIA</p>
 * <p>Project: Acacia</p>
 * <br>
 * An implementation of the rdf:XMLLiteral datatype used by Corese
 * <br>
 * @author Olivier Corby
 */

public class CoreseXMLLiteral extends CoreseStringLiteral {
    static Datatype code= Datatype.XMLLITERAL;
    static final CoreseURI datatype=new CoreseURI(fr.inria.corese.core.next.data.impl.common.vocabulary.RDF.XMLLiteral.getIRI().stringValue());
    //  to store an object such as an XML DOM (see xslt() xpath())
	private Object object; 

    public CoreseXMLLiteral(String value) {
    	super(value);
    }
    
    @Override
    public void setObject(Object obj){
    	object = obj;
    }
    
    @Override
    public boolean isXMLLiteral(){
		return true;
	}
    
    @Override
    public Object getNodeObject(){
    	return object;
    }
    
    @Override
    public boolean isLoop(){
        return object != null && object instanceof Loopable;
    }
    
    @Override
    public  Datatype getCode(){
    	return code;
    }
    
    @Override
    public IDatatype getDatatype(){
    	return datatype;
    }
    
    // TBD: it should parse the XML content
    @Override
    public IDatatype typeCheck(){
        if (getLabel().startsWith("<") 
                && ! getLabel().endsWith(">")){
            return DatatypeMap.createUndef(getLabel(), RDF.XMLLiteral.getIRI().stringValue());
        }
        return this;
    }
    
    
    @Override
    public boolean equalsWE(IDatatype iod) throws CoreseDatatypeException {
    	switch (iod.getCode()){
    	case XMLLITERAL: return getLabel().equals(iod.getLabel());
    	case URI:
    	case BLANK: case TRIPLE: return false;
    	}
        throw new CoreseDatatypeException("Equality evaluation could not be done");
    }
  
}
