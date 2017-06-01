package fr.inria.acacia.corese.cg.datatype;

import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.exceptions.CoreseDatatypeException;


/**
 * <p>Title: Corese</p>
 * <p>Description: A Semantic Search Engine</p>
 * <p>Copyright: Copyright INRIA (c) 2007</p>
 * <p>Company: INRIA</p>
 * <p>Project: Acacia</p>
 * <br>
 * An implementation of the xsd:anyURI datatype used by Corese
 * <br>
 * @author Olivier Savoie
 */

public class CoreseBlankNode extends CoreseResource {
	static int  code=BLANK;
	
	
	public CoreseBlankNode(String value) {
		super(value);
	}
	
        @Override
	public int getCode() {
		return code;
	}
	
        @Override
	public boolean isBlank() {
		return true;
	}
	
        @Override
	public boolean isConstant() {
		return false;
	}
	
        @Override
	public int compare(IDatatype iod) throws CoreseDatatypeException {
		switch (iod.getCode()){
		case BLANK: return getLabel().compareTo(iod.getLabel());
		}
		throw failure();
		//return iod.polyCompare(this);
	}
		
        @Override
	public boolean less(IDatatype iod) throws CoreseDatatypeException {
		switch (iod.getCode()){
		case BLANK: return getLabel().compareTo(iod.getLabel()) < 0;
		}
		throw failure();
	}
	
        @Override
	public boolean lessOrEqual(IDatatype iod) throws CoreseDatatypeException{
		switch (iod.getCode()){
		case BLANK: return getLabel().compareTo(iod.getLabel()) <= 0;
		}
		throw failure();
	}
	
        @Override
	public boolean greater(IDatatype iod) throws CoreseDatatypeException {
		switch (iod.getCode()){
		case BLANK: return getLabel().compareTo(iod.getLabel()) > 0;
		}
		throw failure();
	}
	
        @Override
	public boolean greaterOrEqual(IDatatype iod) throws CoreseDatatypeException {
		switch (iod.getCode()){
		case BLANK: return getLabel().compareTo(iod.getLabel()) >= 0;
		}
		throw failure();
	}
	
        @Override
	public boolean equalsWE(IDatatype iod) throws CoreseDatatypeException{
		switch (iod.getCode()){
		case BLANK: return getLabel().equals(iod.getLabel()) ;
		}
		return false;
	}
	
	@Override 
	public int hashCode() {
		return getLabel().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final CoreseBlankNode other = (CoreseBlankNode) obj;
		return getLabel().equals(other.getLabel());
	}
	
}
