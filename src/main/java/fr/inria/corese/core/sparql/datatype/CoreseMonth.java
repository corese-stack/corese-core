package fr.inria.corese.core.sparql.datatype;

import fr.inria.corese.core.sparql.api.IDatatype;

public class CoreseMonth extends CoreseDateElement {
	static final CoreseURI datatype = new CoreseURI(RDF.xsdmonth);

	public CoreseMonth(String value) {
		super(value);
	}

	@Override
	public IDatatype getDatatype() {
		return datatype;
	}
}
