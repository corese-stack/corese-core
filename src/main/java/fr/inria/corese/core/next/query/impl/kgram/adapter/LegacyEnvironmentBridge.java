package fr.inria.corese.core.next.query.impl.kgram.adapter;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Expr;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.api.query.Environment;
import fr.inria.corese.core.kgram.api.query.ProcessVisitor;
import fr.inria.corese.core.kgram.core.Eval;
import fr.inria.corese.core.kgram.core.Exp;
import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.Query;
import fr.inria.corese.core.kgram.event.EventManager;
import fr.inria.corese.core.kgram.path.Path;
import fr.inria.corese.core.kgram.tool.ApproximateSearchEnv;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.parser.ASTExtension;

import java.util.Map;

/**
 * Wraps a next-package {@link fr.inria.corese.core.next.query.impl.kgram.api.query.Environment}
 * as the old kgram {@link Environment} required by {@code Expression.evalWE}.
 *
 * <p>Only {@link #getNode(Expr)} is functional (variable lookup by label);
 * all other methods throw {@link UnsupportedOperationException} since they are
 * not invoked during basic SPARQL 1.0 FILTER expression evaluation.</p>
 */
final class LegacyEnvironmentBridge implements Environment {

    private final fr.inria.corese.core.next.query.impl.kgram.api.query.Environment delegate;

    LegacyEnvironmentBridge(fr.inria.corese.core.next.query.impl.kgram.api.query.Environment delegate) {
        this.delegate = delegate;
    }

    /**
     * Looks up the variable by label in the wrapped next-package environment and
     * wraps the result in a {@link LegacyNodeBridge}.
     */
    @Override
    public Node getNode(Expr varExpr) {
        fr.inria.corese.core.next.query.impl.kgram.api.core.Node n =
                delegate.getNode(varExpr.getLabel());
        return n == null ? null : new LegacyNodeBridge(n);
    }

    // --- not called during basic FILTER evaluation ---
    @Override public Query   getQuery()                      { throw new UnsupportedOperationException(); }
    @Override public Binding getBind()                       {
        // Extract the Binding from the BindingAdapter that Memory initializes
        fr.inria.corese.core.next.query.impl.kgram.api.core.BindingContext ctx = delegate.getBind();
        if (ctx instanceof fr.inria.corese.core.next.query.impl.kgram.adapter.BindingAdapter ba) {
            return ba.delegate();
        }
        throw new UnsupportedOperationException("Cannot convert BindingContext to Binding: " + ctx);
    }
    @Override public void    setBind(Binding b)              { throw new UnsupportedOperationException(); }
    @Override public boolean hasBind()                       { return delegate.hasBind(); }
    @Override public Node    getGraphNode()                  { return null; }
    @Override public Node    getNode(String label)           {
        fr.inria.corese.core.next.query.impl.kgram.api.core.Node n = delegate.getNode(label);
        return n == null ? null : new LegacyNodeBridge(n);
    }
    @Override public Node    getNode(Node qNode)             { throw new UnsupportedOperationException(); }
    @Override public Node    getQueryNode(int n)             { throw new UnsupportedOperationException(); }
    @Override public Node    getQueryNode(String label)      { throw new UnsupportedOperationException(); }
    @Override public boolean isBound(Node qNode)             { throw new UnsupportedOperationException(); }
    @Override public int     pathLength(Node qNode)          { throw new UnsupportedOperationException(); }
    @Override public Path    getPath(Node qNode)             { throw new UnsupportedOperationException(); }
    @Override public int     pathWeight(Node qNode)          { throw new UnsupportedOperationException(); }
    @Override public int     count()                         { throw new UnsupportedOperationException(); }
    @Override public EventManager getEventManager()          { return null; }
    @Override public boolean hasEventManager()               { return false; }
    @Override public Object  getObject()                     { throw new UnsupportedOperationException(); }
    @Override public void    setObject(Object o)             { throw new UnsupportedOperationException(); }
    @Override public Exp     getExp()                        { throw new UnsupportedOperationException(); }
    @Override public void    setExp(Exp exp)                 { throw new UnsupportedOperationException(); }
    @Override public Map<String, IDatatype> getMap()         { throw new UnsupportedOperationException(); }
    @Override public Edge[]  getEdges()                      { throw new UnsupportedOperationException(); }
    @Override public Node[]  getNodes()                      { throw new UnsupportedOperationException(); }
    @Override public Node[]  getQueryNodes()                 { throw new UnsupportedOperationException(); }
    @Override public Mappings getMappings()                  { throw new UnsupportedOperationException(); }
    @Override public Mapping  getMapping()                   { throw new UnsupportedOperationException(); }
    @Override public Iterable<Mapping> getAggregate()        { throw new UnsupportedOperationException(); }
    @Override public void    aggregate(Mapping m, int n)     { throw new UnsupportedOperationException(); }
    @Override public Node    get(Expr varExpr)               { return getNode(varExpr); }
    @Override public ASTExtension getExtension()             { throw new UnsupportedOperationException(); }
    @Override public ApproximateSearchEnv getAppxSearchEnv() { throw new UnsupportedOperationException(); }
    @Override public Eval    getEval()                       { throw new UnsupportedOperationException(); }
    @Override public void    setEval(Eval e)                 { throw new UnsupportedOperationException(); }
    @Override public ProcessVisitor getVisitor()             { return null; }
    @Override public IDatatype getReport()                   { throw new UnsupportedOperationException(); }
    @Override public void    setReport(IDatatype dt)         { throw new UnsupportedOperationException(); }
    @Override public int     size()                          { throw new UnsupportedOperationException(); }
}
