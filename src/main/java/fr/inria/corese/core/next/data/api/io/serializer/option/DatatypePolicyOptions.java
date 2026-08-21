package fr.inria.corese.core.next.data.api.io.serializer.option;

/**
 * Interface for serializer options to determine the policy for the literal datatypes
 */
public interface DatatypePolicyOptions {


    /**
     * Returns the policy for how literal datatypes are printed.
     *
     * @return The {@link LiteralDatatypePolicy} indicating the literal datatype serialization policy.
     */
    LiteralDatatypePolicy getLiteralDatatypePolicy();
}
