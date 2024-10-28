package fr.inria.corese.core.sparql.datatype;

import fr.inria.corese.core.sparql.api.IDatatype;

public class CoreseYear extends CoreseDateElement {
	static final CoreseURI datatype = new CoreseURI(RDF.xsdyear);

	public CoreseYear(String value) {
		super(value);
	}

	@Override
	public IDatatype getDatatype() {
		return datatype;
	}
}
