package fr.inria.corese.core.next.query.impl.engine.path;

import fr.inria.corese.core.next.query.impl.engine.model.Node;

import java.util.List;

/** Immutable logical property paths; independent of the SPARQL parser. */
public sealed interface PropertyPath {
    record Predicate(Node predicate) implements PropertyPath { }
    record Sequence(PropertyPath left, PropertyPath right) implements PropertyPath { }
    record Alternative(PropertyPath left, PropertyPath right) implements PropertyPath { }
    record Inverse(PropertyPath operand) implements PropertyPath { }
    record Repetition(PropertyPath operand, boolean includeZero, boolean recursive) implements PropertyPath { }
    record Negated(List<Node> forward, List<Node> backward) implements PropertyPath {
        public Negated {
            forward = List.copyOf(forward);
            backward = List.copyOf(backward);
        }
    }
}
