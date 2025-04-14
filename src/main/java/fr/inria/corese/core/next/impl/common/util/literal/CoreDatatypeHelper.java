package fr.inria.corese.core.next.impl.common.util.literal;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.impl.common.literal.RDF;
import fr.inria.corese.core.next.impl.common.literal.XSD;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CoreDatatypeHelper {

    private CoreDatatypeHelper() {
        // Prevent instantiation
    }

    public static CoreDatatype from(IRI datatype) {
        if (datatype == null) {
            return CoreDatatype.NONE;
        }
        for(CoreDatatype dt : XSD.values()) {
            if (dt.getIRI().equals(datatype)) {
                return dt;
            }
        }
        for(CoreDatatype dt : RDF.values()) {
            if (dt.getIRI().equals(datatype)) {
                return dt;
            }
        }
        return CoreDatatype.NONE;
    }
}