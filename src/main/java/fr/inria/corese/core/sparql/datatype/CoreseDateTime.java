package fr.inria.corese.core.sparql.datatype;

import fr.inria.corese.core.sparql.api.IDatatype;

import javax.xml.datatype.XMLGregorianCalendar;

public class CoreseDateTime extends CoreseDate {
	static IDatatype.Datatype code = IDatatype.Datatype.DATETIME;

	static final CoreseURI datatype = new CoreseURI(RDF.xsddateTime);

	public CoreseDateTime(String label) {
		super(label);
	}

	public CoreseDateTime()  {
		super();
	}

	public CoreseDateTime(XMLGregorianCalendar calendar) {
		super(calendar);
	}

	@Override
	public IDatatype getDatatype() {
		return datatype;
	}

	@Override
	public IDatatype.Datatype getCode() {
		return code;
	}

}
