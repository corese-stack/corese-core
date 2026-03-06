package fr.inria.corese.core.next.storagemanager.impl.memory;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.data.api.Value;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory storage backend using {@link ConcurrentHashMap} for thread-safe operations.
 */
public class MemoryAdapter {

    private final Set<Statement> statements;

    /**
     * Constructs a new MemoryAdapter with an empty statement set.
     */
    public MemoryAdapter() {
        this.statements = ConcurrentHashMap.newKeySet();
    }

    /**
     * Adds a statement to the in-memory store.
     *
     * @param stmt the statement to add (must not be null)
     * @return true if the statement was added, false if it already existed
     */
    public boolean add(Statement stmt) {
        return statements.add(stmt);
    }

    /**
     * Removes a statement from the in-memory store.
     *
     * @param stmt the statement to remove (must not be null)
     * @return true if the statement was removed, false if it did not exist
     */
    public boolean remove(Statement stmt) {
        return statements.remove(stmt);
    }

    /**
     * Checks if a statement exists in the in-memory store.
     *
     * @param stmt the statement to check (must not be null)
     * @return true if the statement exists
     */
    public boolean contains(Statement stmt) {
        return statements.contains(stmt);
    }

    /**
     * Finds all statements matching the given pattern.
     *
     * @param s        subject filter (null = any)
     * @param p        predicate filter (null = any)
     * @param o        object filter (null = any)
     * @param contexts context filters (null/empty = any)
     * @return set of matching statements (never null, may be empty)
     */
    public Set<Statement> find(Resource s, IRI p, Value o, Resource[] contexts) {
        return statements.stream()
                .filter(stmt -> matches(stmt, s, p, o, contexts))
                .collect(Collectors.toSet());
    }

    /**
     * Returns the total number of statements in the store.
     *
     * @return statement count
     */
    public int size() {
        return statements.size();
    }

    /**
     * Removes all statements from the store.
     */
    public void clear() {
        statements.clear();
    }

    /**
     * Removes all statements from a specific context (named graph).
     *
     * @param context the context to clear (must not be null)
     */
    public void clearContext(Resource context) {
        statements.removeIf(stmt -> context.equals(stmt.getContext()));
    }

    /**
     * Returns all unique subject resources in the store.
     *
     * @return set of all subjects (never null)
     */
    public Set<Resource> getSubjects() {
        return statements.stream()
                .map(Statement::getSubject)
                .collect(Collectors.toSet());
    }

    /**
     * Returns all unique predicate IRIs in the store.
     *
     * @return set of all predicates (never null)
     */
    public Set<IRI> getPredicates() {
        return statements.stream()
                .map(Statement::getPredicate)
                .collect(Collectors.toSet());
    }

    /**
     * Returns all unique object values in the store.
     *
     * @return set of all objects (never null)
     */
    public Set<Value> getObjects() {
        return statements.stream()
                .map(Statement::getObject)
                .collect(Collectors.toSet());
    }

    /**
     * Returns all unique context identifiers in the store.
     *
     * @return set of all contexts (never null, excludes null contexts)
     */
    public Set<Resource> getContexts() {
        return statements.stream()
                .map(Statement::getContext)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Checks if a statement matches the given pattern.
     *
     * @param stmt     the statement to test
     * @param s        subject filter (null = wildcard)
     * @param p        predicate filter (null = wildcard)
     * @param o        object filter (null = wildcard)
     * @param contexts context filters (null/empty = wildcard)
     * @return true if the statement matches all non-null filters
     */
    private boolean matches(Statement stmt, Resource s, IRI p, Value o, Resource[] contexts) {
        if (s != null && !s.equals(stmt.getSubject())) {
            return false;
        }

        if (p != null && !p.equals(stmt.getPredicate())) {
            return false;
        }

        if (o != null && !o.equals(stmt.getObject())) {
            return false;
        }

        if (contexts != null && contexts.length > 0) {
            return matchesContext(stmt.getContext(), contexts);
        }

        return true;
    }

    /**
     * Checks if a statement context matches any of the given contexts.
     *
     * @param stmtContext the statement's context (may be null)
     * @param contexts    the context filters (must not be null or empty)
     * @return true if the statement context matches any filter
     */
    private boolean matchesContext(Resource stmtContext, Resource[] contexts) {
        return Arrays.stream(contexts)
                .anyMatch(ctx -> {
                    if (ctx == null && stmtContext == null) {
                        return true;
                    }
                    if (ctx == null || stmtContext == null) {
                        return false;
                    }

                    String ctxValue = ctx.stringValue();
                    String stmtCtxValue = stmtContext.stringValue();

                    if (ctxValue != null && stmtCtxValue != null) {
                        return ctxValue.equals(stmtCtxValue);
                    }

                    return ctx.equals(stmtContext);
                });
    }
}