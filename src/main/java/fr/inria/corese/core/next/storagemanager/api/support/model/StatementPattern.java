package fr.inria.corese.core.next.storagemanager.api.support.model;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Value;

import java.util.Arrays;
import java.util.Objects;

/**
 * Pattern for matching statements in operations.
 * Each component can be null to act as a wildcard.
 */
public class StatementPattern {

    private final Resource subject;
    private final IRI predicate;
    private final Value object;
    private final Resource[] contexts;

    private StatementPattern(Builder builder) {
        this.subject = builder.subject;
        this.predicate = builder.predicate;
        this.object = builder.object;
        this.contexts = builder.contexts != null ? builder.contexts.clone() : new Resource[0];
    }

    /**
     * Creates a new builder.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a pattern that matches all statements.
     *
     * @return Pattern matching everything
     */
    public static StatementPattern matchAll() {
        return new Builder().build();
    }

    /**
     * Creates a pattern from components.
     *
     * @param subject   Subject (null for any)
     * @param predicate Predicate (null for any)
     * @param object    Object (null for any)
     * @param contexts  Contexts (null or empty for any)
     * @return Statement pattern
     */
    public static StatementPattern of(Resource subject, IRI predicate, Value object, Resource... contexts) {
        return new Builder()
                .subject(subject)
                .predicate(predicate)
                .object(object)
                .contexts(contexts)
                .build();
    }

    /**
     * Gets the subject pattern.
     *
     * @return Subject or null for wildcard
     */
    public Resource getSubject() {
        return subject;
    }

    /**
     * Gets the predicate pattern.
     *
     * @return Predicate or null for wildcard
     */
    public IRI getPredicate() {
        return predicate;
    }

    /**
     * Gets the object pattern.
     *
     * @return Object or null for wildcard
     */
    public Value getObject() {
        return object;
    }

    /**
     * Gets the contexts pattern.
     *
     * @return Array of contexts (empty for any)
     */
    public Resource[] getContexts() {
        return contexts.clone();
    }

    /**
     * Checks if subject is a wildcard.
     *
     * @return true if subject is null
     */
    public boolean isSubject() {
        return subject == null;
    }

    /**
     * Checks if predicate is a wildcard.
     *
     * @return true if predicate is null
     */
    public boolean isPredicate() {
        return predicate == null;
    }

    /**
     * Checks if object is a wildcard.
     *
     * @return true if object is null
     */
    public boolean isObject() {
        return object == null;
    }

    /**
     * Checks if contexts is a wildcard.
     *
     * @return true if contexts is empty
     */
    public boolean isContexts() {
        return contexts.length == 0;
    }

    /**
     * Checks if this pattern matches all statements.
     *
     * @return true if all components are wildcards
     */
    public boolean matchesAll() {
        return isSubject() && isPredicate() && isObject() && isContexts();
    }

    @Override
    public String toString() {
        return "StatementPattern{" +
                "subject=" + subject +
                ", predicate=" + predicate +
                ", object=" + object +
                ", contexts=" + Arrays.toString(contexts) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatementPattern that = (StatementPattern) o;
        return Objects.equals(subject, that.subject) &&
                Objects.equals(predicate, that.predicate) &&
                Objects.equals(object, that.object) &&
                Arrays.equals(contexts, that.contexts);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(subject, predicate, object);
        result = 31 * result + Arrays.hashCode(contexts);
        return result;
    }

    /**
     * Builder for StatementPattern.
     */
    public static class Builder {
        private Resource subject;
        private IRI predicate;
        private Value object;
        private Resource[] contexts;

        private Builder() {
        }

        /**
         * Sets the subject pattern.
         *
         * @param subject Subject (null for wildcard)
         * @return this builder
         */
        public Builder subject(Resource subject) {
            this.subject = subject;
            return this;
        }

        /**
         * Sets the predicate pattern.
         *
         * @param predicate Predicate (null for wildcard)
         * @return this builder
         */
        public Builder predicate(IRI predicate) {
            this.predicate = predicate;
            return this;
        }

        /**
         * Sets the object pattern.
         *
         * @param object Object (null for wildcard)
         * @return this builder
         */
        public Builder object(Value object) {
            this.object = object;
            return this;
        }

        /**
         * Sets the contexts pattern.
         *
         * @param contexts Contexts (null or empty for wildcard)
         * @return this builder
         */
        public Builder contexts(Resource... contexts) {
            this.contexts = contexts;
            return this;
        }

        /**
         * Builds the pattern.
         *
         * @return StatementPattern instance
         */
        public StatementPattern build() {
            return new StatementPattern(this);
        }
    }
}