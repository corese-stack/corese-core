package fr.inria.corese.core.next.query.impl.result;

import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.result.Binding;

import java.util.Objects;

/** Immutable variable-to-value binding. */
public record CoreseBinding(String name, Value value) implements Binding {

    public CoreseBinding {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
    }
}
