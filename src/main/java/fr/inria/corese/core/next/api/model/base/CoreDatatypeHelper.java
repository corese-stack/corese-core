package fr.inria.corese.core.next.api.model.base;

import fr.inria.corese.core.next.api.model.IRI;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class CoreDatatypeHelper {

    private static Map<IRI, CoreDatatype> DATATYPE_MAP;

    static Map<IRI, CoreDatatype> getDatatypeMap() {
        HashMap<IRI, CoreDatatype> map = new HashMap<>();
        if (DATATYPE_MAP == null) {
            for (CoreDatatype.XSD xsd : CoreDatatype.XSD.values()) {
                map.put(xsd.getIRI(), xsd);
            }
            for (CoreDatatype.RDF rdf : CoreDatatype.RDF.values()) {
                map.put(rdf.getIRI(), rdf);
            }

            DATATYPE_MAP = Collections.unmodifiableMap(map);
        }
        return DATATYPE_MAP;
    }

    /**
     * Helper method to get the CoreDatatype based on the IRI.
     * @param datatype The IRI of the datatype.
     * @return The corresponding CoreDatatype, or NONE if not found.
     */
    public static CoreDatatype getDatatypeFromIRI(IRI datatype) {
        return DATATYPE_MAP.getOrDefault(datatype, CoreDatatype.NONE);
    }
}
