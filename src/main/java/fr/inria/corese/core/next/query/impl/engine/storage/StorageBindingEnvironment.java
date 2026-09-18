package fr.inria.corese.core.next.query.impl.engine.storage;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.impl.engine.eval.ApproximateSearchEnv;
import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.event.KgramEventDispatcher;
import fr.inria.corese.core.next.query.impl.engine.event.ProcessVisitor;
import fr.inria.corese.core.next.query.impl.engine.model.BindingContext;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.path.Path;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;

import java.util.List;
import java.util.Map;

/**
 * Environment overlay used while joining BGP edges.
 *
 * <p>Local bindings produced by earlier triple patterns take precedence over the delegate
 * environment, which lets later patterns see already-bound variables.</p>
 */
final class StorageBindingEnvironment implements Environment {

    private final Environment delegate;
    private final StorageBindingSet bindings;

    StorageBindingEnvironment(Environment delegate, StorageBindingSet bindings) {
        this.delegate = delegate;
        this.bindings = bindings;
    }

    @Override
    public Node getNode(Node queryNode) {
        Node node = bindings.get(queryNode);
        if (node != null) {
            return node;
        }
        return delegate == null ? null : delegate.getNode(queryNode);
    }

    @Override
    public boolean isBound(Node queryNode) {
        return getNode(queryNode) != null;
    }

    @Override
    public Query getQuery() {
        return delegate == null ? null : delegate.getQuery();
    }

    @Override
    public BindingContext getBind() {
        return delegate == null ? null : delegate.getBind();
    }

    @Override
    public void setBind(BindingContext bindingContext) {
        if (delegate != null) {
            delegate.setBind(bindingContext);
        }
    }

    @Override
    public boolean hasBind() {
        return delegate != null && delegate.hasBind();
    }

    @Override
    public Node getGraphNode() {
        return delegate == null ? null : delegate.getGraphNode();
    }

    @Override
    public Node getNode(Expr varExpr) {
        return delegate == null ? null : delegate.getNode(varExpr);
    }

    @Override
    public Node getNode(String label) {
        return delegate == null ? null : delegate.getNode(label);
    }

    @Override
    public Node getQueryNode(int n) {
        return delegate == null ? null : delegate.getQueryNode(n);
    }

    @Override
    public Node getQueryNode(String label) {
        return delegate == null ? null : delegate.getQueryNode(label);
    }

    @Override
    public int pathLength(Node queryNode) {
        return delegate == null ? 0 : delegate.pathLength(queryNode);
    }

    @Override
    public Path getPath(Node queryNode) {
        return delegate == null ? null : delegate.getPath(queryNode);
    }

    @Override
    public int count() {
        return delegate == null ? 0 : delegate.count();
    }

    @Override
    public KgramEventDispatcher getEventManager() {
        return delegate == null ? null : delegate.getEventManager();
    }

    @Override
    public Object getObject() {
        return delegate == null ? null : delegate.getObject();
    }

    @Override
    public void setObject(Object object) {
        if (delegate != null) {
            delegate.setObject(object);
        }
    }

    @Override
    public Exp getExp() {
        return delegate == null ? null : delegate.getExp();
    }

    @Override
    public void setExp(Exp exp) {
        if (delegate != null) {
            delegate.setExp(exp);
        }
    }

    @Override
    public Map<String, DatatypeValue> getMap() {
        return delegate == null ? Map.of() : delegate.getMap();
    }

    @Override
    public Edge[] getEdges() {
        return delegate == null ? new Edge[0] : delegate.getEdges();
    }

    @Override
    public Node[] getNodes() {
        return delegate == null ? new Node[0] : delegate.getNodes();
    }

    @Override
    public Node[] getQueryNodes() {
        return delegate == null ? new Node[0] : delegate.getQueryNodes();
    }

    @Override
    public Mappings getMappings() {
        return delegate == null ? null : delegate.getMappings();
    }

    @Override
    public Mapping getMapping() {
        return delegate == null ? null : delegate.getMapping();
    }

    @Override
    public Iterable<Mapping> getAggregate() {
        return delegate == null ? List.of() : delegate.getAggregate();
    }

    @Override
    public void aggregate(Mapping mapping, int n) {
        if (delegate != null) {
            delegate.aggregate(mapping, n);
        }
    }

    @Override
    public Node get(Expr varExpr) {
        return delegate == null ? null : delegate.get(varExpr);
    }

    @Override
    public ApproximateSearchEnv getAppxSearchEnv() {
        return delegate == null ? null : delegate.getAppxSearchEnv();
    }

    @Override
    public Eval getEval() {
        return delegate == null ? null : delegate.getEval();
    }

    @Override
    public void setEval(Eval eval) {
        if (delegate != null) {
            delegate.setEval(eval);
        }
    }

    @Override
    public ProcessVisitor getVisitor() {
        return delegate == null ? null : delegate.getVisitor();
    }

    @Override
    public DatatypeValue getReport() {
        return delegate == null ? null : delegate.getReport();
    }

    @Override
    public void setReport(DatatypeValue datatype) {
        if (delegate != null) {
            delegate.setReport(datatype);
        }
    }

    @Override
    public int size() {
        return delegate == null ? 0 : delegate.size();
    }
}
