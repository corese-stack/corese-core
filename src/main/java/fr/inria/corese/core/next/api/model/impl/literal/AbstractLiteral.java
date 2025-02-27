package fr.inria.corese.core.next.api.model.impl.literal;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.Literal;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.vocabulary.XSD;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

public interface AbstractLiteral extends Literal {

    void setCoreDatatype(CoreDatatype coreDatatype);
}
