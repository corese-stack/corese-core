package fr.inria.corese.core.sparql.datatype;

import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * <p>Title: Corese</p>
 * <p>Description: A Semantic Search Engine</p>
 * <p>Copyright: Copyright INRIA (c) 2007</p>
 * <p>Company: INRIA</p>
 * <p>Project: Acacia</p>
 * <br>
 * An implementation of the xsd:float datatype used by Corese
 * <br>
 * @author Olivier Savoie
 */

public  class CoreseFloat extends CoreseDouble{
	static final CoreseURI datatype=new CoreseURI(RDF.xsdfloat);
	static final IDatatype.Datatype code = IDatatype.Datatype.FLOAT;
        
        CoreseFloat() {}
	
	public CoreseFloat(String value) {
		super(value);
	}
	
	public CoreseFloat(float value) {
		super(value);
	}
	
	public CoreseFloat(double value) {
		super(value);
	}
        
        public static CoreseFloat create(float val) {
            CoreseFloat dt = new CoreseFloat();
            dt.setValue(val);
            return dt;
        }
        
        public static CoreseFloat create(double val) {
            CoreseFloat dt = new CoreseFloat();
            dt.setValue(val);
            return dt;
        }
	
        @Override
	public IDatatype getDatatype(){
		return datatype;
	}
	
        @Override
	 public IDatatype.Datatype getCode(){
			return code;
		}
	
        @Override
	public boolean isFloat(){
		return true;
	}

}
