package fr.inria.corese.core.next.data.api.literal;

import java.util.HashMap;
import java.util.Map;

import fr.inria.corese.core.next.data.api.term.IRI;

/** Resolves the known core datatype associated with a datatype IRI. */
public final class CoreDatatypes {

    private static final Map<IRI, CoreDatatype> DATATYPE_MAP = new HashMap<>();

    static {
        for (CoreDatatype dt : XSDDatatype.values()) {
            DATATYPE_MAP.put(dt.getIRI(), dt);
        }
        for (CoreDatatype dt : RDFDatatype.values()) {
            DATATYPE_MAP.put(dt.getIRI(), dt);
        }
    }

    private CoreDatatypes() {
        // Utility class.
    }

    /**
     * Resolves the {@link CoreDatatype} corresponding to the given datatype IRI.
     *
     * @param datatype the datatype IRI to resolve
     * @return the matching CoreDatatype, or {@link CoreDatatype#NONE} if null or unmapped
     */
    public static CoreDatatype from(IRI datatype) {
        if (datatype == null) {
            return CoreDatatype.NONE;
        }
        CoreDatatype resolved = DATATYPE_MAP.get(datatype);
        return resolved != null ? resolved : CoreDatatype.NONE;
    }
}
