package fr.inria.corese.core.next.storage.api.model;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Pattern for matching statements in queries.
 * Subject, predicate, and object may be {@code null} wildcards. No contexts
 * means every graph; an explicit {@code null} context selects the default graph.
 */
public final class StatementPattern {

    private final Resource subject;
    private final IRI predicate;
    private final Value object;
    private final Set<Resource> contexts;

    private StatementPattern(Builder builder) {
        this.subject = builder.subject;
        this.predicate = builder.predicate;
        this.object = builder.object;
        if (builder.contexts == null || builder.contexts.length == 0) {
            this.contexts = Collections.emptySet();
        } else {
            this.contexts = Collections.unmodifiableSet(
                    new LinkedHashSet<>(Arrays.asList(builder.contexts)));
        }
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
     * @param contexts  contexts to match; null or empty means every graph and an
     *                  explicit null element means the default graph
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
     * @return contexts in insertion order (empty for every graph)
     */
    public Resource[] getContexts() {
        return contexts.toArray(Resource[]::new);
    }

    /**
     * Checks if subject is a wildcard.
     *
     * @return true if subject is null
     */
    public boolean isSubjectWildcard() {
        return subject == null;
    }

    /**
     * Checks if predicate is a wildcard.
     *
     * @return true if predicate is null
     */
    public boolean isPredicateWildcard() {
        return predicate == null;
    }

    /**
     * Checks if object is a wildcard.
     *
     * @return true if object is null
     */
    public boolean isObjectWildcard() {
        return object == null;
    }

    /**
     * Checks if contexts is a wildcard.
     *
     * @return true if contexts is empty
     */
    public boolean isContextWildcard() {
        return contexts.isEmpty();
    }

    /**
     * Checks if this pattern matches all statements.
     *
     * @return true if all components are wildcards
     */
    public boolean matchesAll() {
        return isSubjectWildcard()
                && isPredicateWildcard()
                && isObjectWildcard()
                && isContextWildcard();
    }

    @Override
    public String toString() {
        return "StatementPattern{" +
                "subject=" + subject +
                ", predicate=" + predicate +
                ", object=" + object +
                ", contexts=" + contexts +
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
                contexts.equals(that.contexts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, predicate, object, contexts);
    }

    /**
     * Builder for StatementPattern.
     */
    public static final class Builder {
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
         * @param contexts contexts to match; null or empty means every graph and
         *                 an explicit null element means the default graph
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
