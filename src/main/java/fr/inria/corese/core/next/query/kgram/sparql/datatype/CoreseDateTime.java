package fr.inria.corese.core.next.query.kgram.sparql.datatype;

import fr.inria.corese.core.next.data.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.query.kgram.sparql.api.IDatatype;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 *
 * @deprecated @TODO replace by fr.inria.corese.core.next.data class
 */
public class CoreseDateTime extends CoreseDate {
	static Datatype code = Datatype.DATETIME;

	static final fr.inria.corese.core.next.query.kgram.sparql.datatype.CoreseURI datatype = new CoreseURI(XSD.xsdDateTime.getIRI().stringValue());

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
	public Datatype getCode() {
		return code;
	}

}
