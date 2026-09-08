package fr.inria.corese.core.next.query.impl.engine.event;

import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;

import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.Pointerable;
import fr.inria.corese.core.next.query.impl.engine.path.Path;
import fr.inria.corese.core.next.data.api.model.DatatypeValue;


/**
 * @author Olivier Corby, Wimmics INRIA I3S, 2018
 */
public interface ProcessVisitor extends Pointerable<Object> {

    int SLICE_DEFAULT = 20;

    default boolean isShareable() {
        return false;
    }

    default DatatypeValue defaultValue() {
        return null;
    }

    default DatatypeValue init(Query q) {
        return defaultValue();
    }

    default DatatypeValue before(Query q) {
        return defaultValue();
    }

    default DatatypeValue after(Mappings map) {
        return defaultValue();
    }

    default DatatypeValue start(Query q) {
        return defaultValue();
    }

    default DatatypeValue finish(Mappings map) {
        return defaultValue();
    }

    default DatatypeValue orderby(Mappings map) {
        return defaultValue();
    }

    default boolean distinct(Eval eval, Query q, Mapping map) {
        return true;
    }

    default boolean limit(Mappings map) {
        return true;
    }

    default int slice() {
        return SLICE_DEFAULT;
    }

    default DatatypeValue produce(Eval eval, Node g, Edge edge) {
        return defaultValue();
    }

    default DatatypeValue candidate(Eval eval, Node g, Edge q, Edge e) {
        return defaultValue();
    }

    default DatatypeValue path(Eval eval, Node g, Edge q, Path p, Node s, Node o) {
        return defaultValue();
    }

    default boolean step(Eval eval, Node g, Edge q, Path p, Node s, Node o) {
        return true;
    }

    default boolean result(Eval eval, Mappings map, Mapping m) {
        return true;
    }

    default DatatypeValue statement(Eval eval, Node g, Exp e) {
        return defaultValue();
    }


    default DatatypeValue bgp(Eval eval, Node g, Exp e, Mappings m) {
        return defaultValue();
    }

    default DatatypeValue join(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        return defaultValue();
    }

    default DatatypeValue optional(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        return defaultValue();
    }

    default DatatypeValue minus(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        return defaultValue();
    }

    default DatatypeValue union(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        return defaultValue();
    }

    default DatatypeValue graph(Eval eval, Node g, Exp e, Mappings m) {
        return defaultValue();
    }

    default DatatypeValue query(Eval eval, Node g, Exp e, Mappings m) {
        return defaultValue();
    }

    default DatatypeValue service(Eval eval, Node s, Exp e, Mappings m) {
        return defaultValue();
    }

    default DatatypeValue values(Eval eval, Node g, Exp e, Mappings m) {
        return defaultValue();
    }

    default boolean filter(Eval eval, Node g, Expr e, boolean b) {
        return b;
    }

    default boolean having(Eval eval, Expr e, boolean b) {
        return b;
    }

    default DatatypeValue bind(Eval eval, Node g, Exp e, DatatypeValue val) {
        return val;
    }

    default DatatypeValue select(Eval eval, Expr e, DatatypeValue val) {
        return val;
    }

    default DatatypeValue aggregate(Eval eval, Expr e, DatatypeValue val) {
        return val;
    }

    default boolean produce() {
        return false;
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
