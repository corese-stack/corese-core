package fr.inria.corese.core.next.data.io.serializer;

import fr.inria.corese.core.next.data.impl.io.serialization.option.LiteralDatatypePolicyEnum;

/**
 * Interface for serializer options to determine the policy for the literal datatypes
 */
public interface DatatypePolicyOptions {


    /**
     * Returns the policy for how literal datatypes are printed.
     *
     * @return The {@link LiteralDatatypePolicyEnum} indicating the literal datatype serialization policy.
     */
    LiteralDatatypePolicyEnum getLiteralDatatypePolicy();
}
