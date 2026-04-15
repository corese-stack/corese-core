package fr.inria.corese.core.next.query.kgram.sparql.datatype;

import fr.inria.corese.core.next.query.kgram.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.CoreseDatatypeException;

/**
 * 
 * Generic datatype for other such as dayTimeDuration
 */
public class CoreseGeneric extends CoreseString {
	
    IDatatype datatype;
	
	public CoreseGeneric(String label, String uri){
		super(label);
		setDatatype(uri);
	}
	
	public CoreseGeneric(String label){
		super(label);
	}
	
    @Override
	public void setDatatype(String uri){
	    datatype = getGenericDatatype(uri);
	}

    @Override
	public IDatatype getDatatype(){
		return datatype;
	}
	
    @Override
	public boolean equalsWE(IDatatype dt) throws CoreseDatatypeException {
		switch (dt.getCode()){
		case STRING: 
			if (! getDatatypeURI().equals(dt.getDatatypeURI())) throw new CoreseDatatypeException("Equality comparison not possible with different datatypes");
			return getLabel().equals(dt.getLabel());
		case URI:
		case BLANK: case TRIPLE: return false;
		}
		throw new CoreseDatatypeException("Equality comparison not possible");
	}
	
	
}
