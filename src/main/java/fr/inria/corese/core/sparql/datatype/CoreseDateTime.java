package fr.inria.corese.core.sparql.datatype;

import fr.inria.corese.core.sparql.api.IDatatype;

public class CoreseDateTime extends CoreseDate {
	static int code = DATETIME;

	static final CoreseURI datatype = new CoreseURI(RDF.xsddateTime);

	public CoreseDateTime(String label) {
		super(label);
	}

	public CoreseDateTime()  {
		super();
	}

	@Override
	public IDatatype getDatatype() {
		return datatype;
	}

	@Override
	public int getCode() {
		return code;
	}

}
