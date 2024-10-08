package fr.inria.corese.core.sparql.datatype;

import fr.inria.corese.core.sparql.api.IDatatype;

public class CoreseDay extends CoreseDateElement {
	static final CoreseURI datatype = new CoreseURI(RDF.xsdday);

	public CoreseDay(String value) {
		super(value);
	}

	@Override
	public IDatatype getDatatype() {
		return datatype;
	}
}
