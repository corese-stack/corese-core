package fr.inria.corese.core.next.api.io.serializer;

import fr.inria.corese.core.next.impl.io.serialization.option.LiteralDatatypePolicyEnum;

public interface DatatypePolicyOptions {


    /**
     * Returns the policy for how literal datatypes are printed.
     *
     * @return The {@link LiteralDatatypePolicyEnum} indicating the literal datatype serialization policy.
     */
    LiteralDatatypePolicyEnum getLiteralDatatypePolicy();
}
