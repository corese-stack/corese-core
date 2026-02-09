package fr.inria.corese.core.next.query.api.result;

import fr.inria.corese.core.next.data.api.Value;

/**
 * Represents a single variable-to-value association in a SPARQL solution.
 * <p>
 * A {@code Binding} corresponds to one entry in a {@link BindingSet},
 * mapping a SPARQL variable name (without the leading <code>?</code>)
 * to a concrete RDF {@link Value}. Bindings are immutable snapshots of the
 * state of a variable within a single result row of a {@code SELECT} query.
 * </p>
 *
 *
 * @see BindingSet
 * @see fr.inria.corese.core.next.data.api.Value
 */
public interface Binding {

    /**
     * Returns the name of the SPARQL variable represented by this binding.
     * <p>
     * The returned name does not include the leading <code>?</code>.
     * </p>
     *
     * @return the variable name, never {@code null}
     */
    String getName();

    /**
     * Returns the RDF value associated with this variable.
     *
     * @return the bound {@link Value}, never {@code null}
     */
    Value getValue();
}