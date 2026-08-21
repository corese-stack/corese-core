package fr.inria.corese.core.next.data.api.literal;

import fr.inria.corese.core.next.data.api.term.IRI;
/** Resolves the known core datatype associated with a datatype IRI. */
public final class CoreDatatypes {

    private CoreDatatypes() {
        // Utility class.
    }

    public static CoreDatatype from(IRI datatype) {
        if (datatype == null) {
            return CoreDatatype.NONE;
        }
        for(CoreDatatype dt : XSDDatatype.values()) {
            if (dt.getIRI().equals(datatype)) {
                return dt;
            }
        }
        for(CoreDatatype dt : RDFDatatype.values()) {
            if (dt.getIRI().equals(datatype)) {
                return dt;
            }
        }
        return CoreDatatype.NONE;
    }
}
