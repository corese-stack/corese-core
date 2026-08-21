package fr.inria.corese.core.next.query.impl.kgram.api.query;

import fr.inria.corese.core.next.query.impl.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Expr;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Pointerable;
import fr.inria.corese.core.next.query.impl.kgram.core.*;
import fr.inria.corese.core.next.query.impl.kgram.path.Path;
import fr.inria.corese.core.sparql.api.IDatatype;

import java.util.List;

/**
 * @author Olivier Corby, Wimmics INRIA I3S, 2018
 */
@SuppressWarnings("unused")
public interface ProcessVisitor extends Pointerable<Object> {

    int SLICE_DEFAULT = 20;

    default boolean isShareable() {
        return false;
    }

    default IDatatype defaultValue() {
        return null;
    }

    default IDatatype init(Query q) {
        return defaultValue();
    }

    default IDatatype before(Query q) {
        return defaultValue();
    }

    default IDatatype after(Mappings map) {
        return defaultValue();
    }

    default IDatatype start(Query q) {
        return defaultValue();
    }

    default IDatatype finish(Mappings map) {
        return defaultValue();
    }

    default IDatatype orderby(Mappings map) {
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

    default IDatatype produce(Eval eval, Node g, Edge edge) {
        return defaultValue();
    }

    default IDatatype candidate(Eval eval, Node g, Edge q, Edge e) {
        return defaultValue();
    }

    default IDatatype path(Eval eval, Node g, Edge q, Path p, Node s, Node o) {
        return defaultValue();
    }

    default boolean step(Eval eval, Node g, Edge q, Path p, Node s, Node o) {
        return true;
    }

    default boolean result(Eval eval, Mappings map, Mapping m) {
        return true;
    }

    default IDatatype statement(Eval eval, Node g, Exp e) {
        return defaultValue();
    }


    default IDatatype bgp(Eval eval, Node g, Exp e, Mappings m) {
        return defaultValue();
    }

    default IDatatype join(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        return defaultValue();
    }

    default IDatatype optional(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        return defaultValue();
    }

    default IDatatype minus(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        return defaultValue();
    }

    default IDatatype union(Eval eval, Node g, Exp e, Mappings m1, Mappings m2) {
        return defaultValue();
    }

    default IDatatype graph(Eval eval, Node g, Exp e, Mappings m) {
        return defaultValue();
    }

    default IDatatype query(Eval eval, Node g, Exp e, Mappings m) {
        return defaultValue();
    }

    default IDatatype service(Eval eval, Node s, Exp e, Mappings m) {
        return defaultValue();
    }

    default IDatatype values(Eval eval, Node g, Exp e, Mappings m) {
        return defaultValue();
    }

    default boolean filter(Eval eval, Node g, Expr e, boolean b) {
        return b;
    }

    default boolean having(Eval eval, Expr e, boolean b) {
        return b;
    }

    default IDatatype bind(Eval eval, Node g, Exp e, IDatatype val) {
        return val;
    }

    default IDatatype select(Eval eval, Expr e, IDatatype val) {
        return val;
    }

    default IDatatype aggregate(Eval eval, Expr e, IDatatype val) {
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

    default int compare(Eval eval, int res, IDatatype dt1, IDatatype dt2) {
        return res;
    }

}
