package fr.inria.corese.core.next.query.impl.kgram.adapter;

import fr.inria.corese.core.next.query.impl.kgram.api.core.BindingContext;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Expr;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.next.query.impl.kgram.core.Exp;
import fr.inria.corese.core.next.query.impl.kgram.core.Mappings;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.function.term.Binding;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter to use a {@link Binding} instance via the {@link BindingContext} interface.
 *
 * @param delegate The delegated Binding instance.
 */
public record BindingAdapter(Binding delegate) implements BindingContext {

    /**
     * Constructs an adapter for the given Binding delegate.
     *
     * @param delegate the Binding instance to wrap.
     * @throws IllegalArgumentException if delegate is null.
     */
    public BindingAdapter {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate cannot be null");
        }
    }


    /**
     * Returns the underlying delegated Binding instance.
     *
     * @return the delegate.
     */
    @Override
    public Binding delegate() {
        return delegate;
    }

    @Override
    public Node getValue(String variable) {
        // Binding.get() requires an Expr, not a String.
        // Look for the corresponding Expr variable label.
        for (fr.inria.corese.core.kgram.api.core.Expr var : delegate.getVariables()) {
            if (var.getLabel().equals(variable)) {
                Object value = delegate.get(var);
                return (value instanceof Node) ? (Node) value : null;
            }
        }
        return null;
    }

    @Override
    public void setValue(String variable, Node value) {
        // Binding.set() requires an Expr and IDatatype, not a String and Node.
        // Search for the corresponding Expr variable.
        for (fr.inria.corese.core.kgram.api.core.Expr var : delegate.getVariables()) {
            if (var.getLabel().equals(variable)) {
                // Convert Node to IDatatype
                IDatatype dataValue = (value != null) ? value.getDatatypeValue() : null;
                delegate.set(var, dataValue);
                return;
            }
        }
    }

    @Override
    public boolean isDefined(String variable) {
        for (fr.inria.corese.core.kgram.api.core.Expr var : delegate.getVariables()) {
            if (var.getLabel().equals(variable)) {
                // isBound() accepts a String label
                return delegate.isBound(var.getLabel());
            }
        }
        return false;
    }

    @Override
    public Map<String, Node> getBindings() {
        Map<String, Node> map = new HashMap<>();
        for (fr.inria.corese.core.kgram.api.core.Expr var : delegate.getVariables()) {
            Object value = delegate.get(var);
            if (value instanceof Node) {
                map.put(var.getLabel(), (Node) value);
            }
        }
        return map;
    }

    @Override
    public void copy(BindingContext other) {
        if (other == null) {
            return;
        }

        if (other instanceof BindingAdapter) {
            Binding otherBinding = ((BindingAdapter) other).delegate;
            delegate.share(otherBinding);
        } else {
            // Copy variable by variable
            for (Map.Entry<String, Node> entry : other.getBindings().entrySet()) {
                setValue(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Shares data between two contexts.
     * Necessary for implementations like Memory.share()
     */
    @Override
    public void share(BindingContext source) {
        if (source instanceof BindingAdapter) {
            delegate.share(((BindingAdapter) source).delegate);
        }
    }

    /**
     * Retrieves the value for an Expr variable.
     * Used by Mapping.get(Expr) and Memory.get(Expr).
     */
    @Override
    public Object get(Expr varExpr) {
        // Cast to the appropriate KGRAM Expr type
        if (varExpr instanceof fr.inria.corese.core.kgram.api.core.Expr) {
            return delegate.get((fr.inria.corese.core.kgram.api.core.Expr) varExpr);
        }
        // Fallback to label lookup
        return getValue(varExpr.getLabel());
    }

    /**
     * Retrieves the associated ProcessVisitor.
     */
    @Override
    public Object getVisitor() {
        return delegate.getVisitor();
    }

    /**
     * Sets the associated ProcessVisitor.
     */
    @Override
    public void setVisitor(Object visitor) {
        if (visitor instanceof fr.inria.corese.core.kgram.api.query.ProcessVisitor) {
            delegate.setVisitor((fr.inria.corese.core.kgram.api.query.ProcessVisitor) visitor);
        }
    }

    /**
     * Visit method for reporting.
     */
    @Override
    public void visit(Object e, Object g, Object m1, Object m2) {
        try {
            // Check for "next" kgram types
            if (e instanceof Exp &&
                    (g == null || g instanceof Node) &&
                    (m1 == null || m1 instanceof Mappings) &&
                    (m2 == null || m2 instanceof Mappings)) {

                delegate.visit(
                        (Exp) e,
                        (Node) g,
                        (Mappings) m1,
                        (Mappings) m2
                );
                return;
            }

            // Fallback to "legacy" kgram types
            if (e instanceof fr.inria.corese.core.kgram.core.Exp &&
                    (g == null || g instanceof fr.inria.corese.core.kgram.api.core.Node) &&
                    (m1 == null || m1 instanceof fr.inria.corese.core.kgram.core.Mappings) &&
                    (m2 == null || m2 instanceof fr.inria.corese.core.kgram.core.Mappings)) {

                delegate.visit(
                        (fr.inria.corese.core.kgram.core.Exp) e,
                        (fr.inria.corese.core.kgram.api.core.Node) g,
                        (fr.inria.corese.core.kgram.core.Mappings) m1,
                        (fr.inria.corese.core.kgram.core.Mappings) m2
                );
            }
        } catch (Exception ex) {
            // Silently ignore errors as the visit() method is optional/informative
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof BindingAdapter) {
            return delegate.equals(((BindingAdapter) obj).delegate);
        }

        return false;
    }

}