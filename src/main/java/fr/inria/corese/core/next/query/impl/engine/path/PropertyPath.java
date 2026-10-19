package fr.inria.corese.core.next.query.impl.engine.path;

import fr.inria.corese.core.next.query.impl.engine.pattern.PlanCopy;
import fr.inria.corese.core.next.query.impl.engine.pattern.PlanCopyable;
import fr.inria.corese.core.next.query.impl.engine.model.Node;

import java.util.List;

/** Immutable logical property paths; independent of the SPARQL parser. */
public sealed interface PropertyPath extends PlanCopyable<PropertyPath> {
    /** Copies indexed nodes while preserving the logical path structure. */
    @Override
    default PropertyPath copyPlan(PlanCopy copies) {
        PropertyPath result = switch (this) {
            case Predicate(var predicate) -> new Predicate(copies.copy(predicate));
            case Sequence(var left, var right) -> new Sequence(copies.copy(left), copies.copy(right));
            case Alternative(var left, var right) -> new Alternative(copies.copy(left), copies.copy(right));
            case Inverse(var operand) -> new Inverse(copies.copy(operand));
            case Repetition(var operand, var includeZero, var recursive) -> new Repetition(copies.copy(operand), includeZero, recursive);
            case Negated(var forward, var backward) -> new Negated(copies.copy(forward), copies.copy(backward));
        };
        copies.remember(this, result);
        return result;
    }

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
