package fr.inria.corese.core.next.impl.common.serialization;

/**
 * Defines the policy for serializing literal datatypes.
 */
public enum LiteralDatatypePolicyEnum {
    /**
     * Only show datatype if it's not xsd:string and not rdf:langString.
     */
    MINIMAL,
    /**
     * Always show the full datatype, even for xsd:string.
     */
    ALWAYS_TYPED,
    /**
     * Only show explicit datatype for XSD types (non-XSD datatypes might be omitted or full URI).
     */
    XSD_TYPED
}
