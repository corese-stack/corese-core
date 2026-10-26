package fr.inria.corese.core.next.query.impl.engine.event;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;

/**
 * Execution observer and lifecycle listener interface for query processing in {@link Eval}.
 *
 * <p>Supports life-cycle notifications (init, before, after, start, finish),
 * solution filtering (distinct, limit, result), term comparison, and AST operator events.</p>
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2018
 */
public interface ProcessVisitor {

    default boolean isShareable() {
        return false;
    }

    default void init(QueryPlan q) {
        // Notification callback
    }

    default void before(QueryPlan q) {
        // Notification callback
    }

    default void after(Mappings map) {
        // Notification callback
    }

    default void start(QueryPlan q) {
        // Notification callback
    }

    default void finish(Mappings map) {
        // Notification callback
    }

    default void orderby(Mappings map) {
        // Notification callback
    }

    default boolean distinct(Eval eval, QueryPlan q, Mapping map) {
        return true;
    }

    default boolean limit(Mappings map) {
        return true;
    }

    default DatatypeValue candidate(Eval eval, Node g, Edge q, Edge e) {
        return null;
    }

    default boolean result(Eval eval, Mappings map, Mapping m) {
        return true;
    }

    default void statement(Eval eval, Node g, Exp e) {
        // Notification callback
    }

    default void bgp(Eval eval, Node g, Exp e, Mappings m) {
        // Notification callback
    }

    default void join(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        // Notification callback
    }

    default void optional(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        // Notification callback
    }

    default void minus(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        // Notification callback
    }

    default void union(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        // Notification callback
    }

    default void graph(Eval eval, Node g, Exp e, Mappings m) {
        // Notification callback
    }

    default void query(Eval eval, Node g, Exp e, Mappings m) {
        // Notification callback
    }

    default void service(Eval eval, Node s, Exp e, Mappings m) {
        // Notification callback
    }

    default void values(Eval eval, Node g, Exp e, Mappings m) {
        // Notification callback
    }

    default boolean filter(Eval eval, Node g, Expr e, boolean b) {
        return b;
    }

    default void having(Eval eval, Expr e, boolean b) {
        // Notification callback
    }

    default void bind(Eval eval, Node g, Exp e, DatatypeValue val) {
        // Notification callback
    }

    default void select(Eval eval, Expr e, DatatypeValue val) {
        // Notification callback
    }

    default void aggregate(Eval eval, Expr e, DatatypeValue val) {
        // Notification callback
    }

    default boolean statement() {
        return false;
    }

    default boolean candidate() {
        return false;
    }

    default boolean filter() {
        return false;
    }

    default int compare(Eval eval, int res, DatatypeValue dt1, DatatypeValue dt2) {
        return res;
    }

}
