package fr.inria.corese.core.storage.api.datamanager.support.model;

import fr.inria.corese.core.kgram.api.core.Node;

import java.util.List;
import java.util.Objects;

/**
 * Pattern for matching edges in queries.
 */
public final class EdgePattern {

    private final Node subject;
    private final Node predicate;
    private final Node object;
    private final List<Node> contexts;

    /**
     * Private constructor - use Builder.
     */
    private EdgePattern(Builder builder) {
        this.subject = builder.subject;
        this.predicate = builder.predicate;
        this.object = builder.object;
        this.contexts = builder.contexts != null
                ? List.copyOf(builder.contexts)
                : null;
    }

    /**
     * Returns the subject node (null means any).
     *
     * @return Subject node or null
     */
    public Node getSubject() {
        return subject;
    }

    /**
     * Returns the predicate node (null means any).
     *
     * @return Predicate node or null
     */
    public Node getPredicate() {
        return predicate;
    }

    /**
     * Returns the object node (null means any).
     *
     * @return Object node or null
     */
    public Node getObject() {
        return object;
    }

    /**
     * Returns the contexts to search in (null or empty means all).
     *
     * @return List of contexts or null
     */
    public List<Node> getContexts() {
        return contexts;
    }

    /**
     * Checks if this pattern has a subject constraint.
     *
     * @return true if subject is specified
     */
    public boolean hasSubject() {
        return subject == null;
    }

    /**
     * Checks if this pattern has a predicate createFunCall.
     *
     * @return true if predicate is specified
     */
    public boolean hasPredicate() {
        return predicate == null;
    }

    /**
     * Checks if this pattern has an object createFunCall.
     *
     * @return true if object is specified
     */
    public boolean hasObject() {
        return object == null;
    }

    /**
     * Checks if this pattern has context constraints.
     *
     * @return true if contexts are specified
     */
    public boolean hasContexts() {
        return contexts == null || contexts.isEmpty();
    }

    /**
     * Checks if this pattern matches everything (no constraints).
     *
     * @return true if no constraints
     */
    public boolean matchesAll() {
        return hasSubject() && hasPredicate() && hasObject() && hasContexts();
    }

    /**
     * Creates a new builder.
     *
     * @return New builder instance
     */
    public static Builder builder() {
        return new Builder();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EdgePattern that = (EdgePattern) o;
        return Objects.equals(subject, that.subject) &&
                Objects.equals(predicate, that.predicate) &&
                Objects.equals(object, that.object) &&
                Objects.equals(contexts, that.contexts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, predicate, object, contexts);
    }

    @Override
    public String toString() {
        return "EdgePattern{" +
                "subject=" + subject +
                ", predicate=" + predicate +
                ", object=" + object +
                ", contexts=" + contexts +
                '}';
    }

    /**
     * Builder for EdgePattern.
     */
    public static final class Builder {
        private Node subject;
        private Node predicate;
        private Node object;
        private List<Node> contexts;

        private Builder() {
        }

        /**
         * Sets the subject createFunCall.
         *
         * @param subject Subject node (null for any)
         * @return This builder (for chaining)
         */
        public Builder subject(Node subject) {
            this.subject = subject;
            return this;
        }

        /**
         * Sets the predicate createFunCall.
         *
         * @param predicate Predicate node (null for any)
         * @return This builder (for chaining)
         */
        public Builder predicate(Node predicate) {
            this.predicate = predicate;
            return this;
        }

        /**
         * Sets the object createFunCall.
         *
         * @param object Object node (null for any)
         * @return This builder (for chaining)
         */
        public Builder object(Node object) {
            this.object = object;
            return this;
        }

        /**
         * Sets the contexts createFunCall.
         *
         * @param contexts List of contexts (null or empty for all)
         * @return This builder (for chaining)
         */
        public Builder contexts(List<Node> contexts) {
            this.contexts = contexts;
            return this;
        }

        /**
         * Sets a single context createFunCall.
         *
         * @param context Single context
         * @return This builder (for chaining)
         */
        public Builder context(Node context) {
            this.contexts = context != null ? List.of(context) : null;
            return this;
        }

        /**
         * Builds the EdgePattern.
         *
         * @return New EdgePattern instance
         */
        public EdgePattern build() {
            return new EdgePattern(this);
        }
    }
}